package it.unical.ea_project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final Logger AUDIT = LogManager.getLogger("AUDIT");
    private static final long SLOW_MS = 1000;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            if (ms > SLOW_MS || response.getStatus() >= 500) {
                AUDIT.warn("SLOW_OR_ERROR status={} ms={} {} {}",
                        response.getStatus(), ms, request.getMethod(), request.getRequestURI());
            }
        }
    }
}
