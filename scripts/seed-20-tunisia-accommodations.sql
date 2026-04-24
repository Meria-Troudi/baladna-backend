-- 20 sample accommodations (Tunisia tourism areas) for hosts 7, 4, 10.
-- All ACTIVE with latitude/longitude so they appear on the tourist map.
-- Run against your `baladna` schema (adjust DB name if needed).
--
-- MySQL: UUID stored as BINARY(16) via UNHEX(REPLACE(...)).
-- Run once; to re-run, delete these rows first (see bottom).

SET NAMES utf8mb4;

START TRANSACTION;

-- Host 7 (7 listings): north / Sahel / Cap Bon / Mahdia coast
INSERT INTO accommodations (accommodation_id, title, description, address, latitude, longitude, max_guests, amenities, rules, type, status, host_id, cover_image_file_name, created_at, updated_at) VALUES
(UNHEX(REPLACE('a1000001-0001-4001-8001-000000000001','-','')), 'Dar El Médina — Tunis', 'Maison d''hôtes typique dans la médina de Tunis, proche souks et Zitouna.', 'Rue de la Kasbah, Médina de Tunis', 36.8065, 10.1815, 4, 'WiFi, climatisation, petit-déjeuner tunisien', 'Pas de fêtes après 23h', 'GUEST_HOUSE', 'ACTIVE', 7, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8001-000000000002','-','')), 'Bleu Sidi Bou — maison aux volets bleus', 'Vue mer et ruelles de Sidi Bou Saïd, idéal photos et promenades.', 'Sidi Bou Saïd, Carthage', 36.8689, 10.3419, 2, 'WiFi, terrasse, thé à la menthe', 'Respecter le calme du quartier', 'GUEST_HOUSE', 'ACTIVE', 7, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8001-000000000003','-','')), 'Appartement Carthage Byrsa', 'Calme résidentiel, accès rapide aux sites puniques et musées.', 'Carthage Byrsa, Tunis', 36.8531, 10.3233, 4, 'WiFi, cuisine équipée, parking', 'Non fumeur à l''intérieur', 'APARTMENT', 'ACTIVE', 7, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8001-000000000004','-','')), 'Mahdia Corniche Breeze', 'Face à la mer, entre médina mahdoise et plage.', 'Corniche, Mahdia', 35.5050, 11.0622, 5, 'WiFi, climatisation, accès plage', 'Animaux sur demande', 'APARTMENT', 'ACTIVE', 7, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8001-000000000005','-','')), 'Dar Ribat — Monastir', 'Proche du ribat et du mausolée Bourguiba, centre historique.', 'Monastir médina', 35.7780, 10.8262, 6, 'WiFi, petit-déjeuner, cour intérieure', 'Arrivée après 15h', 'GUEST_HOUSE', 'ACTIVE', 7, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8001-000000000006','-','')), 'Maison Oliviers — El Jem', 'Séjour autour de l''amphithéâtre romain (UNESCO) et vignobles locaux.', 'El Jem, Mahdia', 35.2964, 10.7069, 4, 'WiFi, climatisation, jardin', 'Pas de musique forte', 'GUEST_HOUSE', 'ACTIVE', 7, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8001-000000000007','-','')), 'Dar Céramique — Nabeul', 'Cap Bon, artisanat céramique et plages à quelques minutes.', 'Nabeul centre', 36.4511, 10.7356, 5, 'WiFi, terrasse, atelier poterie (sur demande)', 'Check-out 11h', 'GUEST_HOUSE', 'ACTIVE', 7, NULL, NOW(), NOW());

-- Host 4 (7 listings): Hammamet, nord-ouest, sud profond
INSERT INTO accommodations (accommodation_id, title, description, address, latitude, longitude, max_guests, amenities, rules, type, status, host_id, cover_image_file_name, created_at, updated_at) VALUES
(UNHEX(REPLACE('a1000001-0001-4001-8002-000000000008','-','')), 'Villa Jasmins — Hammamet Sud', 'Jardin, piscine partagée, proche médina et plages de sable fin.', 'Hammamet Sud', 36.4000, 10.6167, 8, 'WiFi, piscine, parking, cuisine', 'Groupes calmes', 'GUEST_HOUSE', 'ACTIVE', 4, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8002-000000000009','-','')), 'Cap Bon Suite — Kélibia', 'Vue sur mer et fort génois, poisson frais du port.', 'Kélibia', 36.8476, 11.0964, 3, 'WiFi, climatisation, petit-déjeuner', 'Pas de fête', 'APARTMENT', 'ACTIVE', 4, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000a','-','')), 'Corniche Bizerte — loft', 'Canal et vieux port, départs vers îles et forts espagnols.', 'Bizerte corniche', 37.2744, 9.8739, 4, 'WiFi, cuisine, balcon', 'Non fumeur', 'APARTMENT', 'ACTIVE', 4, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000b','-','')), 'Tabarka Perle — forêt et mer', 'Entre aiguilles de Tabarka et plages de sable.', 'Tabarka centre', 36.7601, 8.7572, 4, 'WiFi, climatisation', 'Respecter la nature', 'GUEST_HOUSE', 'ACTIVE', 4, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000c','-','')), 'Palmeraie Tozeur — dar oasis', 'Chalets style sud, chameaux et chott el-Jerid à proximité.', 'Tozeur palmeraie', 33.9197, 8.1336, 6, 'WiFi, climatisation, piscine (selon saison)', 'Économiser l''eau', 'GUEST_HOUSE', 'ACTIVE', 4, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000d','-','')), 'Camp Douz — porte du Sahara', 'Tentes berbères et nuits étoilées, excursions dunes.', 'Douz', 33.4623, 9.0292, 4, 'Repas traditionnels, excursions (option)', 'Respecter consignes désert', 'CAMPING', 'ACTIVE', 4, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000e','-','')), 'Maison troglodyte — Matmata', 'Hébergement troglodyte authentique, Star Wars / culture amazigh.', 'Matmata', 33.5415, 9.6666, 5, 'Repas maison, visites guidées (option)', 'Escaliers raides', 'FARM', 'ACTIVE', 4, NULL, NOW(), NOW());

-- Host 10 (6 listings): Djerba, Tataouine, Sahel, La Marsa
INSERT INTO accommodations (accommodation_id, title, description, address, latitude, longitude, max_guests, amenities, rules, type, status, host_id, cover_image_file_name, created_at, updated_at) VALUES
(UNHEX(REPLACE('a1000001-0001-4001-8003-00000000000f','-','')), 'Riad Houmt Souk — Djerba', 'Souk, synagogue Ghriba et plages de l''île à portée de main.', 'Houmt Souk, Djerba', 33.8720, 10.8577, 6, 'WiFi, cour, petit-déjeuner', 'Caution sur demande', 'GUEST_HOUSE', 'ACTIVE', 10, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8003-000000000010','-','')), 'Appart Midoun — plage Sega', 'Proche zones hôtelières et lagunes, idéal familles.', 'Midoun, Djerba', 33.8072, 10.9929, 5, 'WiFi, climatisation, cuisine', 'Pas de fêtes', 'APARTMENT', 'ACTIVE', 10, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8003-000000000011','-','')), 'Gîte Chenini — villages berbères', 'Vue panoramique sur le ksar, randonnées dans le massif.', 'Chenini, Tataouine', 32.9189, 10.2617, 4, 'Repas berbère, guide (option)', 'Accès 4x4 conseillé', 'GUEST_HOUSE', 'ACTIVE', 10, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8003-000000000012','-','')), 'Glamping Ksar Ghilane', 'Oasis et dunes, bains de sable chaud, nuits sous tente lodge.', 'Ksar Ghilane', 33.5047, 9.5381, 4, 'Dîner inclus, excursions dunes', 'Respecter l''oasis', 'CAMPING', 'ACTIVE', 10, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8003-000000000013','-','')), 'Dar Sousse — médina et port', 'Entre ribat, médina et corniche, vie nocturne modérée.', 'Sousse médina', 35.8256, 10.6411, 5, 'WiFi, terrasse', 'Calme après minuit', 'GUEST_HOUSE', 'ACTIVE', 10, NULL, NOW(), NOW()),
(UNHEX(REPLACE('a1000001-0001-4001-8003-000000000014','-','')), 'Loft La Marsa — mer et cafés', 'Quartier branché, plages et galeries, tram vers Tunis.', 'La Marsa', 36.8897, 10.3256, 3, 'WiFi, cuisine, balcon', 'Non fumeur', 'APARTMENT', 'ACTIVE', 10, NULL, NOW(), NOW());

-- One room per accommodation (prices in TND, realistic ranges)
INSERT INTO rooms (room_id, accommodation_id, type, capacity, price_per_night, amenities, created_at, updated_at) VALUES
(UNHEX(REPLACE('b2000001-0001-4001-9001-000000000001','-','')), UNHEX(REPLACE('a1000001-0001-4001-8001-000000000001','-','')), 'DOUBLE', 2, 85.00, 'WiFi, climatisation', NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9001-000000000002','-','')), UNHEX(REPLACE('a1000001-0001-4001-8001-000000000002','-','')), 'SUITE', 2, 120.00, 'Vue mer', NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9001-000000000003','-','')), UNHEX(REPLACE('a1000001-0001-4001-8001-000000000003','-','')), 'FAMILY', 4, 95.00, 'Cuisine équipée', NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9001-000000000004','-','')), UNHEX(REPLACE('a1000001-0001-4001-8001-000000000004','-','')), 'DOUBLE', 2, 75.00, 'Vue mer', NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9001-000000000005','-','')), UNHEX(REPLACE('a1000001-0001-4001-8001-000000000005','-','')), 'STANDARD', 2, 65.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9001-000000000006','-','')), UNHEX(REPLACE('a1000001-0001-4001-8001-000000000006','-','')), 'DOUBLE', 2, 70.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9001-000000000007','-','')), UNHEX(REPLACE('a1000001-0001-4001-8001-000000000007','-','')), 'STANDARD', 3, 68.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9002-000000000008','-','')), UNHEX(REPLACE('a1000001-0001-4001-8002-000000000008','-','')), 'FAMILY', 6, 180.00, 'Accès jardin', NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9002-000000000009','-','')), UNHEX(REPLACE('a1000001-0001-4001-8002-000000000009','-','')), 'DOUBLE', 2, 88.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9002-00000000000a','-','')), UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000a','-','')), 'STANDARD', 2, 72.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9002-00000000000b','-','')), UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000b','-','')), 'DOUBLE', 2, 79.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9002-00000000000c','-','')), UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000c','-','')), 'SUITE', 4, 110.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9002-00000000000d','-','')), UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000d','-','')), 'OTHER', 4, 95.00, 'Tente équipée', NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9002-00000000000e','-','')), UNHEX(REPLACE('a1000001-0001-4001-8002-00000000000e','-','')), 'FAMILY', 5, 90.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9003-00000000000f','-','')), UNHEX(REPLACE('a1000001-0001-4001-8003-00000000000f','-','')), 'DOUBLE', 2, 92.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9003-000000000010','-','')), UNHEX(REPLACE('a1000001-0001-4001-8003-000000000010','-','')), 'FAMILY', 4, 85.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9003-000000000011','-','')), UNHEX(REPLACE('a1000001-0001-4001-8003-000000000011','-','')), 'STANDARD', 4, 78.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9003-000000000012','-','')), UNHEX(REPLACE('a1000001-0001-4001-8003-000000000012','-','')), 'OTHER', 4, 130.00, 'Lodge tente', NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9003-000000000013','-','')), UNHEX(REPLACE('a1000001-0001-4001-8003-000000000013','-','')), 'DOUBLE', 2, 62.00, NULL, NOW(), NOW()),
(UNHEX(REPLACE('b2000001-0001-4001-9003-000000000014','-','')), UNHEX(REPLACE('a1000001-0001-4001-8003-000000000014','-','')), 'STANDARD', 2, 88.00, NULL, NOW(), NOW());

COMMIT;

-- To remove this seed later:
-- DELETE r FROM rooms r
-- INNER JOIN accommodations a ON r.accommodation_id = a.accommodation_id
-- WHERE a.accommodation_id IN (
--   UNHEX(REPLACE('a1000001-0001-4001-8001-000000000001','-','')),
--   ... (same 20 UUIDs as above)
-- );
-- DELETE FROM accommodations WHERE accommodation_id IN (...);
