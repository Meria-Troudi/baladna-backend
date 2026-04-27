package tn.esprit.spring.baladna.event.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.spring.baladna.event.entity.enums.MediaType;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    @Value("${cloudinary.image.max-size:5242880}")
    private long imageMaxSize;

    @Value("${cloudinary.video.max-size:20971520}")
    private long videoMaxSize;

    /**
     * Upload a file to Cloudinary and return the secure URL
     */
    public String uploadFile(MultipartFile file) throws IOException {
        validateFileSize(file);
        
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", "baladna_events"
                    )
            );
            
            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            throw new IOException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Validate file size before upload
     */
    private void validateFileSize(MultipartFile file) {
        String contentType = file.getContentType();
        long maxSize = isVideo(contentType) ? videoMaxSize : imageMaxSize;
        
        if (file.getSize() > maxSize) {
            String type = isVideo(contentType) ? "video" : "image";
            long maxSizeMB = maxSize / (1024 * 1024);
            throw new IllegalArgumentException(
                    String.format("File too large. Maximum %s size is %d MB", type, maxSizeMB)
            );
        }
    }

    /**
     * Determine media type from content type
     */
    public MediaType getMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("video")) {
            return MediaType.VIDEO;
        }
        return MediaType.IMAGE;
    }

    /**
     * Check if content type is a video
     */
    private boolean isVideo(String contentType) {
        return contentType != null && contentType.startsWith("video");
    }

    /**
     * Delete a file from Cloudinary by public ID
     */
    public boolean deleteFile(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (IOException e) {
            return false;
        }
    }
}