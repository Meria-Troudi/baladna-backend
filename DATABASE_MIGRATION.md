# Database Migration: Training Dataset

This file contains SQL scripts to set up the database schema for the AI Recommendation System.

## MySQL Migration Script

Run this script against your `baladna` database to create the `training_dataset` table.

```sql
-- Create training_dataset table for AI recommendation system
CREATE TABLE IF NOT EXISTS training_dataset (
    id CHAR(36) NOT NULL PRIMARY KEY COMMENT 'UUID',
    itinerary_id CHAR(36) NOT NULL COMMENT 'Reference to itinerary table',
    budget DECIMAL(10, 2) NOT NULL COMMENT 'Total trip budget',
    location VARCHAR(100) NOT NULL COMMENT 'Destination region',
    duration_days INT NOT NULL COMMENT 'Trip duration in days',
    avg_daily_cost DECIMAL(10, 2) COMMENT 'Average cost per day',
    num_steps INT NOT NULL COMMENT 'Number of activities/steps',
    num_collaborators INT NOT NULL COMMENT 'Number of people in the trip',
    num_expenses INT NOT NULL COMMENT 'Total number of expense entries',
    rating DECIMAL(3, 2) COMMENT 'Calculated quality rating (0.0-5.0)',
    normalized_budget DECIMAL(5, 4) COMMENT 'Normalized budget (0.0-1.0)',
    normalized_duration DECIMAL(5, 4) COMMENT 'Normalized duration (0.0-1.0)',
    normalized_complexity DECIMAL(5, 4) COMMENT 'Normalized complexity (0.0-1.0)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    last_used_at TIMESTAMP NULL COMMENT 'Last time this recommendation was used',
    
    -- Foreign key constraint
    CONSTRAINT fk_training_dataset_itinerary 
        FOREIGN KEY (itinerary_id) 
        REFERENCES itinerary(id) 
        ON DELETE CASCADE,
    
    -- Indexes for query optimization
    INDEX idx_location (location),
    INDEX idx_budget (budget),
    INDEX idx_duration (duration_days),
    INDEX idx_rating (rating),
    INDEX idx_itinerary_id (itinerary_id),
    INDEX idx_created_at (created_at)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Training dataset for AI-powered itinerary recommendations';

-- Create index for range queries
CREATE INDEX idx_budget_range ON training_dataset(budget, location);
CREATE INDEX idx_duration_range ON training_dataset(duration_days, location);
```

## Rollback Migration

If you need to remove the table:

```sql
DROP TABLE IF EXISTS training_dataset;
```

## Verification

After running the migration, verify the table was created:

```sql
-- Check if table exists
SHOW TABLES LIKE 'training_dataset';

-- Check table structure
DESCRIBE training_dataset;

-- Check indexes
SHOW INDEX FROM training_dataset;
```

Expected output:
```
Field                      Type           Null  Key  Default            Extra
id                         char(36)       NO    PRI  NULL               
itinerary_id               char(36)       NO    MUL  NULL               
budget                     decimal(10,2)  NO         NULL               
location                   varchar(100)   NO    MUL  NULL               
duration_days              int            NO    MUL  NULL               
avg_daily_cost             decimal(10,2)  YES        NULL               
num_steps                  int            NO         NULL               
num_collaborators          int            NO         NULL               
num_expenses               int            NO         NULL               
rating                     decimal(3,2)   YES    MUL  NULL               
normalized_budget          decimal(5,4)   YES        NULL               
normalized_duration        decimal(5,4)   YES        NULL               
normalized_complexity      decimal(5,4)   YES        NULL               
created_at                 timestamp      NO         CURRENT_TIMESTAMP  
last_used_at               timestamp      YES        NULL               
```

## Hibernate Auto-DDL

Alternatively, if you're using Hibernate's `ddl-auto=update` (as configured in your project):

1. The table will be automatically created when you first run the application
2. Ensure `spring.jpa.hibernate.ddl-auto=update` in `application.properties`
3. The `TrainingDataset` entity will generate the schema automatically

**Note**: This is already enabled in your project, so manual migration may not be necessary.

## Backup Before Updates

If upgrading an existing schema:

```sql
-- Backup existing data
CREATE TABLE training_dataset_backup 
AS SELECT * FROM training_dataset;

-- Then run migration scripts above
```

## Sample Data Query

After creating the table, you can verify it's working:

```sql
-- Count training records
SELECT COUNT(*) as total_records FROM training_dataset;

-- View statistics
SELECT 
    COUNT(*) as total_records,
    ROUND(AVG(budget), 2) as avg_budget,
    ROUND(AVG(duration_days), 1) as avg_duration,
    ROUND(AVG(rating), 2) as avg_rating,
    COUNT(DISTINCT location) as unique_locations
FROM training_dataset;

-- Find best rated destinations
SELECT 
    location,
    COUNT(*) as count,
    ROUND(AVG(budget), 2) as avg_budget,
    ROUND(AVG(rating), 2) as avg_rating
FROM training_dataset
WHERE rating IS NOT NULL
GROUP BY location
ORDER BY avg_rating DESC;
```

## Performance Tuning

For large datasets (10,000+ records), consider:

```sql
-- Add partitioning by location for very large tables
ALTER TABLE training_dataset 
PARTITION BY KEY(location) 
PARTITIONS 10;

-- Or by date
ALTER TABLE training_dataset 
PARTITION BY RANGE(YEAR(created_at)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);

-- Check partition status
SELECT PARTITION_NAME, PARTITION_EXPRESSION, PARTITION_DESCRIPTION 
FROM INFORMATION_SCHEMA.PARTITIONS 
WHERE TABLE_NAME = 'training_dataset';
```

## Monitoring Table Size

```sql
-- Check table size
SELECT 
    ROUND(((data_length + index_length) / 1024 / 1024), 2) as size_mb
FROM information_schema.TABLES 
WHERE table_schema = 'baladna' 
AND table_name = 'training_dataset';

-- View table statistics
ANALYZE TABLE training_dataset;

-- Show statistics
SHOW TABLE STATUS WHERE Name = 'training_dataset'\G
```

## Common Queries After Setup

```sql
-- Find cheapest trips by location
SELECT location, MIN(budget) as cheapest, AVG(budget) as avg
FROM training_dataset
GROUP BY location
ORDER BY cheapest ASC;

-- Find highly rated trips
SELECT * FROM training_dataset
WHERE rating >= 4.0
ORDER BY rating DESC
LIMIT 10;

-- Find popular destinations
SELECT location, COUNT(*) as trip_count
FROM training_dataset
GROUP BY location
ORDER BY trip_count DESC;

-- Find trips by duration
SELECT 
    CASE 
        WHEN duration_days <= 3 THEN 'Weekend'
        WHEN duration_days <= 7 THEN 'Short'
        WHEN duration_days <= 14 THEN 'Medium'
        ELSE 'Long'
    END as trip_type,
    COUNT(*) as count,
    ROUND(AVG(budget), 2) as avg_budget
FROM training_dataset
GROUP BY trip_type;
```

## Cleanup Old Records

```sql
-- Remove old training data (older than 90 days)
DELETE FROM training_dataset
WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY)
AND last_used_at IS NULL;

-- Archive old records
CREATE TABLE training_dataset_archive AS
SELECT * FROM training_dataset
WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);

DELETE FROM training_dataset
WHERE id IN (SELECT id FROM training_dataset_archive);
```

## Troubleshooting

### Table Already Exists Error
```sql
-- Drop and recreate
DROP TABLE IF EXISTS training_dataset;
-- Then run the creation script above
```

### Foreign Key Constraint Error
```sql
-- Check if itinerary table exists
SHOW TABLES LIKE 'itinerary';

-- If not, ensure the itinerary table is created first
-- Check for orphaned records
SELECT COUNT(*) FROM training_dataset 
WHERE itinerary_id NOT IN (SELECT id FROM itinerary);
```

### Permission Denied Error
```sql
-- Grant permissions
GRANT ALL PRIVILEGES ON baladna.training_dataset TO 'your_user'@'localhost';
FLUSH PRIVILEGES;
```

## Verification Checklist

After running migration, verify:

- [ ] Table `training_dataset` exists
- [ ] All 14 columns are present
- [ ] Foreign key to `itinerary` is set
- [ ] 6 indexes are created
- [ ] Table engine is InnoDB
- [ ] Charset is utf8mb4
- [ ] Can insert test records
- [ ] Can query test records
