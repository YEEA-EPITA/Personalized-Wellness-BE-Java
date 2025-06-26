package fr.epita.yeea2.controller;

import fr.epita.yeea2.service.TrelloService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trello")
@RequiredArgsConstructor
public class TrelloController {

    private final TrelloService trelloService;

    @GetMapping("/login")
    public RedirectView startTrelloOAuth() {
        String authorizationUrl = trelloService.getAuthorizationUrl();
        return new RedirectView(authorizationUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(@RequestParam("oauth_token") String oauthToken,
                                            @RequestParam("oauth_verifier") String oauthVerifier) {
        try {
            Map<String, String> tokens = trelloService.handleOAuthCallback(oauthToken, oauthVerifier);
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "OAuth callback failed", "details", e.getMessage()));
        }
    }

    @GetMapping("/boards")
    public ResponseEntity<?> getBoards(@RequestParam("token") String token, @RequestParam("secret") String secret) {
        try {
            List<Map<String, Object>> boards = trelloService.getBoards(token, secret);
            return ResponseEntity.ok(boards);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch boards", "details", e.getMessage()));
        }
    }

    // Add more endpoints here if needed (e.g., getCards, createCard, etc.)
}
