package fr.epita.yeea2.config;

import fr.epita.yeea2.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class GoogleAccessTokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public GoogleAccessTokenAuthenticationFilter(AuthenticationManager authenticationManager,
                                                 JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/auth/google")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (token == null || jwtService.isTokenExpired(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("JWT token expired or invalid");
            return;
        }
        try {
            Authentication authResult;

            if (isGoogleToken(token)) {
                authResult = authenticationManager.authenticate(
                        new GoogleAccessTokenAuthenticationToken(token));
            } else {
                String username = jwtService.extractUsername(token);
                authResult = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(username, null));
            }

            SecurityContextHolder.getContext().setAuthentication(authResult);

        } catch (AuthenticationException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized: " + ex.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isGoogleToken(String token) {
        // Google tokens are opaque (not JWT), no dots
        return !token.contains(".");
    }
}
