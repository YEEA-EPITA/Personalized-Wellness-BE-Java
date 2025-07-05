package fr.epita.yeea2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.TrelloApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuth10aService;
import fr.epita.yeea2.constant.PlatformConstant;
import fr.epita.yeea2.dto.TrelloCardCreateRequest;
import fr.epita.yeea2.dto.TrelloCardResponse;
import fr.epita.yeea2.dto.TrelloCardUpdateRequest;
import fr.epita.yeea2.dto.TrelloListOrCardGetRequest;
import fr.epita.yeea2.entity.PlatformCredential;
import fr.epita.yeea2.repository.PlatformCredentialRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
    private final JwtService jwtService;
    private final PlatformCredentialRepository platformCredentialRepository;


    @PostConstruct
    public void init() {
        service = new ServiceBuilder(apiKey)
                .apiSecret(apiSecret)
                .callback(callbackUrl)
                .build(TrelloApi.instance());
    }
    public String getAuthorizationUrl(String authHeader) throws IOException, ExecutionException, InterruptedException {
        String systemToken = authHeader.replace("Bearer ", "");
        String encodedState = Base64.getUrlEncoder().encodeToString(systemToken.getBytes(StandardCharsets.UTF_8));

        String callbackWithState = UriComponentsBuilder
                .newInstance()
                .uri(URI.create(callbackUrl))
                .queryParam("state", encodedState)
                .build()
                .toString();

        // temporary service just for login
        OAuth10aService tempService = new ServiceBuilder(apiKey)
                .apiSecret(apiSecret)
                .callback(callbackWithState)
                .build(TrelloApi.instance());

        OAuth1RequestToken requestToken = tempService.getRequestToken();
        requestTokenCache.put(requestToken.getToken(), requestToken);

        // Append scope, expiration, name
        String baseUrl = tempService.getAuthorizationUrl(requestToken);
        String fullUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("scope", "read,write,account")
                .queryParam("expiration", "never") // optional: 1hour, 1day, 30days, never
                .queryParam("name", "YEEA2")
                .build()
                .toUriString();

        return fullUrl;
    }

    public PlatformCredential handleOAuthCallback(String oauthToken, String oauthVerifier, String encodedState) {
        try {
            // 1. Decode state → get system token (JWT)
            String systemToken = new String(Base64.getUrlDecoder().decode(encodedState), StandardCharsets.UTF_8);
            String userEmail = jwtService.extractUsername(systemToken); // Extract email from JWT
            String userId = jwtService.extractUserId(systemToken);
            // 2. Get request token from cache
            OAuth1RequestToken requestToken = requestTokenCache.get(oauthToken);
            if (requestToken == null) {
                throw new RuntimeException("OAuth request token not found or expired");
            }

            // 3. Exchange request token + verifier for access token
            OAuth1AccessToken accessToken = service.getAccessToken(requestToken, oauthVerifier);

            // 4. Fetch Trello user info
            Map<String, Object> userInfo = getTrelloUserInfo(accessToken);
            String trelloUsername = (String) userInfo.get("username");
            String fullName = (String) userInfo.get("fullName");
            String trelloEmail = userInfo.get("email") != null ? userInfo.get("email").toString() : userEmail;

            // 5. Save to PlatformCredential table
            return this.saveOrUpdateTrelloCredential(userId, userEmail, accessToken, trelloUsername, fullName, trelloEmail);

        } catch (Exception e) {
            throw new RuntimeException("Failed to handle Trello OAuth callback", e);
        }
    }

    public PlatformCredential saveOrUpdateTrelloCredential(
            String userId,
            String userEmail,
                                                           OAuth1AccessToken accessToken,
                                                           String trelloUsername,
                                                           String fullName,
                                                           String trelloEmail) {
        PlatformCredential.Token token = PlatformCredential.Token.builder()
                .accessToken(accessToken.getToken())
                .accessTokenSecret(accessToken.getTokenSecret())
                .build();

        return platformCredentialRepository.findByPlatformEmailAndType(trelloEmail, PlatformConstant.TRELLO)
                .map(existing -> {
                    existing.setTokens(token);
                    existing.setUpdatedAt(Instant.now());
                    return platformCredentialRepository.save(existing);
                })
                .orElseGet(() -> {
                    PlatformCredential newCredential = PlatformCredential.builder()
                            .type(PlatformConstant.TRELLO)
                            .userEmail(userEmail)
                            .connectorId(new ObjectId(userId))
                            .name(fullName)
                            .platformUserId(trelloUsername)
                            .platformEmail(trelloEmail)
                            .tokens(token)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return platformCredentialRepository.save(newCredential);
                });
    }


    public Map<String, Object> getTrelloUserInfo(OAuth1AccessToken accessToken) throws IOException, InterruptedException, ExecutionException {
        OAuthRequest request = new OAuthRequest(Verb.GET, PlatformConstant.TrelloConstant.MEMBER_ME);
        service.signRequest(accessToken, request);
        Response response = service.execute(request);

        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed to fetch Trello user info: " + response.getMessage());
        }

        return new ObjectMapper().readValue(response.getBody(), Map.class); // Jackson
    }

    public List<Map<String, Object>> getBoards(String trelloEmail) {
        // 1. Get Trello access token from database
        PlatformCredential credential = platformCredentialRepository
                .findByPlatformEmailAndType(trelloEmail, PlatformConstant.TRELLO)
                .orElseThrow(() -> new RuntimeException("Trello credentials not found for: " + trelloEmail));

        String accessToken = credential.getTokens().getAccessToken();
        String tokenSecret = credential.getTokens().getAccessTokenSecret();

        OAuth1AccessToken token = new OAuth1AccessToken(accessToken, tokenSecret);

        // 2. Prepare and execute API call to fetch boards
        OAuthRequest request = new OAuthRequest(Verb.GET, PlatformConstant.TrelloConstant.MEMBER_BOARDS);
        service.signRequest(token, request);

        try {
            Response response = service.execute(request);
            if (!response.isSuccessful()) {
                throw new RuntimeException("Trello API error: " + response.getMessage());
            }

            List<Map<String, Object>> rawBoards = new ObjectMapper().readValue(response.getBody(), List.class);

            // 3. Simplify and return only id/name/url
            return rawBoards.stream()
                    .map(board -> Map.of(
                            "id", board.get("id"),
                            "name", board.get("name"),
                            "url", board.get("url")
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Trello boards", e);
        }
    }

//    public List<Map<String, Object>> getCardsInBoard(String boardId, String trelloEmail) {
//        // 1. Get stored access token + secret
//        PlatformCredential credential = platformCredentialRepository
//                .findByPlatformEmailAndType(trelloEmail, PlatformConstant.TRELLO)
//                .orElseThrow(() -> new RuntimeException("Trello credentials not found"));
//
//        OAuth1AccessToken token = new OAuth1AccessToken(
//                credential.getPlatformToken().getAccessToken(),
//                credential.getPlatformToken().getAccessTokenSecret()
//        );
//
//        List<Map<String, Object>> allCards = new ArrayList<>();
//
//        try {
//            // 2. Get all lists in the board
//            OAuthRequest listRequest = new OAuthRequest(Verb.GET, String.format(PlatformConstant.TrelloConstant.BOARD_LISTS, boardId));
//            service.signRequest(token, listRequest);
//            Response listResponse = service.execute(listRequest);
//
//            if (!listResponse.isSuccessful()) {
//                throw new RuntimeException("Failed to fetch lists: " + listResponse.getMessage());
//            }
//
//            List<Map<String, Object>> lists = new ObjectMapper().readValue(listResponse.getBody(), List.class);
//
//            // 3. For each list, get its cards
//            for (Map<String, Object> list : lists) {
//                String listId = (String) list.get("id");
//                String listName = (String) list.get("name");
//
//                OAuthRequest cardRequest = new OAuthRequest(Verb.GET, String.format(PlatformConstant.TrelloConstant.LIST_CARDS, listId));
//                service.signRequest(token, cardRequest);
//                Response cardResponse = service.execute(cardRequest);
//
//                if (!cardResponse.isSuccessful()) continue;
//
//                List<Map<String, Object>> cards = new ObjectMapper().readValue(cardResponse.getBody(), List.class);
//
//                // 4. Add simplified card info with list name
//                for (Map<String, Object> card : cards) {
//                    allCards.add(Map.of(
//                            "id", card.get("id"),
//                            "name", card.get("name"),
//                            "url", card.get("url"),
//                            "list", listName
//                    ));
//                }
//            }
//
//            return allCards;
//
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to fetch cards in board", e);
//        }
//    }

//    public List<TrelloCardResponse> getCardDetailsFromBoards(TrelloListOrCardGetRequest request) {
//        PlatformCredential credential = platformCredentialRepository
//                .findByPlatformEmailAndType(request.getTrelloEmail(), PlatformConstant.TRELLO)
//                .orElseThrow(() -> new RuntimeException("Trello credentials not found"));
//
//        OAuth1AccessToken token = new OAuth1AccessToken(
//                credential.getPlatformToken().getAccessToken(),
//                credential.getPlatformToken().getAccessTokenSecret()
//        );
//
//        List<TrelloCardResponse> allCards = new ArrayList<>();
//
//        for (String boardId : request.getBoardIds()) {
//            try {
//                // 1. Get lists in board
//                OAuthRequest listRequest = new OAuthRequest(Verb.GET, String.format(PlatformConstant.TrelloConstant.BOARD_LISTS, boardId));
//                service.signRequest(token, listRequest);
//                Response listResponse = service.execute(listRequest);
//                List<Map<String, Object>> lists = new ObjectMapper().readValue(listResponse.getBody(), List.class);
//
//                for (Map<String, Object> list : lists) {
//                    String listId = (String) list.get("id");
//                    String listName = (String) list.get("name");
//
//                    // 2. Get cards in each list
//                    OAuthRequest cardRequest = new OAuthRequest(Verb.GET, String.format(PlatformConstant.TrelloConstant.LIST_CARDS, listId));
//                    service.signRequest(token, cardRequest);
//                    Response cardResponse = service.execute(cardRequest);
//
//                    List<Map<String, Object>> cards = new ObjectMapper().readValue(cardResponse.getBody(), List.class);
//
//                    for (Map<String, Object> card : cards) {
//                        allCards.add(new TrelloCardResponse(
//                                (String) card.get("id"),
//                                (String) card.get("name"),
//                                (String) card.get("url"),
//                                listName
//                        ));
//                    }
//                }
//
//            } catch (Exception e) {
//                // Optionally log and continue with next board
//                throw new RuntimeException("Failed to fetch cards from board: " + boardId, e);
//            }
//        }
//
//        return allCards;
//    }
public List<TrelloCardResponse> getCardsFromListIds(TrelloListOrCardGetRequest request) {
    PlatformCredential credential = platformCredentialRepository
            .findByPlatformEmailAndType(request.getTrelloEmail(), PlatformConstant.TRELLO)
            .orElseThrow(() -> new RuntimeException("Trello credentials not found"));

    OAuth1AccessToken token = new OAuth1AccessToken(
            credential.getTokens().getAccessToken(),
            credential.getTokens().getAccessTokenSecret()
    );

    List<TrelloCardResponse> allCards = new ArrayList<>();

    for (String listId : request.getListIds()) {
        try {
            // Step 1: Get list details (to fetch name)
            OAuthRequest listInfoRequest = new OAuthRequest(
                    Verb.GET,
                    String.format(PlatformConstant.TrelloConstant.LIST_DETAIL, listId)
            );
            service.signRequest(token, listInfoRequest);
            Response listInfoResponse = service.execute(listInfoRequest);

            if (!listInfoResponse.isSuccessful()) continue;

            Map<String, Object> listInfo = objectMapper.readValue(listInfoResponse.getBody(), Map.class);
            String listName = (String) listInfo.get("name");

            // Step 2: Get cards in list
            OAuthRequest cardRequest = new OAuthRequest(
                    Verb.GET,
                    String.format(PlatformConstant.TrelloConstant.LIST_CARDS, listId)
            );
            service.signRequest(token, cardRequest);
            Response cardResponse = service.execute(cardRequest);

            if (!cardResponse.isSuccessful()) continue;

            List<Map<String, Object>> cards = objectMapper.readValue(cardResponse.getBody(), List.class);

            // Step 3: Map each card with list info
            for (Map<String, Object> card : cards) {
                allCards.add(new TrelloCardResponse(
                        (String) card.get("id"),
                        (String) card.get("name"),
                        (String) card.get("url"),
                        listName,
                        listId
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get cards from list " + listId, e);
        }
    }

    return allCards;
}

    public Map<String, Object> createCardInBoard(String listId, String trelloEmail, String cardName, String cardDesc) {
        PlatformCredential credential = platformCredentialRepository
                .findByPlatformEmailAndType(trelloEmail, PlatformConstant.TRELLO)
                .orElseThrow(() -> new RuntimeException("Trello credentials not found"));

        OAuth1AccessToken token = new OAuth1AccessToken(
                credential.getTokens().getAccessToken(),
                credential.getTokens().getAccessTokenSecret()
        );

        try {
            OAuthRequest request = new OAuthRequest(Verb.POST, PlatformConstant.TrelloConstant.LIST_CARDS_NO_QUERY);

            request.addParameter("idList", listId);           // required
            request.addParameter("name", cardName);           // required
            request.addParameter("desc", cardDesc);           // optional
            request.addParameter("pos", "top");               // optional: top, bottom, or numeric

            service.signRequest(token, request);
            Response response = service.execute(request);

            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to create card: " + response.getMessage());
            }

            return new ObjectMapper().readValue(response.getBody(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error while creating card in Trello", e);
        }
    }

    public Map<String, List<Map<String, Object>>> getListsFromBoards(TrelloListOrCardGetRequest request) {
        PlatformCredential credential = platformCredentialRepository
                .findByPlatformEmailAndType(request.getTrelloEmail(), PlatformConstant.TRELLO)
                .orElseThrow(() -> new RuntimeException("Trello credentials not found"));

        OAuth1AccessToken token = new OAuth1AccessToken(
                credential.getTokens().getAccessToken(),
                credential.getTokens().getAccessTokenSecret()
        );

        Map<String, List<Map<String, Object>>> boardListsMap = new HashMap<>();

        for (String boardId : request.getBoardIds()) {
            try {
                OAuthRequest trelloRequest = new OAuthRequest(Verb.GET,
                        String.format(PlatformConstant.TrelloConstant.BOARD_LISTS, boardId));

                service.signRequest(token, trelloRequest);
                Response response = service.execute(trelloRequest);

                if (!response.isSuccessful()) continue;

                List<Map<String, Object>> lists = objectMapper.readValue(response.getBody(), List.class);

                List<Map<String, Object>> simplified = lists.stream()
                        .map(list -> Map.of(
                                "id", list.get("id"),
                                "name", list.get("name")
                        ))
                        .collect(Collectors.toList());

                boardListsMap.put(boardId, simplified);

            } catch (Exception e) {
                throw new RuntimeException("Error getting lists from board: " + boardId, e);
            }
        }

        return boardListsMap;
    }

    public Map<String, Object> createCard(TrelloCardCreateRequest request) {
        PlatformCredential credential = this.getCredential(request.getTrelloEmail());
        OAuth1AccessToken token = buildToken(credential);

        OAuthRequest cardRequest = new OAuthRequest(Verb.POST, PlatformConstant.TrelloConstant.LIST_CARDS_NO_QUERY);
        cardRequest.addParameter("idList", request.getListId());
        cardRequest.addParameter("name", request.getName());
        cardRequest.addParameter("desc", request.getDescription());

        service.signRequest(token, cardRequest);

        try {
            Response response = service.execute(cardRequest);
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to create card: " + response.getMessage());
            }
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Create card failed", e);
        }
    }

    public Map<String, Object> updateCard(TrelloCardUpdateRequest request) {
        PlatformCredential credential = getCredential(request.getTrelloEmail());
        OAuth1AccessToken token = buildToken(credential);

        String url = PlatformConstant.TrelloConstant.LIST_CARDS_NO_QUERY + "/" + request.getCardId();
        OAuthRequest updateRequest = new OAuthRequest(Verb.PUT, url);
        updateRequest.addParameter("name", request.getName());
        updateRequest.addParameter("desc", request.getDescription());

        service.signRequest(token, updateRequest);

        try {
            Response response = service.execute(updateRequest);
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to update card: " + response.getMessage());
            }
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Update card failed", e);
        }
    }

    public boolean deleteCard(String trelloEmail, String cardId) {
        PlatformCredential credential = getCredential(trelloEmail);
        OAuth1AccessToken token = buildToken(credential);

        String url = PlatformConstant.TrelloConstant.LIST_CARDS_NO_QUERY + "/" + cardId;
        OAuthRequest deleteRequest = new OAuthRequest(Verb.DELETE, url);

        service.signRequest(token, deleteRequest);

        try {
            Response response = service.execute(deleteRequest);
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to delete card: " + response.getMessage());
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Delete card failed", e);
        }
    }

    private PlatformCredential getCredential(String trelloEmail) {
        return platformCredentialRepository.findByPlatformEmailAndType(trelloEmail, PlatformConstant.TRELLO)
                .orElseThrow(() -> new RuntimeException("Trello credentials not found"));
    }

    private OAuth1AccessToken buildToken(PlatformCredential credential) {
        return new OAuth1AccessToken(
                credential.getTokens().getAccessToken(),
                credential.getTokens().getAccessTokenSecret()
        );
    }

}
