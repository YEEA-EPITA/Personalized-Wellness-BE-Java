package fr.epita.yeea2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.TrelloApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrelloService {

    @Value("${trello.api-key}")
    private String apiKey;

    @Value("${trello.api-secret}")
    private String apiSecret;

    @Value("${trello.callback-url}")
    private String callbackUrl;

    private OAuth10aService service;
    private final Map<String, OAuth1RequestToken> requestTokenCache = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        service = new ServiceBuilder(apiKey)
                .apiSecret(apiSecret)
                .callback(callbackUrl)
                .build(TrelloApi.instance());
    }

    public String getAuthorizationUrl() {
        try {
            OAuth1RequestToken requestToken = service.getRequestToken();
            requestTokenCache.put(requestToken.getToken(), requestToken);
            return service.getAuthorizationUrl(requestToken);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Trello authorization URL", e);
        }
    }

    public Map<String, String> handleOAuthCallback(String oauthToken, String oauthVerifier) {
        try {
            OAuth1RequestToken requestToken = requestTokenCache.get(oauthToken);
            OAuth1AccessToken accessToken = service.getAccessToken(requestToken, oauthVerifier);
            return Map.of(
                    "token", accessToken.getToken(),
                    "tokenSecret", accessToken.getTokenSecret()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to handle Trello OAuth callback", e);
        }
    }

    public List<Map<String, Object>> getBoards(String token, String tokenSecret) throws IOException {
        String url = UriComponentsBuilder.fromUriString("https://api.trello.com/1/members/me/boards")
                .queryParam("key", apiKey)
                .queryParam("token", token)
                .build().toUriString();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        JsonNode json = objectMapper.readTree(response.getBody());

        List<Map<String, Object>> boards = new ArrayList<>();
        for (JsonNode node : json) {
            Map<String, Object> board = new HashMap<>();
            board.put("id", node.get("id").asText());
            board.put("name", node.get("name").asText());
            boards.add(board);
        }
        return boards;
    }
}
