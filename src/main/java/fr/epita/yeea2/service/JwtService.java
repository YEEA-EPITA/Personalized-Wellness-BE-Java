package fr.epita.yeea2.service;

import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private UserRepository userRepository;

    private Key key;

    private final long expirationMillis = 1000 * 60 * 60; // 1 hour

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

//    public String generateToken(Authentication authentication) {
//        User user = (User) authentication.getPrincipal();
//
//        return Jwts.builder()
//                .setSubject(user.getUsername())
//                .claim("roles", user.getAuthorities())
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
//                .signWith(key, SignatureAlgorithm.HS256)
//                .compact();
//    }

    public String generateToken(Authentication authentication) {
        String email;
        String userId;

        // Google login
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            email = oidcUser.getAttribute("email");

            // System login
        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            email = userDetails.getUsername();

        } else {
            throw new IllegalArgumentException("Unsupported principal type: " + authentication.getPrincipal().getClass());
        }

        // Get user ID from DB
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userId = user.getId();

        // iat and exp
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + 3600 * 1000); // 1 hour

        return Jwts.builder()
                .claim("email", email)
                .claim("id", userId)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).get("email").toString();
    }

    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        return Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractUsername(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).get("id").toString();
    }

}
