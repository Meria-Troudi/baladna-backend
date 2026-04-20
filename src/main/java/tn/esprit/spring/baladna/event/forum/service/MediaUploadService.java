package tn.esprit.spring.baladna.event.forum.service;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MediaUploadService {

    private final Cloudinary cloudinary;

    public String upload(MultipartFile file) {
        try {
            Map res = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of("resource_type", "auto")
            );
            return res.get("secure_url").toString();
        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }
    }
}