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

import static org.hamcrest.Matchers.containsString;
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
        userRepository.deleteAll();

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
    void getUserProfile_shouldReturnCorrectInfo() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.email").value(email))
                .andExpect(jsonPath("$.body.firstName").value("Test"))
                .andExpect(jsonPath("$.body.lastName").value("User"));
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "USER")
    void updateUserProfile_shouldSucceed_withValidPassword() throws Exception {
        ProfileRequest request = ProfileRequest.builder()
                .email("newuser@example.com")
                .firstName("Updated")
                .lastName("User")
                .password(password) // 올바른 비밀번호
                .build();

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.email").value("newuser@example.com"));
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "USER")
    void updateUserProfile_shouldFail_whenEmailAlreadyExists() throws Exception {
        // 중복 이메일 등록
        userRepository.save(AppUser.builder()
                .email("existing@example.com")
                .password(passwordEncoder.encode("another"))
                .firstName("Other")
                .lastName("User")
                .build());

        ProfileRequest request = ProfileRequest.builder()
                .email("existing@example.com")
                .firstName("Updated")
                .lastName("User")
                .password(password)
                .build();

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("The new email is already in use")));
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "USER")
    void updateUserProfile_shouldFail_forGoogleUser() throws Exception {
        AppUser googleUser = userRepository.findByEmail(email).orElseThrow();
        googleUser.setProvider("GOOGLE");
        userRepository.save(googleUser);

        ProfileRequest request = ProfileRequest.builder()
                .email("updated@example.com")
                .firstName("Google")
                .lastName("User")
                .build();

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Google login users are not allowed")));
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "USER")
    void deleteUser_shouldFail_withWrongPassword() throws Exception {
        String requestJson = "{ \"password\": \"wrongpassword\" }";

        mockMvc.perform(put(BASE_URL + "/delete")  // DELETE → PUT
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The password does not match."));
    }

}

