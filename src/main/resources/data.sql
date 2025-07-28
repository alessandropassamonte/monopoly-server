-- File: src/main/resources/data.sql
-- Proprietà ufficiali del Monopoly Italiano

-- Pulisci tabelle esistenti
DELETE FROM property_ownership;
DELETE FROM properties;

-- STRADE - Gruppo MARRONE
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
                                                                      (1, 'Vicolo Corto', 60, 2, 'BROWN', 'STREET'),
                                                                      (2, 'Vicolo Stretto', 60, 4, 'BROWN', 'STREET');

-- STRADE - Gruppo AZZURRO (LIGHT_BLUE)
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
                                                                      (3, 'Bastioni Gran Sasso', 100, 6, 'LIGHT_BLUE', 'STREET'),
                                                                      (4, 'Viale Monterosa', 100, 6, 'LIGHT_BLUE', 'STREET'),
                                                                      (5, 'Viale Vesuvio', 120, 8, 'LIGHT_BLUE', 'STREET');

-- STAZIONI
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
    (6, 'Stazione Sud', 200, 25, 'BLACK', 'RAILROAD');

-- STRADE - Gruppo ROSA (PINK)
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
                                                                      (7, 'Via Accademia', 140, 10, 'PINK', 'STREET'),
                                                                      (8, 'Corso Ateneo', 140, 10, 'PINK', 'STREET'),
                                                                      (9, 'Piazza Università', 160, 12, 'PINK', 'STREET');

-- SOCIETÀ
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
    (10, 'Società Elettrica', 150, 0, 'WHITE', 'UTILITY');

-- STRADE - Gruppo ARANCIONE
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
                                                                      (11, 'Via Verdi', 180, 14, 'ORANGE', 'STREET'),
                                                                      (12, 'Corso Raffaello', 180, 14, 'ORANGE', 'STREET'),
                                                                      (13, 'Piazza Dante', 200, 16, 'ORANGE', 'STREET');

-- STAZIONI
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
    (14, 'Stazione Ovest', 200, 25, 'BLACK', 'RAILROAD');

-- STRADE - Gruppo ROSSO
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
                                                                      (15, 'Via Marco Polo', 220, 18, 'RED', 'STREET'),
                                                                      (16, 'Corso Magellano', 220, 18, 'RED', 'STREET'),
                                                                      (17, 'Largo Colombo', 240, 20, 'RED', 'STREET');

-- STRADE - Gruppo GIALLO
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
                                                                      (18, 'Viale Costantino', 260, 22, 'YELLOW', 'STREET'),
                                                                      (19, 'Viale Traiano', 260, 22, 'YELLOW', 'STREET'),
                                                                      (20, 'Piazza Giulio Cesare', 280, 24, 'YELLOW', 'STREET');

-- STAZIONI
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
    (21, 'Stazione Nord', 200, 25, 'BLACK', 'RAILROAD');

-- STRADE - Gruppo VERDE
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
                                                                      (22, 'Via Roma', 300, 26, 'GREEN', 'STREET'),
                                                                      (23, 'Corso Impero', 300, 26, 'GREEN', 'STREET'),
                                                                      (24, 'Largo Augusto', 320, 28, 'GREEN', 'STREET');

-- SOCIETÀ
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
    (25, 'Società Acqua Potabile', 150, 0, 'WHITE', 'UTILITY');

-- STRADE - Gruppo BLU SCURO (DARK_BLUE)
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
                                                                      (26, 'Viale dei Giardini', 350, 35, 'DARK_BLUE', 'STREET'),
                                                                      (27, 'Parco della Vittoria', 400, 50, 'DARK_BLUE', 'STREET');

-- STAZIONI
INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
    (28, 'Stazione Est', 200, 25, 'BLACK', 'RAILROAD');



-- Verifica inserimento
SELECT
    color_group,
    COUNT(*) as count,
    type
FROM properties
GROUP BY color_group, type
ORDER BY
    CASE color_group
    WHEN 'BROWN' THEN 1
    WHEN 'LIGHT_BLUE' THEN 2
    WHEN 'PINK' THEN 3
    WHEN 'ORANGE' THEN 4
    WHEN 'RED' THEN 5
    WHEN 'YELLOW' THEN 6
    WHEN 'GREEN' THEN 7
    WHEN 'DARK_BLUE' THEN 8
    WHEN 'BLACK' THEN 9
    WHEN 'WHITE' THEN 10
END;