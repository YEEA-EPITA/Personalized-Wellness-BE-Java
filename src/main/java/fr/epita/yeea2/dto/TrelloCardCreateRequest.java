package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrelloCardCreateRequest {
    private String trelloEmail;
    private String listId;
    private String name;
    private String description;
}
