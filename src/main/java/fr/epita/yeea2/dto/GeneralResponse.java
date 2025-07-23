package fr.epita.yeea2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GeneralResponse {
    private int statusCode;
    private String message;
}
