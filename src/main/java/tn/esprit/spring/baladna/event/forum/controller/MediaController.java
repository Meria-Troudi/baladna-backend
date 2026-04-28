package tn.esprit.spring.baladna.event.forum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.spring.baladna.event.forum.service.MediaUploadService;

import java.util.Map;

@RestController
@RequestMapping("/api/forum/media")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MediaController {

    private final MediaUploadService service;

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam MultipartFile file) {
        String url = service.upload(file);
        return Map.of("url", url);
    }
}