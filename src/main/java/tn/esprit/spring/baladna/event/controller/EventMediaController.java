package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.spring.baladna.event.dto.EventMediaDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.EventMedia;
import tn.esprit.spring.baladna.event.entity.enums.MediaType;
import tn.esprit.spring.baladna.event.repository.EventMediaRepository;
import tn.esprit.spring.baladna.event.service.impl.evCloudinaryService;
import tn.esprit.spring.baladna.event.service.interfaces.EventService;
import tn.esprit.spring.baladna.event.service.interfaces.IEventMediaService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events")
public class EventMediaController {

    private final IEventMediaService mediaService;
    private final EventService eventService;
    private final EventMediaRepository mediaRepository;
    private final evCloudinaryService evCloudinaryService;

    @GetMapping("/event-media/list")
    public List<EventMedia> retrieveEventMedias() {
        return mediaService.retrieveEventMedias();
    }

    @GetMapping("/event-media/get/{id}")
    public EventMedia retrieveEventMedia(@PathVariable Long id) {
        return mediaService.retrieveEventMedia(id);
    }

    @PostMapping("/event-media/add")
    public EventMedia addEventMedia(@RequestBody EventMediaDTO media) {
        return mediaService.addEventMedia(media);
    }

    @PutMapping("/event-media/update/{id}")
    public EventMedia updateEventMedia(@PathVariable Long id, @RequestBody EventMediaDTO media) {
        media.setId(id);
        return mediaService.updateEventMedia(media);
    }

    @DeleteMapping("/event-media/delete/{id}")
    public void removeEventMedia(@PathVariable Long id) {
        mediaService.removeEventMedia(id);
    }

    // Get media for a specific event
    @GetMapping("/{eventId}/media")
    public List<EventMedia> getEventMedia(@PathVariable Long eventId) {
        return mediaRepository.findByEventIdOrderByOrderIndexAsc(eventId);
    }

    // Upload media to Cloudinary
    @PostMapping("/{eventId}/media")
    public ResponseEntity<?> uploadEventMedia(
            @PathVariable Long eventId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "coverIndex", required = false, defaultValue = "0") int coverIndex,
            @RequestParam(value = "order", required = false) String orderJson) {
        
        try {
            // Get the event
            Event event = eventService.retrieveEvent(eventId);
            if (event == null) {
                return ResponseEntity.badRequest().body("Event not found");
            }
            
            // Parse order if provided (simple parsing without Jackson)
            List<Integer> order = new ArrayList<>();
            if (orderJson != null && !orderJson.isEmpty()) {
                // Parse simple JSON array format: [0,1,2,3]
                String cleaned = orderJson.replaceAll("[\\[\\]\\s]", "");
                if (!cleaned.isEmpty()) {
                    order = Arrays.stream(cleaned.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                }
            }
            
            List<EventMedia> savedMedia = new ArrayList<>();
            
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                
                // Upload to Cloudinary
                String cloudinaryUrl = evCloudinaryService.uploadFile(file);
                MediaType mediaType = evCloudinaryService.getMediaType(file);
                
                // Create EventMedia entity
                EventMedia media = EventMedia.builder()
                        .event(event)
                        .url(cloudinaryUrl)
                        .type(mediaType)
                        .isCover(i == coverIndex)
                        .orderIndex(order.isEmpty() ? i : order.get(i))
                        .build();
                
                savedMedia.add(mediaRepository.save(media));
            }
            
            // Return a clean response
            List<Map<String, Object>> response = new ArrayList<>();
            for (EventMedia m : savedMedia) {
                Map<String, Object> mediaMap = new HashMap<>();
                mediaMap.put("id", m.getId());
                mediaMap.put("url", m.getUrl());
                mediaMap.put("type", m.getType().name());
                mediaMap.put("isCover", m.getIsCover());
                mediaMap.put("orderIndex", m.getOrderIndex());
                mediaMap.put("eventId", m.getEvent().getId());
                response.add(mediaMap);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload files: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // Delete media from Cloudinary and database
    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<?> deleteEventMedia(@PathVariable Long mediaId) {
        try {
            EventMedia media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found"));
            
            // Extract public ID from Cloudinary URL (simplified - you may need to adjust)
            // Cloudinary URLs are like: https://res.cloudinary.com/dsc0lmdgm/image/upload/v1234567890/baladna_events/abc123.jpg
            // The public ID would be: baladna_events/abc123
            
            // Delete from Cloudinary (optional - you might want to keep files)
            // cloudinaryService.deleteFile(publicId);
            
            // Delete from database
            mediaRepository.delete(media);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting media: " + e.getMessage());
        }
    }

    // Update media order
    @PutMapping("/{eventId}/media/order")
    public ResponseEntity<?> updateMediaOrder(
            @PathVariable Long eventId,
            @RequestBody List<Map<String, Object>> mediaOrderUpdates) {
        try {
            for (Map<String, Object> update : mediaOrderUpdates) {
                if (update.get("id") == null || update.get("orderIndex") == null) {
                    return ResponseEntity.badRequest().body("Each media update must include non-null 'id' and 'orderIndex'.");
                }
                Long mediaId;
                Integer orderIndex;
                try {
                    mediaId = Long.parseLong(update.get("id").toString());
                    orderIndex = Integer.parseInt(update.get("orderIndex").toString());
                } catch (Exception parseEx) {
                    return ResponseEntity.badRequest().body("Invalid 'id' or 'orderIndex' value: " + parseEx.getMessage());
                }
                Boolean isCover = (Boolean) update.getOrDefault("isCover", false);

                EventMedia media = mediaRepository.findById(mediaId).orElse(null);
                if (media != null) {
                    media.setOrderIndex(orderIndex);
                    media.setIsCover(isCover);
                    mediaRepository.save(media);
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating media order: " + e.getMessage());
        }
    }
    }
