package tn.esprit.spring.baladna.itinerary.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.itinerary.entity.ChatMessage;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ChatMessage entity
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Find all non-deleted messages for a specific itinerary, ordered by creation date
     *
     * @param itineraryId the itinerary ID
     * @param pageable    pagination information
     * @return paginated list of messages
     */
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.itinerary.id = :itineraryId AND m.isDeleted = false " +
            "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByItineraryIdAndNotDeleted(
            @Param("itineraryId") UUID itineraryId,
            Pageable pageable
    );

    /**
     * Find all messages for an itinerary (including deleted ones)
     *
     * @param itineraryId the itinerary ID
     * @param pageable    pagination information
     * @return paginated list of messages
     */
    Page<ChatMessage> findByItinerary_Id(UUID itineraryId, Pageable pageable);

    /**
     * Get the latest message for an itinerary
     *
     * @param itineraryId the itinerary ID
     * @return the latest message or null
     */
    @Query(value = "SELECT * FROM chat_message " +
            "WHERE itinerary_id = :itineraryId AND is_deleted = false " +
            "ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    ChatMessage findLatestMessageByItineraryId(@Param("itineraryId") UUID itineraryId);

    /**
     * Get all messages sent by a specific user in an itinerary
     *
     * @param itineraryId the itinerary ID
     * @param senderId    the sender's user ID
     * @return list of messages
     */
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.itinerary.id = :itineraryId AND m.senderId = :senderId AND m.isDeleted = false " +
            "ORDER BY m.createdAt DESC")
    List<ChatMessage> findByItineraryIdAndSenderId(
            @Param("itineraryId") UUID itineraryId,
            @Param("senderId") Long senderId
    );

    /**
     * Count non-deleted messages in an itinerary
     *
     * @param itineraryId the itinerary ID
     * @return count of messages
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
            "WHERE m.itinerary.id = :itineraryId AND m.isDeleted = false")
    Long countByItineraryIdAndNotDeleted(@Param("itineraryId") UUID itineraryId);
}
