package fr.epita.yeea2.entity;

import fr.epita.yeea2.constant.RecoveryRecommendationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "burnout_status")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BurnoutStatus {
    @Id
    private String id;
    private String userId;
    private int burnoutScore;
    private String riskLevel;
    private RecoveryRecommendationType recommendationType;
    private LocalDate calculatedDate;
}
