-- Sets cover_image_file_name for the 20 seed accommodations from seed-20-tunisia-accommodations.sql
-- Filenames must match files created by scripts/download-seed-cover-images.ps1
-- (pattern: {uuid}_{timestamp}_guesthouse_cover.jpg with timestamp 1744020000001 .. 1744020000020)

SET NAMES utf8mb4;

UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8001-000000000001_1744020000001_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8001-000000000001','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8001-000000000002_1744020000002_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8001-000000000002','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8001-000000000003_1744020000003_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8001-000000000003','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8001-000000000004_1744020000004_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8001-000000000004','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8001-000000000005_1744020000005_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8001-000000000005','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8001-000000000006_1744020000006_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8001-000000000006','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8001-000000000007_1744020000007_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8001-000000000007','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8002-000000000008_1744020000008_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8002-000000000008','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8002-000000000009_1744020000009_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8002-000000000009','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8002-00000000000a_1744020000010_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000a','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8002-00000000000b_1744020000011_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000b','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8002-00000000000c_1744020000012_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000c','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8002-00000000000d_1744020000013_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000d','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8002-00000000000e_1744020000014_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000e','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8003-00000000000f_1744020000015_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8003-00000000000f','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8003-000000000010_1744020000016_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8003-000000000010','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8003-000000000011_1744020000017_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8003-000000000011','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8003-000000000012_1744020000018_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8003-000000000012','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8003-000000000013_1744020000019_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8003-000000000013','-',''));
UPDATE accommodations SET cover_image_file_name = 'a1000001-0001-4001-8003-000000000014_1744020000020_guesthouse_cover.jpg' WHERE accommodation_id = UNHEX(REPLACE('a1000001-0001-4001-8003-000000000014','-',''));
