package tn.esprit.spring.baladna.event.service.payment;

import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.entity.EventReservation;

@Service
public class QrService {

    private static final String SECRET = "change-this-secret";

    // Generate a signed token string for validation
    public String generateToken(EventReservation r) {
        long expiry = r.getEvent().getStartAt()
                .plusHours(2)
                .toEpochSecond(java.time.ZoneOffset.UTC);

        String payload = r.getId() + "|" + r.getEvent().getId() + "|" + expiry;
        String signature = sign(payload);

        return payload + "|" + signature;
    }

    // Generate a base64 PNG QR image from the token string
    public String generateQrImageBase64(String qrToken) {
        try {
            com.google.zxing.common.BitMatrix matrix = new com.google.zxing.MultiFormatWriter()
                    .encode(qrToken, com.google.zxing.BarcodeFormat.QR_CODE, 250, 250);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("QR image generation failed: " + e.getMessage());
        }
    }

    // Legacy: generate() is now an alias for generateToken()
    public String generate(EventReservation r) {
        return generateToken(r);
    }

    public boolean verify(String qr) {
        String[] p = qr.split("\\|");
        if (p.length != 4) return false;

        String payload = p[0] + "|" + p[1] + "|" + p[2];
        String signature = p[3];

        if (!sign(payload).equals(signature)) return false;

        long expiry = Long.parseLong(p[2]);
        return expiry >= java.time.Instant.now().getEpochSecond();
    }

    public String sign(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((data + SECRET).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
