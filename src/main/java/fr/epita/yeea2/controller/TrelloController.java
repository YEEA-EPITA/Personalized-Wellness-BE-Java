package fr.epita.yeea2.controller;

import fr.epita.yeea2.dto.ApiResponse;
import fr.epita.yeea2.dto.TrelloCardGetRequest;
import fr.epita.yeea2.dto.TrelloCardResponse;
import fr.epita.yeea2.entity.PlatformCredential;
import fr.epita.yeea2.service.TrelloService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/trello")
@RequiredArgsConstructor
public class TrelloController {

    private final TrelloService trelloService;

    @GetMapping("/login")
    public RedirectView startTrelloOAuth(
            @RequestHeader("Authorization") String authHeader
            ) throws IOException, ExecutionException, InterruptedException {
        String authorizationUrl = trelloService.getAuthorizationUrl(authHeader);
        return new RedirectView(authorizationUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(@RequestParam("oauth_token") String oauthToken,
                                            @RequestParam("oauth_verifier") String oauthVerifier,
                                            @RequestParam("state") String encodedState) {
        try {
            PlatformCredential credential = trelloService.handleOAuthCallback(oauthToken, oauthVerifier, encodedState);
            return ResponseEntity.ok(Map.of(
                    "message", "Trello linked successfully",
                    "platformCredentialId", credential.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "OAuth callback failed", "details", e.getMessage()));
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
