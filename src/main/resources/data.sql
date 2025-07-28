-- Inserimento delle proprietà del Monopoly italiano
-- Questo file va salvato in src/main/resources/data.sql

-- Strade gruppo MARRONE
INSERT INTO properties (name, price, rent, color_group, type) VALUES
                                                                  ('Vicolo Corto', 60, 2, 'BROWN', 'STREET'),
                                                                  ('Vicolo Stretto', 60, 4, 'BROWN', 'STREET');

-- Strade gruppo AZZURRO CHIARO
INSERT INTO properties (name, price, rent, color_group, type) VALUES
                                                                  ('Bastioni Gran Sasso', 100, 6, 'LIGHT_BLUE', 'STREET'),
                                                                  ('Viale Monterosa', 100, 6, 'LIGHT_BLUE', 'STREET'),
                                                                  ('Viale Vesuvio', 120, 8, 'LIGHT_BLUE', 'STREET');

-- Strade gruppo ROSA
INSERT INTO properties (name, price, rent, color_group, type) VALUES
                                                                  ('Via Accademia', 140, 10, 'PINK', 'STREET'),
                                                                  ('Corso Ateneo', 140, 10, 'PINK', 'STREET'),
                                                                  ('Piazza Università', 160, 12, 'PINK', 'STREET');

-- Strade gruppo ARANCIONE
INSERT INTO properties (name, price, rent, color_group, type) VALUES
                                                                  ('Via Verdi', 180, 14, 'ORANGE', 'STREET'),
                                                                  ('Corso Raffaello', 180, 14, 'ORANGE', 'STREET'),
                                                                  ('Piazza Dante', 200, 16, 'ORANGE', 'STREET');

-- Strade gruppo ROSSO
INSERT INTO properties (name, price, rent, color_group, type) VALUES
                                                                  ('Via Marco Polo', 220, 18, 'RED', 'STREET'),
                                                                  ('Corso Magellano', 220, 18, 'RED', 'STREET'),
                                                                  ('Largo Colombo', 240, 20, 'RED', 'STREET');

-- Strade gruppo GIALLO
INSERT INTO properties (name, price, rent, color_group, type) VALUES
                                                                  ('Viale Costantino', 260, 22, 'YELLOW', 'STREET'),
                                                                  ('Viale Traiano', 260, 22, 'YELLOW', 'STREET'),
                                                                  ('Piazza Giulio Cesare', 280, 24, 'YELLOW', 'STREET');

-- Strade gruppo VERDE
INSERT INTO properties (name, price, rent, color_group, type) VALUES
                                                                  ('Via Roma', 300, 26, 'GREEN', 'STREET'),
                                                                  ('Corso Impero', 300, 26, 'GREEN', 'STREET'),
                                                                  ('Largo Augusto', 320, 28, 'GREEN', 'STREET');

-- Strade gruppo BLU SCURO
INSERT INTO properties (name, price, rent, color_group, type) VALUES
                                                                  ('Viale dei Giardini', 350, 35, 'DARK_BLUE', 'STREET'),
                                                                  ('Parco della Vittoria', 400, 50, 'DARK_BLUE', 'STREET');

-- STAZIONI FERROVIARIE
INSERT INTO properties (name, price, rent, color_group, type) VALUES
                                                                  ('Stazione Nord', 200, 25, 'BLACK', 'RAILROAD'),
                                                                  ('Stazione Est', 200, 25, 'BLACK', 'RAILROAD'),
                                                                  ('Stazione Sud', 200, 25, 'BLACK', 'RAILROAD'),
                                                                  ('Stazione Ovest', 200, 25, 'BLACK', 'RAILROAD');

-- SOCIETÀ
INSERT INTO properties (name, price, rent, color_group, type) VALUES
                                                                  ('Società Elettrica', 150, 0, 'WHITE', 'UTILITY'),
                                                                  ('Società Acqua Potabile', 150, 0, 'WHITE', 'UTILITY');

