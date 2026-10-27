package it.unical.ea_project.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                try {
                    Claims claims = jwtService.parseToken(token, JwtService.TYPE_ACCESS);
                    Object rawId = claims.get("id");
                    String subject = claims.getSubject();
                    String role = claims.get("role", String.class);
                    if (!(rawId instanceof Number) || subject == null || subject.isBlank() || role == null
                            || role.isBlank()) {
                        throw new JwtException("Claim JWT mancanti o non validi");
                    }
                    Long userId = ((Number) rawId).longValue();
                    AuthUser user = new AuthUser(userId, subject, role);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (JwtException | IllegalArgumentException e) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        chain.doFilter(request, response);
    }
}
