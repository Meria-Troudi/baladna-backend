package tn.esprit.spring.baladna.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.user.entity.ActivityLog;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.ActivityLogRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActivityLogService {
    private final ActivityLogRepository repo;

    public void log(String action, User user){

        ActivityLog log= ActivityLog.builder()

                .action(action)

                .timestamp(LocalDateTime.now())

                .user(user)

                .build();

        repo.save(log);

    }
}
