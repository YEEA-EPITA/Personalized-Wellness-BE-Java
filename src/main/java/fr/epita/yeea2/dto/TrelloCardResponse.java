package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrelloCardResponse {
    private String id;
    private String name;
    private String url;
    private String list; // List name
}
