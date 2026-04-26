package tn.esprit.spring.baladna.marketplace.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@RestController
@RequestMapping("/api/video")
@CrossOrigin(origins = "*")
public class VideoController {

    private static final String PYTHON_VIDEO_URL = "http://localhost:8001/generate-video";

    @PostMapping("/generate")
    public ResponseEntity<?> generateVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("productName") String productName,
            @RequestParam("price") double price,
            @RequestParam(value = "category", defaultValue = "handicraft") String category,
            @RequestParam(value = "description", defaultValue = "") String description) {

        try {
            RestTemplate restTemplate = new RestTemplate();

            // Build multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("productName", productName);
            body.add("price", String.valueOf(price));
            body.add("category", category);
            body.add("description", description);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Send to Python server
            ResponseEntity<byte[]> pythonResponse = restTemplate.exchange(
                    PYTHON_VIDEO_URL,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            if (pythonResponse.getStatusCode() == HttpStatus.OK && pythonResponse.getBody() != null) {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                responseHeaders.setContentDisposition(
                        ContentDisposition.attachment()
                                .filename(productName.replace(" ", "_") + "_showcase.mp4")
                                .build()
                );

                return new ResponseEntity<>(pythonResponse.getBody(), responseHeaders, HttpStatus.OK);
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Python server returned empty response"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}