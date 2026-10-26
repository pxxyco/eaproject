package it.unical.ea_project.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unical.ea_project.security.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Local bounded token buckets. Uses the socket peer, never untrusted forwarding
 * headers.
 */
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger AUDIT = LogManager.getLogger("AUDIT");
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder().maximumSize(50_000)
            .expireAfterAccess(Duration.ofMinutes(10)).build();

    private static final class Bucket {
        double tokens;
        long updated = System.nanoTime();
        boolean reported;

        Bucket(int capacity) {
            tokens = capacity;
        }

        synchronized int take(int capacity, int seconds) {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + Math.max(0, now - updated) / 1_000_000_000d * capacity / seconds);
            updated = now;
            if (tokens >= 1) {
                tokens--;
                reported = false;
                return 0;
            }
            return Math.max(1, (int) Math.ceil((1 - tokens) * seconds / capacity));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest r) {
        return r.getRequestURI().equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest r, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = r.getRequestURI();
        String method = r.getMethod();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthUser user = authentication != null && authentication.getPrincipal() instanceof AuthUser a ? a : null;
        String bucket = "general";
        int capacity = 180;
        int seconds = 60;
        if (path.equals("/api/users/login") || path.equals("/api/users") || path.equals("/api/users/exists")) {
            bucket = "auth";
            capacity = 10;
        } else if (path.equals("/api/users/refresh")) {
            bucket = "refresh";
            capacity = 30;
        } else if (path.equals("/api/account/tickets") && method.equals("POST")) {
            bucket = "support";
            capacity = 5;
        } else if (path.equals("/api/account/cards") && method.equals("POST")) {
            bucket = "cards";
            capacity = 10;
        } else if (path.startsWith("/photo/")) {
            bucket = "photos";
            capacity = 600;
        } else if (path.startsWith("/api/notifications")) {
            bucket = "notifications";
            capacity = 60;
        } else if (path.equals("/api/chat/events")) {
            bucket = "chat-connect";
            capacity = 10;
        } else if (path.startsWith("/api/chat/") && path.endsWith("/messages") && method.equals("POST")) {
            bucket = "chat-send";
            capacity = 40;
        } else if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("OPTIONS")) {
            bucket = "write";
            capacity = path.contains("photo") || path.contains("images") ? 20 : 60;
        }
        String ip = r.getRemoteAddr();
        String identity = user == null ? "ip:" + ip : "user:" + user.id();
        final int burst = capacity;
        Bucket counter = buckets.get(bucket + ":" + identity, key -> new Bucket(burst));
        int retry = counter.take(capacity, seconds);
        if (retry == 0) {
            chain.doFilter(r, response);
            return;
        }
        synchronized (counter) {
            if (!counter.reported) {
                counter.reported = true;
                AUDIT.warn("RATE_LIMIT bucket={} userId={} ip={}", bucket, user == null ? "anonymous" : user.id(), ip);
            }
        }
        reject(response, retry);
    }

    private static void reject(HttpServletResponse response, int wait) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", Integer.toString(wait));
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":\"ERR-104\",\"message\":\"Troppe richieste. Attendi " + wait + " secondi e riprova.\"}");
    }

    /**
     * Rejects abusive peers before JWT signature verification and database lookup.
     */
    public static final class PeerGuard extends OncePerRequestFilter {
        private final Cache<String, Bucket> peers = Caffeine.newBuilder().maximumSize(25_000)
                .expireAfterAccess(Duration.ofMinutes(10)).build();

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return request.getRequestURI().equals("/api/health");
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain chain) throws ServletException, IOException {
            Bucket counter = peers.get(request.getRemoteAddr(), key -> new Bucket(1200));
            int wait = counter.take(1200, 60);
            if (wait == 0) {
                chain.doFilter(request, response);
                return;
            }
            synchronized (counter) {
                if (!counter.reported) {
                    counter.reported = true;
                    AUDIT.warn("RATE_LIMIT bucket=peer ip={}", request.getRemoteAddr());
                }
            }
            reject(response, wait);
        }
    }
}
