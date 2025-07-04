package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrelloCardDeleteRequest {
    private String trelloEmail;
    private String cardId;
}
