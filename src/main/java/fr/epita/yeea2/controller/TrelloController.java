package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.ApiResponse;
import fr.epita.yeea2.dto.TrelloCardGetRequest;
import fr.epita.yeea2.dto.TrelloCardResponse;
import fr.epita.yeea2.entity.PlatformCredential;
import fr.epita.yeea2.service.TrelloService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/trello")
@RequiredArgsConstructor
public class TrelloController {

    private final TrelloService trelloService;

    @Value("${platform.redirectUrl}")
    private String redirectUrl;

    @GetMapping("/login")
    public ResponseEntity<?>  startTrelloOAuth(
            @RequestHeader("Authorization") String authHeader
            ) throws IOException, ExecutionException, InterruptedException {
        String authUrl = trelloService.getAuthorizationUrl(authHeader);
        ApiResponse<String> response_ = new ApiResponse<>(200, "Jira redirected", authUrl);

        return ResponseEntity.ok(response_);    }

    @GetMapping("/callback")
    public void handleCallback(@RequestParam("oauth_token") String oauthToken,
                               @RequestParam("oauth_verifier") String oauthVerifier,
                               @RequestParam("state") String encodedState,
                               HttpServletResponse response) throws IOException {
        try {
            // 1. Decode the state into system JWT
//            String systemToken = new String(Base64.getUrlDecoder().decode(encodedState), StandardCharsets.UTF_8);
//            String email = jwtService.extractUsername(systemToken);

            // 2. Handle Trello OAuth and save credential
            PlatformCredential credential = trelloService.handleOAuthCallback(oauthToken, oauthVerifier, encodedState);

            // 3. Optionally issue a new JWT (or reuse systemToken)
//            String jwt = jwtService.generateToken(email);
//            String redirectUrl = this.redirectUrl + systemToken;
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            // Optional: redirect to a failure page
            String errorRedirect = UriComponentsBuilder
                    .fromUriString(redirectUrl)
                    .queryParam("error", "OAuthFailed")
                    .build()
                    .toUriString();

            response.sendRedirect(errorRedirect);
        }
    }

    @GetMapping("/boards")
    public ResponseEntity<?> getBoards(@RequestParam String trelloEmail) {
        try {
            List<Map<String, Object>> boards = trelloService.getBoards(trelloEmail);
            ApiResponse<List<Map<String, Object>>> response = new ApiResponse<>(200, "Boards fetched successfully", boards);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to fetch Trello boards",
                    "details", e.getMessage()
            ));
        }
    }

    @PostMapping("/cards")
    public ResponseEntity<?> getCardsFromBoards(@RequestBody TrelloCardGetRequest request) {
        if (request.getTrelloEmail() == null || request.getBoardIds() == null || request.getBoardIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "trelloEmail and boardIds are required."));
        }

        try {
            List<TrelloCardResponse> cards = trelloService.getCardDetailsFromBoards(request);
            ApiResponse<List<TrelloCardResponse>> response = new ApiResponse<>(HttpStatus.OK.value(), "Retrieved cards successfully", cards);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<List<TrelloCardResponse>> errorResponse = new ApiResponse<>(400, "Failed to retrieve cards: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }



}
