package fr.epita.yeea2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.epita.yeea2.dto.ApiResponse;
import fr.epita.yeea2.dto.UserResponse;
import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.repository.UserRepository;
import fr.epita.yeea2.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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

        // Create UserResponse
        UserResponse userResponse = UserResponse.from(user, token);

        // Create standardized API response
        ApiResponse<UserResponse> apiResponse = new ApiResponse<>(200, "Login successful", userResponse);

        // Write as JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), apiResponse);
    }

}
