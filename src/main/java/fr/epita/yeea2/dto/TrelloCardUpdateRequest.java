package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrelloCardUpdateRequest {
    private String trelloEmail;
    private String cardId;
    private String name;
    private String description;
}
