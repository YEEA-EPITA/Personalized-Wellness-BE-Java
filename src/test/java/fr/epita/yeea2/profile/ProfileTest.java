package fr.epita.yeea2.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.epita.yeea2.dto.ProfileRequest;
import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = fr.epita.yeea2.Yeea2Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProfileTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final String BASE_URL = "/api/profile/me";
    private final String email = "testuser@example.com";
    private final String password = "password123";

    @BeforeEach
    void setUp() {
        AppUser user = AppUser.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName("Test")
                .lastName("User")
                .provider(null)
                .build();

        userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "USER")
    void getUserProfile() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.email").value(email))
                .andExpect(jsonPath("$.body.firstName").value("Test"))
                .andExpect(jsonPath("$.body.lastName").value("User"));
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "USER")
    void updateUserProfile_shouldSucceed_withCorrectPassword() throws Exception {
        ProfileRequest request = ProfileRequest.builder()
                .email("new@example.com")
                .firstName("Updated")
                .lastName("Name")
                .password(password)
                .build();

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your profile has been successfully updated."));
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "USER")
    void updateUserProfile_shouldFail_withWrongPassword() throws Exception {
        ProfileRequest request = ProfileRequest.builder()
                .email("new@example.com")
                .firstName("Updated")
                .lastName("Name")
                .password("wrong password")
                .build();

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The password does not match."));
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "USER")
    void deleteUser_shouldSucceed_withCorrectPassword() throws Exception {
        String requestJson = "{ \"password\": \"" + password + "\" }";

        mockMvc.perform(delete(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your profile has been successfully deleted."));
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "USER")
    void deleteUser_shouldFail_withWrongPassword() throws Exception {
        String requestJson = "{ \"password\": \"wrongpassword\" }";

        mockMvc.perform(delete(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The password does not match."));
    }
}
