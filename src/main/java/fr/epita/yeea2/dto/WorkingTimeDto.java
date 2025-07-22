package fr.epita.yeea2.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class WorkingTimeDto {
    Instant startTime;
    Instant endTime;
}
