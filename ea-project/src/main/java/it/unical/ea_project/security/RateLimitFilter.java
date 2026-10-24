package it.unical.ea_project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger AUDIT = LogManager.getLogger("AUDIT");

    private static final long WINDOW_MS = 60_000;
    private static final int GENERAL_LIMIT = 120;
    private static final int PHOTO_LIMIT = 600;
    private static final int NOTIFICATION_LIMIT = 60;
    private static final int AUTH_LIMIT = 10;

    private static final class Counter {
        long windowStart;
        int count;
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = request.getRemoteAddr();

        String bucket;
        int limit;
        if (isAuthEndpoint(method, path)) {
            bucket = "auth";
            limit = AUTH_LIMIT;
        } else if (path.startsWith("/photo/")) {
            bucket = "photo";
            limit = PHOTO_LIMIT;
        } else if (path.startsWith("/api/notifications")) {
            // Le notifiche hanno un bucket separato: un burst sull'inbox non deve
            // consumare il limite generale e bloccare prenotazioni, profilo o ricerca.
            bucket = "notifications";
            limit = NOTIFICATION_LIMIT;
        } else {
            bucket = "general";
            limit = GENERAL_LIMIT;
        }

        int count = hit(ip + ":" + bucket);
        if (count > limit) {
            if (count == limit + 1) {
                AUDIT.warn("RATE_LIMIT ip={} bucket={} limit={} method={} path={}",
                        ip, bucket, limit, method, path);
            }
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":\"ERR-104\",\"message\":\"Troppe richieste in poco tempo. Attendi 60 secondi e riprova.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isAuthEndpoint(String method, String path) {
        return ("POST".equals(method) && (path.equals("/api/users/login") || path.equals("/api/users")))
                || ("GET".equals(method) && path.equals("/api/users/exists"));
    }

    private int hit(String key) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        Counter c = counters.computeIfAbsent(key, k -> new Counter());
        synchronized (c) {
            if (now - c.windowStart >= WINDOW_MS) {
                c.windowStart = now;
                c.count = 0;
            }
            return ++c.count;
        }
    }

    private void cleanupIfNeeded(long now) {
        if (now - lastCleanup < WINDOW_MS) return;
        lastCleanup = now;
        counters.entrySet().removeIf(e -> now - e.getValue().windowStart >= WINDOW_MS);
    }
}

