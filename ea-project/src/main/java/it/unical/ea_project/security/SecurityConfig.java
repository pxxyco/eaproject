package it.unical.ea_project.security;

import it.unical.ea_project.service.JwtService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/photo/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/login", "/api/users", "/api/users/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/exists").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers("/api/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/activities/organizer/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.GET, "/api/activities/**", "/api/trips/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/activities/**", "/api/trips/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.PUT, "/api/activities/**", "/api/trips/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.DELETE, "/api/activities/**", "/api/trips/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/organizer/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.GET, "/api/payments/organizer/**").hasRole("ORGANIZER")
                        .requestMatchers("/api/bookings/**").hasRole("TRAVELER")
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}