-- Inserimento delle proprietà del Monopoly
-- Posizioni 1-40 del tabellone tradizionale

INSERT INTO properties (id, name, price, rent, color_group, type) VALUES
-- Posizione 1: Via!
(1, 'VIA!', 0, 0, 'BROWN', 'SPECIAL'),

-- Posizione 2-3: Gruppo Marrone
(2, 'Vicolo Corto', 60, 2, 'BROWN', 'STREET'),
(3, 'Probabilità', 0, 0, 'BROWN', 'SPECIAL'),
(4, 'Vicolo Stretto', 60, 4, 'BROWN', 'STREET'),

-- Posizione 5: Tassa Patrimoniale
(5, 'Tassa Patrimoniale', 0, 200, 'BROWN', 'SPECIAL'),

-- Posizione 6: Stazione Sud
(6, 'Stazione Sud', 200, 25, 'LIGHT_BLUE', 'RAILROAD'),

-- Posizione 7-9: Gruppo Azzurro
(7, 'Bastioni Gran Sasso', 100, 6, 'LIGHT_BLUE', 'STREET'),
(8, 'Imprevisti', 0, 0, 'LIGHT_BLUE', 'SPECIAL'),
(9, 'Viale Monterosa', 100, 6, 'LIGHT_BLUE', 'STREET'),
(10, 'Viale Vesuvio', 120, 8, 'LIGHT_BLUE', 'STREET'),

-- Posizione 11: Prigione
(11, 'Prigione/In Visita', 0, 0, 'PINK', 'SPECIAL'),

-- Posizione 12-14: Gruppo Rosa
(12, 'Via Accademia', 140, 10, 'PINK', 'STREET'),
(13, 'Società Elettrica', 150, 4, 'PINK', 'UTILITY'),
(14, 'Corso Ateneo', 140, 10, 'PINK', 'STREET'),
(15, 'Piazza Università', 160, 12, 'PINK', 'STREET'),

-- Posizione 16: Stazione Ovest
(16, 'Stazione Ovest', 200, 25, 'ORANGE', 'RAILROAD'),

-- Posizione 17-19: Gruppo Arancione
(17, 'Via Verdi', 180, 14, 'ORANGE', 'STREET'),
(18, 'Probabilità', 0, 0, 'ORANGE', 'SPECIAL'),
(19, 'Corso Garibaldi', 180, 14, 'ORANGE', 'STREET'),
(20, 'Piazza Cavour', 200, 16, 'ORANGE', 'STREET'),

-- Posizione 21: Posteggio Gratuito
(21, 'Posteggio Gratuito', 0, 0, 'RED', 'SPECIAL'),

-- Posizione 22-24: Gruppo Rosso
(22, 'Via Marco Polo', 220, 18, 'RED', 'STREET'),
(23, 'Imprevisti', 0, 0, 'RED', 'SPECIAL'),
(24, 'Corso Magellano', 220, 18, 'RED', 'STREET'),
(25, 'Largo Colombo', 240, 20, 'RED', 'STREET'),

-- Posizione 26: Stazione Nord
(26, 'Stazione Nord', 200, 25, 'YELLOW', 'RAILROAD'),

-- Posizione 27-29: Gruppo Giallo
(27, 'Via Condotti', 260, 22, 'YELLOW', 'STREET'),
(28, 'Via Veneto', 260, 22, 'YELLOW', 'STREET'),
(29, 'Società Acqua Potabile', 150, 4, 'YELLOW', 'UTILITY'),
(30, 'Piazza San Marco', 280, 24, 'YELLOW', 'STREET'),

-- Posizione 31: Vai in Prigione
(31, 'Vai in Prigione', 0, 0, 'GREEN', 'SPECIAL'),

-- Posizione 32-34: Gruppo Verde
(32, 'Via del Corso', 300, 26, 'GREEN', 'STREET'),
(33, 'Largo Augusto', 300, 26, 'GREEN', 'STREET'),
(34, 'Probabilità', 0, 0, 'GREEN', 'SPECIAL'),
(35, 'Via Roma', 320, 28, 'GREEN', 'STREET'),

-- Posizione 36: Stazione Est
(36, 'Stazione Est', 200, 25, 'DARK_BLUE', 'RAILROAD'),

-- Posizione 37: Imprevisti
(37, 'Imprevisti', 0, 0, 'DARK_BLUE', 'SPECIAL'),

-- Posizione 38-40: Gruppo Blu Scuro
(38, 'Viale dei Giardini', 350, 35, 'DARK_BLUE', 'STREET'),
(39, 'Tassa di Lusso', 0, 100, 'DARK_BLUE', 'SPECIAL'),
(40, 'Parco della Vittoria', 400, 50, 'DARK_BLUE', 'STREET');