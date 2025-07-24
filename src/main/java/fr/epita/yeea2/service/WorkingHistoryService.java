package fr.epita.yeea2.service;

import fr.epita.yeea2.dto.SumarizationDto;
import fr.epita.yeea2.dto.TaskChangedDto;
import fr.epita.yeea2.dto.WorkingTimeDto;
import fr.epita.yeea2.entity.WorkingHistory;
import fr.epita.yeea2.repository.WorkingHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class WorkingHistoryService {
    @Autowired
    private WorkingHistoryRepository workingHistoryRepository;

    public WorkingHistory changeWorkingHistory(String userId) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        WorkingHistory workingHistory = workingHistoryRepository.findByUserIdAndDay(userId, today).orElse(null);
        if (workingHistory != null) {
            List<WorkingTimeDto> workingTimes = workingHistory.getWorkTimeHistory();
            //if they are working -> set endTime because they want to pause
            long addedWorkingTime = 0;
            if (workingHistory.isWorking()) {
                for (WorkingTimeDto workingTimeDto : workingTimes) {
                    if (workingTimeDto.getEndTime()==null) {
                        Instant endTime = new Date().toInstant();
                        workingTimeDto.setEndTime(endTime);
                        addedWorkingTime = endTime.toEpochMilli() - workingTimeDto.getStartTime().toEpochMilli();
                        break;
                    }
                }
                workingHistory.setWorkTimeHistory(workingTimes);
                workingHistory.getSumarization().setWorkingDuration(workingHistory.getSumarization().getWorkingDuration()+addedWorkingTime);
            } else { //if they want to start to work again
                WorkingTimeDto workingTimeDto = new WorkingTimeDto();
                Instant newStartTime = new Date().toInstant();
                workingTimeDto.setStartTime(newStartTime);
                if (workingTimes.size()>0){
                    Instant lastEndTime = workingHistory.getWorkTimeHistory().get(workingTimes.size()-1).getEndTime();
                    long newBreakDuration = newStartTime.toEpochMilli() - lastEndTime.toEpochMilli();
                    workingHistory.getSumarization().setBreakDuration(workingHistory.getSumarization().getBreakDuration()+newBreakDuration);
                    workingHistory.getSumarization().setNumberOfBreaks(workingHistory.getSumarization().getNumberOfBreaks()+1);
                }
                workingTimes.add(workingTimeDto);
            }
            workingHistory.setWorking(!workingHistory.isWorking());
        }
        else{
            workingHistory = new WorkingHistory();
            workingHistory.setUserId(userId);
            workingHistory.setDay(today);
            workingHistory.setWorking(true);
            List<WorkingTimeDto> history = new ArrayList<>();
            WorkingTimeDto workingTimeDto = new WorkingTimeDto();
            workingTimeDto.setStartTime(new Date().toInstant());
            history.add(workingTimeDto);
            workingHistory.setWorkTimeHistory(history);
            SumarizationDto sumarizationDto = new SumarizationDto();
            workingHistory.setSumarization(sumarizationDto);
        }
        return workingHistoryRepository.save(workingHistory);
    }

    public void changeTaskStatus(String issueOrCardKey, String newStatus, String platform, String userId) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        WorkingHistory workingHistory = workingHistoryRepository.findByUserIdAndDay(userId, today).orElse(null);
        Instant now = Instant.now();
        TaskChangedDto taskChangedDto = TaskChangedDto.builder()
                .taskKey(issueOrCardKey)
                .updatedStatus(newStatus)
                .platform(platform)
                .updatedAt(now)
                .build();
        if (workingHistory != null) {
            List<TaskChangedDto> taskChangedHistory = workingHistory.getTaskChangedHistory();
            if (!taskChangedHistory.isEmpty()) {
                //check if the status change for task is less than 15mins to add to context switching
                if (now.toEpochMilli()-taskChangedHistory.get(taskChangedHistory.size()-1).getUpdatedAt().toEpochMilli()<15*60*1000) {
                    workingHistory.getSumarization().setContextSwitching(workingHistory.getSumarization().getContextSwitching() + 1);
                }
            }
            taskChangedHistory.add(taskChangedDto);
        } else {
            workingHistory = new WorkingHistory();
            List<TaskChangedDto> taskChangedHistory = new ArrayList<>();
            taskChangedHistory.add(taskChangedDto);
            workingHistory.setTaskChangedHistory(taskChangedHistory);
            workingHistory.getSumarization().setContextSwitching(1);
            workingHistory.setDay(today);
            workingHistory.setUserId(userId);
        }
        workingHistoryRepository.save(workingHistory);
    }
}
