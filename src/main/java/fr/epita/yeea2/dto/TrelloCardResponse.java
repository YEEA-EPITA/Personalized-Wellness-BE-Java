package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrelloCardResponse {
    private String id;
    private String name;
    private String url;
    private String listName;
    private String listId;
    private String description;
    private String createdAt;
    private String updatedAt;
//    private String createdBy;
    private String dueDate;
}
