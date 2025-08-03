package fr.epita.yeea2.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SumarizationDto {
    long workingDuration = 0;
    long breakDuration = 0;
    int contextSwitching = 0;
    int numberOfBreaks = 0;
}
