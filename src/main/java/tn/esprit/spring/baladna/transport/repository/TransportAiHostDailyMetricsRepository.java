package tn.esprit.spring.baladna.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.transport.entity.TransportAiHostDailyMetrics;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TransportAiHostDailyMetricsRepository extends JpaRepository<TransportAiHostDailyMetrics, Long> {

    Optional<TransportAiHostDailyMetrics> findByHostEmailAndMetricDate(String email, LocalDate metricDate);

    long countByHostEmail(String email);
}
