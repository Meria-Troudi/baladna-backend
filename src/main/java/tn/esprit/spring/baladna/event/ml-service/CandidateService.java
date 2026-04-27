package tn.esprit.spring.baladna.event.service;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CandidateService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Returns a list of upcoming event IDs that the user has not already reserved (CONFIRMED or WAITLISTED).
     *
     * @param userId the ID of the user
     * @return list of candidate event IDs
     */
    public List<Long> getCandidates(Long userId) {
        String sql = """
                SELECT e.id
                FROM event e
                WHERE e.status = 'UPCOMING'
                  AND e.start_at > NOW()
                  AND e.id NOT IN (
                      SELECT event_id
                      FROM event_reservation
                      WHERE user_id = ?
                        AND status IN ('CONFIRMED', 'WAITLISTED')
                  )
                ORDER BY e.start_at ASC
                LIMIT 300
                """;

        return jdbcTemplate.queryForList(sql, Long.class, userId);
    }
}