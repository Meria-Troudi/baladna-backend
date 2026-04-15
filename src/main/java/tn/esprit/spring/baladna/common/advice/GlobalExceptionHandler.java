package tn.esprit.spring.baladna.common.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", e.getReason() != null ? e.getReason() : e.getStatusCode().toString());
        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", humanize(rootMessage(e))));
    }

    @ExceptionHandler(JpaSystemException.class)
    public ResponseEntity<Map<String, Object>> handleJpa(JpaSystemException e) {
        log.error("JPA error", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", humanize(rootMessage(e))));
    }

    /**
     * Often caused by a VARCHAR/ENUM value in MySQL that does not match a Java enum (e.g. room type).
     * Return 400 with a clear message instead of a generic 500.
     */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDataAccess(InvalidDataAccessApiUsageException e) {
        String root = rootMessage(e);
        log.warn("Invalid JPA/data access: {}", root);
        if (root.contains("No enum constant")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message",
                            "Une donnée enregistrée ne correspond pas au format attendu. "
                                    + "Réessayez après redémarrage du serveur ou contactez le support."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", humanize(root)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Unhandled error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", humanize(rootMessage(e))));
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        int depth = 0;
        while (t.getCause() != null && t.getCause() != t && depth++ < 12) {
            t = t.getCause();
        }
        String m = t.getMessage();
        return m != null && !m.isBlank() ? m : e.getClass().getSimpleName();
    }

    private static String humanize(String raw) {
        if (raw == null) {
            return "Unexpected server error";
        }
        if (raw.contains("Data truncated for column") && raw.contains("type")) {
            return raw + " — If your MySQL column `type` (or `status`) is an ENUM, run: "
                    + "ALTER TABLE accommodations MODIFY COLUMN type VARCHAR(50); "
                    + "ALTER TABLE accommodations MODIFY COLUMN status VARCHAR(50); "
                    + "ALTER TABLE rooms MODIFY COLUMN type VARCHAR(50); "
                    + "then restart the app (or align ENUM values with GUEST_HOUSE, CAMPING, APARTMENT, FARM, OTHER, ACTIVE, INACTIVE, DRAFT, STANDARD, DOUBLE, SUITE, DORM, FAMILY, OTHER).";
        }
        return raw;
    }
}
