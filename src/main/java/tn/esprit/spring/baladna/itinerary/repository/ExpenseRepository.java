package tn.esprit.spring.baladna.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.itinerary.entity.Expense;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    // All expenses for an itinerary
    List<Expense> findByItineraryId(UUID itineraryId);

    // All expenses paid by a specific user in an itinerary
    List<Expense> findByItineraryIdAndPaidByUserId(UUID itineraryId, Long userId);

    // Total amount paid by a user in an itinerary
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.itinerary.id = :itineraryId
            AND e.paidByUserId = :userId
            """)
    BigDecimal sumAmountByItineraryIdAndPaidByUserId(
            @Param("itineraryId") UUID itineraryId,
            @Param("userId") Long userId);

    // Total of ALL expenses in an itinerary
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.itinerary.id = :itineraryId")
    BigDecimal sumTotalByItineraryId(@Param("itineraryId") UUID itineraryId);
}