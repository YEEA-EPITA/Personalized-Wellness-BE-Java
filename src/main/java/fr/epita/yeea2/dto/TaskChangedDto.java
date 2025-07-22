package fr.epita.yeea2.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TaskChangedDto {
    private String taskKey;
    private String platform;
    private String updatedStatus;
    private Instant updatedAt;
}
