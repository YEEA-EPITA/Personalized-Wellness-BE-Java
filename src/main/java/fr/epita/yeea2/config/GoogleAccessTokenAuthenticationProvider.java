package fr.epita.yeea2.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

public class GoogleAccessTokenAuthenticationProvider implements AuthenticationProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getCredentials();

        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?access_token=" + token;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.get("aud") != null) {
                // Token is valid
                String email = (String) response.get("email"); // may be null if not included in token
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

                User user = new User(email != null ? email : "googleuser", "", authorities);
                return new UsernamePasswordAuthenticationToken(user, token, authorities);
            }
        } catch (HttpClientErrorException e) {
            throw new BadCredentialsException("Invalid Google access token");
        }

        throw new BadCredentialsException("Invalid Google access token");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return GoogleAccessTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

