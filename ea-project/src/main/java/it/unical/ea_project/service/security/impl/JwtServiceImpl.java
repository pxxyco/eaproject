package it.unical.ea_project.service.security.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.service.security.JwtService;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secret;
    @Value("${app.security.jwt-issuer:itinera}")
    private String issuer;
    private final UserRepository users;
    private SecretKey signingKey;
    private static final long ACCESS_TOKEN_VALIDITY = 15 * 60 * 1000L; // 15 minuti
    private static final long REFRESH_TOKEN_VALIDITY = 7L * 24 * 60 * 60 * 1000; // 7 giorni

    @PostConstruct
    private void validateJwtKey() {
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMillis) {
        return Jwts.builder()
                .claims(extraClaims)
                .issuer(issuer)
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    @Override
    public String generateAccessToken(User user) {
        if (user.getRole() == null) {
            throw new IllegalStateException("Impossibile generare il token: ruolo utente mancante.");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", TYPE_ACCESS);
        claims.put("role", user.getRole().name());
        claims.put("id", user.getId());
        claims.put("ver", user.getAuthVersion());
        return buildToken(claims, user.getEmail(), ACCESS_TOKEN_VALIDITY);
    }

    @Override
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", TYPE_REFRESH);
        claims.put("id", user.getId());
        claims.put("ver", user.getAuthVersion());
        return buildToken(claims, user.getEmail(), REFRESH_TOKEN_VALIDITY);
    }

    @Override
    public Claims parseToken(String token, String expectedType) {
        Claims claims = extractAllClaims(token);
        if (!expectedType.equals(claims.get("type", String.class))) {
            throw new JwtException("Tipo di token non valido");
        }
        if (claims.getExpiration() == null || claims.getIssuedAt() == null || claims.getId() == null
                || claims.getSubject() == null
                || claims.getIssuedAt().getTime() > System.currentTimeMillis() + 30_000
                || !claims.getExpiration().after(claims.getIssuedAt()))
            throw new JwtException("Claim di sessione non validi");
        Object rawId = claims.get("id");
        Object version = claims.get("ver");
        if (!(rawId instanceof Number id) || !(version instanceof Number ver))
            throw new JwtException("Sessione non valida");
        User current = users.findById(id.longValue()).filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new JwtException("Account non disponibile"));
        if (current.getAuthVersion() != ver.longValue() || current.getRole() == null
                || TYPE_ACCESS.equals(expectedType)
                        && !current.getRole().name().equals(claims.get("role", String.class)))
            throw new JwtException("Sessione revocata");
        return claims;
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public boolean isTokenValid(String token, User user) {
        try {
            Claims claims = parseToken(token, TYPE_ACCESS);
            return user.getId().equals(((Number) claims.get("id")).longValue());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
