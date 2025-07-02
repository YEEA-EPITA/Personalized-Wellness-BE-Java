package fr.epita.yeea2.config;

import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.repository.UserRepository;
import fr.epita.yeea2.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Value("${google.googleRedirectUrl}")
    private String googleRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getAttribute("email");

        AppUser user = userRepository.findByEmail(email).orElseGet(() -> {
            AppUser newUser = new AppUser();
            newUser.setEmail(email);
            newUser.setProvider("GOOGLE");
            return userRepository.save(newUser);
        });

        // Generate JWT
        String token = jwtService.generateToken(authentication);

        // Optional: add user info (if needed)
        // String name = oidcUser.getAttribute("name");

        // Redirect to frontend with token
        String redirectUrl = googleRedirectUrl + token;
        response.sendRedirect(redirectUrl);
    }

}
