package it.unical.ea_project.security;

import it.unical.ea_project.service.JwtService;
import jakarta.servlet.DispatcherType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private static final Logger AUDIT = LogManager.getLogger("AUDIT");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) -> {
                            AUDIT.debug("UNAUTHORIZED ip={} {} {}",
                                    request.getRemoteAddr(), request.getMethod(), request.getRequestURI());
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                            Object who = (auth != null && auth.getPrincipal() instanceof AuthUser u) ? u.id() : "?";
                            AUDIT.warn("FORBIDDEN userId={} ip={} {} {}",
                                    who, request.getRemoteAddr(), request.getMethod(), request.getRequestURI());
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                        })
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new RateLimitFilter(), JwtAuthenticationFilter.class)
                .addFilterBefore(new RequestTimingFilter(), RateLimitFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/photo/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/login", "/api/users", "/api/users/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/exists").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/users/me/profile-photo", "/api/users/change-password").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/users/me/profile-photo").authenticated()
                        .requestMatchers("/api/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/activities/organizer/**", "/api/trips/organizer/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.GET, "/api/activities/**", "/api/trips/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/activities/**", "/api/trips/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.POST, "/api/activity-images/**", "/api/trip-images/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.PUT, "/api/activities/**", "/api/trips/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.DELETE, "/api/activities/**", "/api/trips/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/organizer/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.GET, "/api/payments/organizer/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.GET,
                                "/api/activities/**", "/api/trips/**",
                                "/api/activity-images/**", "/api/trip-images/**",
                                "/api/reviews/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reviews").hasRole("TRAVELER")
                        .requestMatchers(HttpMethod.POST,
                                "/api/activities/**", "/api/trips/**",
                                "/api/activity-images/**", "/api/trip-images/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.PUT, "/api/activities/**", "/api/trips/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/activities/**", "/api/trips/**",
                                "/api/activity-images/**", "/api/trip-images/**").hasRole("ORGANIZER")
                        .requestMatchers("/api/bookings/**").hasRole("TRAVELER")
                        .requestMatchers("/api/responses/**").hasRole("ORGANIZER")
                        .anyRequest().authenticated()
                );

        return http.build();

    }

}