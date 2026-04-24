-- Use when Hibernate logs errors such as:
--   "only one auto column and it must be defined as a key"
--   "Cannot change column 'event_id': used in a foreign key constraint"
--   errno 150 on event_media foreign keys
-- Back up data first if you need existing event_media rows.

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS event_media;
SET FOREIGN_KEY_CHECKS = 1;

-- Restart the Spring Boot app with spring.jpa.hibernate.ddl-auto=update
-- so JPA recreates event_media to match EventMedia (Long id, event_id FK, etc.).
