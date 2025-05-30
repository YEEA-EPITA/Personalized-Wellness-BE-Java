package fr.epita.yeea2.config;

import fr.epita.yeea2.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;

public class GoogleAccessTokenAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final JwtService jwtService;

    public GoogleAccessTokenAuthenticationFilter(AuthenticationManager authenticationManager,
                                                 JwtService jwtService) {
        super(request -> new AntPathMatcher().match("/api/**", request.getRequestURI()));
        setAuthenticationManager(authenticationManager);
        this.jwtService = jwtService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadCredentialsException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        if (isGoogleToken(token)) {
            // Treat it as a Google access token
            Authentication authRequest = new GoogleAccessTokenAuthenticationToken(token);
            return getAuthenticationManager().authenticate(authRequest);

        } else {
            // Treat it as a system-generated JWT
            String username = jwtService.extractUsername(token);

            // Typically no password in JWT-based login, so pass null or a dummy password
            Authentication authRequest = new UsernamePasswordAuthenticationToken(username, null);

            return getAuthenticationManager().authenticate(authRequest);
        }
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        SecurityContextHolder.getContext().setAuthentication(authResult);
        chain.doFilter(request, response);
    }

    private boolean isGoogleToken(String token) {
        // Simple check: Google access tokens are usually opaque strings (not JWT format)
        return !token.contains(".");
    }
}
