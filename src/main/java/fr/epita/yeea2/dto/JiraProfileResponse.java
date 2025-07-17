package fr.epita.yeea2.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JiraProfileResponse {
    private String email;
    private String name;
    private String imgUrl;
}
