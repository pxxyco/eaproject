package it.unical.ea_project.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestTimingFilter extends OncePerRequestFilter {

    private static final Logger AUDIT = LogManager.getLogger("AUDIT");
    private static final long SLOW_MS = 1000;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        ThreadContext.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            if (ms > SLOW_MS || response.getStatus() >= 400 || !request.getMethod().equals("GET")) {
                AUDIT.info("HTTP status={} ms={} method={} path={}",
                        response.getStatus(), ms, request.getMethod(), request.getRequestURI());
            }
            ThreadContext.remove("requestId");
        }
    }
}
