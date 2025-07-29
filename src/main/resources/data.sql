-- File: src/main/resources/data.sql
-- Proprietà ufficiali del Monopoly Italiano (affitti completi)

-- ------------------------------------------------------------------
-- INSERIMENTI
-- ------------------------------------------------------------------
-- NB: per STAZIONI e SOCIETÀ i valori degli affitti con edifici sono NULL

---------------------------------------------------------------------
-- STRADE - Gruppo MARRONE
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (1,  'Vicolo Corto',   60,  2,  10,  30,  90, 160,  250, 'BROWN', 'STREET'),
    (2,  'Vicolo Stretto', 60,  4,  20,  60, 180, 320,  450, 'BROWN', 'STREET');

---------------------------------------------------------------------
-- STRADE - Gruppo AZZURRO (LIGHT_BLUE)
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (3,  'Bastioni Gran Sasso', 100,  6, 30,  90, 270, 400, 550, 'LIGHT_BLUE', 'STREET'),
    (4,  'Viale Monterosa',     100,  6, 30,  90, 270, 400, 550, 'LIGHT_BLUE', 'STREET'),
    (5,  'Viale Vesuvio',       120,  8, 40, 100, 300, 450, 600, 'LIGHT_BLUE', 'STREET');

---------------------------------------------------------------------
-- STAZIONE
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (6, 'Stazione Sud', 200, 25, NULL, NULL, NULL, NULL, NULL, 'BLACK', 'RAILROAD');

---------------------------------------------------------------------
-- STRADE - Gruppo ROSA (PINK)
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (7,  'Via Accademia',      140, 10,  50, 150, 450,  625,  750, 'PINK', 'STREET'),
    (8,  'Corso Ateneo',       140, 10,  50, 150, 450,  625,  750, 'PINK', 'STREET'),
    (9,  'Piazza Università',  160, 12,  60, 180, 500,  700,  900, 'PINK', 'STREET');

---------------------------------------------------------------------
-- SOCIETÀ
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (10, 'Società Elettrica', 150, 0, NULL, NULL, NULL, NULL, NULL, 'WHITE', 'UTILITY');

---------------------------------------------------------------------
-- STRADE - Gruppo ARANCIONE
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (11, 'Via Verdi',       180, 14,  70, 200, 550, 750,  950, 'ORANGE', 'STREET'),
    (12, 'Corso Raffaello', 180, 14,  70, 200, 550, 750,  950, 'ORANGE', 'STREET'),
    (13, 'Piazza Dante',    200, 16,  80, 220, 600, 800, 1000, 'ORANGE', 'STREET');

---------------------------------------------------------------------
-- STAZIONE
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (14, 'Stazione Ovest', 200, 25, NULL, NULL, NULL, NULL, NULL, 'BLACK', 'RAILROAD');

---------------------------------------------------------------------
-- STRADE - Gruppo ROSSO
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (15, 'Via Marco Polo',  220, 18,  90, 250, 700,  875, 1050, 'RED', 'STREET'),
    (16, 'Corso Magellano', 220, 18,  90, 250, 700,  875, 1050, 'RED', 'STREET'),
    (17, 'Largo Colombo',   240, 20, 100, 300, 750,  925, 1100, 'RED', 'STREET');

---------------------------------------------------------------------
-- STRADE - Gruppo GIALLO
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (18, 'Viale Costantino',     260, 22, 110, 330, 800,  975, 1150, 'YELLOW', 'STREET'),
    (19, 'Viale Traiano',        260, 22, 110, 330, 800,  975, 1150, 'YELLOW', 'STREET'),
    (20, 'Piazza Giulio Cesare', 280, 24, 120, 360, 850, 1025, 1200, 'YELLOW', 'STREET');

---------------------------------------------------------------------
-- STAZIONE
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (21, 'Stazione Nord', 200, 25, NULL, NULL, NULL, NULL, NULL, 'BLACK', 'RAILROAD');

---------------------------------------------------------------------
-- STRADE - Gruppo VERDE
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (22, 'Via Roma',      300, 26, 130, 390, 900, 1100, 1275, 'GREEN', 'STREET'),
    (23, 'Corso Impero',  300, 26, 130, 390, 900, 1100, 1275, 'GREEN', 'STREET'),
    (24, 'Largo Augusto', 320, 28, 150, 450,1000, 1200, 1400, 'GREEN', 'STREET');

---------------------------------------------------------------------
-- SOCIETÀ
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (25, 'Società Acqua Potabile', 150, 0, NULL, NULL, NULL, NULL, NULL, 'WHITE', 'UTILITY');

---------------------------------------------------------------------
-- STRADE - Gruppo BLU SCURO (DARK_BLUE)
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (26, 'Viale dei Giardini',   350, 35, 175, 500, 1100, 1300, 1500, 'DARK_BLUE', 'STREET'),
    (27, 'Parco della Vittoria', 400, 50, 200, 600, 1400, 1700, 2000, 'DARK_BLUE', 'STREET');

---------------------------------------------------------------------
-- STAZIONE
---------------------------------------------------------------------
INSERT INTO properties
(id, name, price,
 rent, rent_house_1, rent_house_2, rent_house_3, rent_house_4, rent_hotel,
 color_group, type)
VALUES
    (28, 'Stazione Est', 200, 25, NULL, NULL, NULL, NULL, NULL, 'BLACK', 'RAILROAD');

-- ------------------------------------------------------------------
-- VERIFICA (opzionale)
-- ------------------------------------------------------------------
SELECT
    color_group,
    COUNT(*)  AS num_properties,
    MIN(price) AS min_price,
    MAX(price) AS max_price
FROM properties
GROUP BY color_group
ORDER BY
    CASE color_group
        WHEN 'BROWN'      THEN 1
        WHEN 'LIGHT_BLUE' THEN 2
        WHEN 'PINK'       THEN 3
        WHEN 'ORANGE'     THEN 4
        WHEN 'RED'        THEN 5
        WHEN 'YELLOW'     THEN 6
        WHEN 'GREEN'      THEN 7
        WHEN 'DARK_BLUE'  THEN 8
        WHEN 'BLACK'      THEN 9
        WHEN 'WHITE'      THEN 10
        END;
