package it.unical.ea_project.service.security;

import io.jsonwebtoken.Claims;
import it.unical.ea_project.domain.user.User;

public interface JwtService {
    String TYPE_ACCESS = "access";
    String TYPE_REFRESH = "refresh";

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    String extractUsername(String token);

    boolean isTokenValid(String token, User user);

    Claims parseToken(String token, String expectedType);
}
