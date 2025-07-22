package fr.epita.yeea2.entity;

import fr.epita.yeea2.dto.SumarizationDto;
import fr.epita.yeea2.dto.TaskChangedDto;
import fr.epita.yeea2.dto.WorkingTimeDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "working-history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkingHistory {

    @Id
    private String id;
    private String day;
    private List<WorkingTimeDto> workTimeHistory = new ArrayList<>();
    private SumarizationDto sumarization = new SumarizationDto();
    private String userId;
    private Instant updatedAt;
    private boolean isWorking;
    private List<TaskChangedDto> taskChangedHistory = new ArrayList<>();
}
