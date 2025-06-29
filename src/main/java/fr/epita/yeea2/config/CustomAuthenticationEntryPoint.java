package fr.epita.yeea2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.epita.yeea2.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // The 'commence' method needs to be implemented to handle failed authentication
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        // Set status to 400 (Bad Request) or 401 (Unauthorized) depending on the use case
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        // Create the error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "invalid token",new ErrorResponse.ErrorDetails("INVALID_TOKEN"));

        // Write the error response to the output stream using ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

}
