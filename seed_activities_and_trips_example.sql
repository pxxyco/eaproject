-- ====================================================================
-- Script Popolamento Dati: Attività e Viaggi per eaproject_db
-- ====================================================================

-- 1. Attività (ACTIVITY)
INSERT INTO "ACTIVITY" (
    activity_id, created_by, title, description, category,
    latitude, longitude, place_name, city,
    start_date, end_date, duration_minutes,
    price, max_seats, available_seats, status,
    average_rating, notes, created_at, updated_at
) VALUES
(
    1,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Escursione in Quad nel Parco della Sila',
    'Un''emozionante avventura in quad attraverso i sentieri panoramici e i boschi secolari della Sila Grande. Include briefing di sicurezza ed equipaggiamento completo.',
    'EXCURSION',
    39.3512, 16.4428, 'Parco Nazionale della Sila', 'Camigliatello Silano',
    '2026-11-15 09:30:00', '2026-11-15 12:30:00', 180,
    65.0, 12, 8, 'PUBLISHED',
    4.9, 'Richiesta patente B. Casco e sottocasco inclusi. Si raccomanda abbigliamento a strati.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    2,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Tour Guidato Musei Vaticani e Cappella Sistina',
    'Ingresso prioritario salta-fila e visita guidata con storico dell''arte tra le meraviglie dei Musei Vaticani, le Stanze di Raffaello e la volta michelangiolesca.',
    'VISIT',
    41.9065, 12.4536, 'Musei Vaticani', 'Roma',
    '2026-11-20 10:00:00', '2026-11-20 13:30:00', 210,
    55.0, 25, 14, 'PUBLISHED',
    4.8, 'Biglietto d''ingresso e auricolari inclusi. Spalle e ginocchia coperte richieste.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    3,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Snorkeling e Giro in Barca a Tropea e Capo Vaticano',
    'Navigazione lungo la Costa degli Dei con soste snorkeling nelle baie più spettacolari, tra grotte marine, acque turchesi e aperitivo al tramonto a bordo.',
    'EXCURSION',
    38.6775, 15.8978, 'Porto Turistico di Tropea', 'Tropea',
    '2026-11-18 14:00:00', '2026-11-18 18:00:00', 240,
    45.0, 15, 6, 'PUBLISHED',
    4.9, 'Maschera e boccaglio forniti dallo skipper. Aperitivo con vino e prodotti tipici incluso.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    4,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Degustazione Vini e Prodotti Tipici nel Chianti',
    'Visita a una storica tenuta vitivinicola sulle colline toscane con tour delle cantine d''invecchiamento e degustazione guidata di 4 vini Chianti Classico abbinati a salumi e formaggi.',
    'MEAL',
    43.5828, 11.3175, 'Tenuta Chianti Classico', 'Greve in Chianti',
    '2026-11-22 12:30:00', '2026-11-22 15:00:00', 150,
    70.0, 20, 12, 'PUBLISHED',
    4.7, 'Disponibili opzioni vegetariane e senza glutine su richiesta anticipata.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    5,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Trekking Panoramico alle Tre Cime di Lavaredo',
    'Escursione ad anello nel cuore delle Dolomiti patrimonio UNESCO, accompagnati da una Guida Alpina certificata. Viste spettacolari e soste nei rifugi alpini.',
    'EXCURSION',
    46.6187, 12.3028, 'Rifugio Auronzo', 'Auronzo di Cadore',
    '2026-11-25 08:30:00', '2026-11-25 14:30:00', 360,
    50.0, 16, 5, 'PUBLISHED',
    4.9, 'Scarponi da trekking obbligatori. Livello di difficoltà medio. Pranzo al sacco o in rifugio.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    6,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Tour Panoramico in Gondola al Tramonto',
    'Un''esperienza romantica e senza tempo tra i canali più silenziosi di Venezia e il Canal Grande, ammirando palazzi storici accarezzati dalla luce dorata del tramonto.',
    'TRANSPORT',
    45.4371, 12.3326, 'Stazio Gondole Santa Sofia', 'Venezia',
    '2026-11-28 17:00:00', '2026-11-28 18:00:00', 60,
    85.0, 6, 2, 'PUBLISHED',
    4.6, 'Massimo 5 passeggeri per gondola. Si consiglia di arrivare 10 minuti prima.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    7,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Street Food Tour a Spaccanapoli e Centro Antico',
    'Percorso gastronomico tra i vicoli veraci di Napoli: pizza a portafoglio, frittatina di pasta, cuoppo di mare, sfogliatella calda e autentico caffè espresso napoletano.',
    'MEAL',
    40.8518, 14.2681, 'Piazza San Domenico Maggiore', 'Napoli',
    '2026-12-02 11:30:00', '2026-12-02 14:30:00', 180,
    38.0, 18, 9, 'PUBLISHED',
    4.9, 'Tutte le degustazioni e bevande sono incluse nel prezzo.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    8,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Passeggiata Culturale tra Centro Storico e Castello Svevo',
    'Itinerario guidato nella Cosenza vecchia, dal Duomo federiciano lungo Corso Telesio fino al Castello Svevo con vista mozzafiato sulla confluenza dei fiumi Crati e Busento.',
    'VISIT',
    39.2983, 16.2537, 'Castello Normanno-Svevo', 'Cosenza',
    '2026-12-05 15:00:00', '2026-12-05 17:30:00', 150,
    20.0, 25, 18, 'PUBLISHED',
    4.7, 'Biglietto di ingresso al castello incluso. Adatto a tutte le età.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    9,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Capolavori del Museo del Louvre e Giardini Tuileries',
    'Visita guidata mirata ai grandi capolavori del Louvre: la Gioconda, la Venere di Milo e la Nike di Samotracia, con rilassante passeggiata finale alle Tuileries.',
    'VISIT',
    48.8606, 2.3376, 'Musée du Louvre', 'Parigi',
    '2026-12-10 10:00:00', '2026-12-10 13:30:00', 210,
    60.0, 20, 10, 'PUBLISHED',
    4.8, 'Ingresso prioritario incluso. Guida abilitata in lingua italiana.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    10,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Tapas Tour e Spettacolo di Flamenco nel Barrio Gótico',
    'Serata vibrante nel cuore medievale di Barcellona: assaggi di tapas gourmet e sangria in 3 locali storici, seguiti da uno spettacolo intimo di flamenco dal vivo.',
    'MEAL',
    41.3828, 2.1769, 'Barrio Gótico', 'Barcellona',
    '2026-12-15 19:30:00', '2026-12-15 22:30:00', 180,
    55.0, 15, 4, 'PUBLISHED',
    4.9, 'Include 4 tapas, 3 bevande e biglietto d''ingresso con posto riservato per lo spettacolo.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    11,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Soggiorno Relax & Spa in Chalet Alpino',
    'Giornata e pernottamento rigenerante ai piedi del Monte Bianco con percorso benessere, sauna panoramica in legno di cembro, piscina riscaldata all''aperto e cena tipica valdostana.',
    'HOTEL',
    45.7969, 6.9701, 'Chalet Mont Blanc Resort', 'Courmayeur',
    '2026-12-20 14:00:00', '2026-12-21 11:00:00', 1260,
    190.0, 8, 3, 'PUBLISHED',
    4.8, 'Accesso spa illimitato, kit cortesia e colazione inclusi.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    12,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Crociera al Tramonto lungo la Costiera Amalfitana',
    'Esclusiva navigazione costiera con partenza da Amalfi verso Positano e l''arcipelago de Li Galli, con prosecco, frutta fresca e sosta bagno nelle calette incontaminate.',
    'EXCURSION',
    40.6340, 14.6027, 'Molo Pennello', 'Amalfi',
    '2026-12-28 15:30:00', '2026-12-28 18:30:00', 180,
    75.0, 14, 7, 'PUBLISHED',
    5.0, 'Skipper esperto, teli mare e calice di benvenuto inclusi.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- Immagini delle Attività (ACTIVITY_IMAGE)
INSERT INTO "ACTIVITY_IMAGE" (image_id, activity_id, image_url, order_index) VALUES
(1, 1, 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?w=800', 0),
(2, 1, 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800', 1),
(3, 2, 'https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800', 0),
(4, 2, 'https://images.unsplash.com/photo-1531572753322-ad063cecc140?w=800', 1),
(5, 3, 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800', 0),
(6, 3, 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800', 1),
(7, 4, 'https://images.unsplash.com/photo-1506377247377-2a5b3b417ebb?w=800', 0),
(8, 4, 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=800', 1),
(9, 5, 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800', 0),
(10, 5, 'https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?w=800', 1),
(11, 6, 'https://images.unsplash.com/photo-1514890547357-a9ee288728e0?w=800', 0),
(12, 6, 'https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?w=800', 1),
(13, 7, 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800', 0),
(14, 7, 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800', 1),
(15, 8, 'https://images.unsplash.com/photo-1516483638261-f4dbaf036963?w=800', 0),
(16, 8, 'https://images.unsplash.com/photo-1527631746610-bca00a040d60?w=800', 1),
(17, 9, 'https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=800', 0),
(18, 9, 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800', 1),
(19, 10, 'https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800', 0),
(20, 10, 'https://images.unsplash.com/photo-1511527661048-7fe73d85e9a4?w=800', 1),
(21, 11, 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800', 0),
(22, 11, 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800', 1),
(23, 12, 'https://images.unsplash.com/photo-1533105079780-92b9be482077?w=800', 0),
(24, 12, 'https://images.unsplash.com/photo-1516483638261-f4dbaf036963?w=800', 1);


-- 2. Viaggi (TRIP)
INSERT INTO "TRIP" (
    trip_id, created_by, title, description,
    destination_country, destination_city,
    start_date, end_date, total_price,
    max_seats, available_seats, status,
    average_rating, cover_photo_url, created_at, updated_at
) VALUES
(
    1,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Tour della Calabria: Tra Mare e Monti',
    'Un viaggio suggestivo di 5 giorni alla scoperta della Calabria più autentica: dai sentieri e laghi della Sila fino alle scogliere mozzafiato e alle acque cristalline di Tropea e Scilla.',
    'Italia', 'Tropea',
    '2026-11-15', '2026-11-20', 590.00,
    16, 7, 'PUBLISHED',
    4.90, 'https://images.unsplash.com/photo-1594895287313-9a3b68fc3306?w=800',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    2,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Weekend Rinascimentale a Firenze e Colline del Chianti',
    'Quattro giorni tra i capolavori del Rinascimento fiorentino, i vicoli storici, la Galleria degli Uffizi e un''escursione con degustazione tra i vigneti del Chianti.',
    'Italia', 'Firenze',
    '2026-11-22', '2026-11-25', 380.00,
    20, 11, 'PUBLISHED',
    4.85, 'https://images.unsplash.com/photo-1543429776-2782fc8e1acd?w=800',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    3,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Magia d''Islanda: Cascate, Geyser e Aurore Boreali',
    'Spettacolare tour on the road lungo il Golden Circle e la costa meridionale islandese. Cascate imponenti, spiagge di sabbia nera, lagune glaciali e notti a caccia dell''aurora boreale.',
    'Islanda', 'Reykjavík',
    '2026-12-01', '2026-12-07', 1350.00,
    12, 4, 'PUBLISHED',
    4.95, 'https://images.unsplash.com/photo-1504893524553-b855bce32c67?w=800',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    4,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Giappone Classico: Tra Tradizione e Futuro',
    'Itinerario completo di 10 giorni alla scoperta del Sol Levante: l''energia futuristica di Tokyo, la pace spirituale dei templi zen di Kyoto e l''antica capitale Nara con i suoi cervi sacri.',
    'Giappone', 'Tokyo',
    '2026-12-10', '2026-12-20', 2400.00,
    14, 5, 'PUBLISHED',
    5.00, 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    5,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Perle d''Andalusia: Siviglia, Cordova e Granada',
    'Un viaggio sensoriale attraverso l''eredità arabo-andalusa: i cortili profumati di Siviglia, la leggendaria Mezquita di Cordova e la spettacolare fortezza dell''Alhambra di Granada.',
    'Spagna', 'Siviglia',
    '2027-01-10', '2027-01-16', 680.00,
    18, 9, 'PUBLISHED',
    4.80, 'https://images.unsplash.com/photo-1583779457094-0cefcc400b41?w=800',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    6,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Trekking e Relax sulle Dolomiti Bellunesi',
    'Un''esperienza a stretto contatto con la natura tra le maestose pareti dolomitiche. Ciaspolate panoramiche, accoglienti rifugi alpini e rilassanti serate con sauna vista cime innevate.',
    'Italia', 'Cortina d''Ampezzo',
    '2027-01-20', '2027-01-25', 520.00,
    15, 6, 'PUBLISHED',
    4.90, 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    7,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Sicilia Orientale tra Etna, Taormina e Siracusa',
    'Un affascinante itinerario tra vulcani attivi, teatri greco-romani a picco sul mare Jonio e i vicoli barocchi dell''isola di Ortigia, con degustazioni di street food e vini dell''Etna.',
    'Italia', 'Catania',
    '2027-02-05', '2027-02-10', 490.00,
    18, 12, 'PUBLISHED',
    4.75, 'https://images.unsplash.com/photo-1526778548025-fa2f459cd5c1?w=800',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    8,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'ORGANIZER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    'Capodanno a New York: Luci di Manhattan & Brooklyn',
    'Vivi l''inconfondibile atmosfera natalizia e il conto alla rovescia di Capodanno nella Grande Mela: le luci di Times Square, pattinaggio a Central Park e la vista dall''Empire State Building.',
    'Stati Uniti', 'New York',
    '2026-12-28', '2027-01-03', 1850.00,
    10, 2, 'PUBLISHED',
    4.90, 'https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);


-- 3. Tappe dei Viaggi (STAGE)
INSERT INTO "STAGE" (
    stage_id, trip_id, title, description, category,
    location_name, city, country, latitude, longitude,
    day, order_in_day, start_time, end_time,
    photo_url, notes, created_at, updated_at
) VALUES
-- Tappe per Trip 1 (Calabria)
(1, 1, 'Benvenuto a Cosenza e passeggiata storica', 'Ritrovo del gruppo, sistemazione in hotel e tour guidato tra il Duomo e i vicoli di Cosenza Vecchia.', 'VISIT', 'Centro Storico', 'Cosenza', 'Italia', 39.2983, 16.2537, 1, 1, '15:00:00', '18:30:00', 'https://images.unsplash.com/photo-1516483638261-f4dbaf036963?w=800', 'Incontro presso la hall dell''hotel alle 14:30.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 'Trekking tra i Giganti della Sila e Lago Cecita', 'Escursione nella riserva naturale dei pini larici secolari e sosta panoramica sulle rive del Lago Cecita.', 'EXCURSION', 'Riserva Naturale dei Giganti di Fallistro', 'Spezzano della Sila', 'Italia', 39.3142, 16.4839, 2, 1, '09:30:00', '14:00:00', 'https://images.unsplash.com/photo-1448375240586-882707db888b?w=800', 'Scarpe da trekking consigliate.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 'Pranzo Tipico Silano', 'Degustazione di caciocavallo silano DOP, patate della Sila e funghi porcini freschi.', 'MEAL', 'Trattoria Silana', 'Camigliatello Silano', 'Italia', 39.3512, 16.4428, 2, 2, '14:00:00', '16:00:00', 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800', 'Menu fisso con opzioni vegetariane.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, 'Tropea e la Chiesa di Santa Maria dell''Isola', 'Visita al borgo arroccato sulla rupe e discesa alla famosa chiesetta sul promontorio sul mare.', 'VISIT', 'Santuario di Santa Maria dell''Isola', 'Tropea', 'Italia', 38.6792, 15.8973, 3, 1, '10:00:00', '13:00:00', 'https://images.unsplash.com/photo-1594895287313-9a3b68fc3306?w=800', 'Ingresso alla scalinata panoramica.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 1, 'Gita in Barca a Capo Vaticano', 'Navigazione tra calette segrete con sosta bagno nelle acque turchesi della Baia di Riaci.', 'EXCURSION', 'Faro di Capo Vaticano', 'Ricadi', 'Italia', 38.6214, 15.8291, 4, 1, '14:30:00', '18:30:00', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800', 'Attrezzatura snorkeling a bordo.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Tappe per Trip 2 (Firenze)
(6, 2, 'Check-in e Primo Sguardo su Ponte Vecchio', 'Arrivo nel capoluogo toscano, sistemazione nelle camere e suggestiva camminata lungo l''Arno fino a Ponte Vecchio.', 'HOTEL', 'Hotel Brunelleschi', 'Firenze', 'Italia', 43.7696, 11.2558, 1, 1, '14:00:00', '18:00:00', 'https://images.unsplash.com/photo-1543429776-2782fc8e1acd?w=800', 'Tempo libero per acquisti e fotografie.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 2, 'I Tesori della Galleria degli Uffizi', 'Tour guidato tra le sale di Botticelli, Leonardo da Vinci, Michelangelo e Caravaggio.', 'VISIT', 'Piazzale degli Uffizi', 'Firenze', 'Italia', 43.7677, 11.2553, 2, 1, '09:30:00', '13:00:00', 'https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800', 'Ingresso prioritario incluso.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 2, 'Cena Tipica Fiorentina nel quartiere Santo Spirito', 'Autentica bistecca alla fiorentina accompagnata da vino Chianti Classico e cantucci con vin santo.', 'MEAL', 'Osteria di Santo Spirito', 'Firenze', 'Italia', 43.7672, 11.2483, 2, 2, '20:00:00', '22:30:00', 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=800', 'Tavolo riservato per l''intero gruppo.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 2, 'Tramonto panoramico da Piazzale Michelangelo', 'Salita verso la terrazza più famosa di Firenze per godere di una vista indimenticabile su tutta la città.', 'VISIT', 'Piazzale Michelangelo', 'Firenze', 'Italia', 43.7629, 11.2650, 3, 1, '17:30:00', '19:30:00', 'https://images.unsplash.com/photo-1516483638261-f4dbaf036963?w=800', 'Portare la fotocamera.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Tappe per Trip 3 (Islanda)
(10, 3, 'Relax Termale alla Blue Lagoon', 'Arrivo all''aeroporto di Keflavík e trasferimento diretto nelle acque termali geotermiche della Blue Lagoon.', 'OTHER', 'Blue Lagoon Iceland', 'Grindavík', 'Islanda', 63.8804, -22.4495, 1, 1, '15:00:00', '18:30:00', 'https://images.unsplash.com/photo-1504893524553-b855bce32c67?w=800', 'Asciugamano e fango di silice inclusi.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 3, 'Circolo d''Oro: Parco di Thingvellir e Geysir', 'Visita alla faglia tettonica tra placca europea e nordamericana e alle potenti eruzioni del geyser Strokkur.', 'EXCURSION', 'Parco Nazionale Thingvellir', 'Bláskógabyggð', 'Islanda', 64.2559, -21.1295, 2, 1, '09:00:00', '16:00:00', 'https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?w=800', 'Giacca antivento e impermeabile richiesta.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 3, 'Cascate del Sud e Spiaggia Nera di Reynisfjara', 'Ammiriamo le cascate Seljalandsfoss e Skógafoss, seguite dalle maestose colonne basaltiche sulla spiaggia di Reynisfjara.', 'VISIT', 'Spiaggia di Reynisfjara', 'Vík í Mýrdal', 'Islanda', 63.4044, -19.0494, 3, 1, '09:30:00', '17:00:00', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800', 'Attenzione alle onde anomale sulla spiaggia.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 3, 'Laguna Glaciale Jökulsárlón e Caccia all''Aurora', 'Navigazione tra gli iceberg galleggianti e serata dedicata all''osservazione dell''aurora boreale.', 'EXCURSION', 'Laguna di Jökulsárlón', 'Höfn', 'Islanda', 64.0784, -16.2306, 4, 1, '11:00:00', '23:30:00', 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?w=800', 'In caso di cielo coperto la caccia sarà rimandata al giorno successivo.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Tappe per Trip 4 (Giappone)
(14, 4, 'Benvenuto a Tokyo e le Luci di Shinjuku', 'Arrivo a Tokyo Narita, trasferimento in treno e prima serata tra i vicoli di Omoide Yokocho e Kabukicho.', 'VISIT', 'Shinjuku City', 'Tokyo', 'Giappone', 35.6938, 139.7036, 1, 1, '17:00:00', '21:30:00', 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800', 'Distribuzione schede Suica e pocket Wi-Fi.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, 4, 'Treno Proiettile Shinkansen verso Kyoto', 'Esperienza ad alta velocità a bordo del treno Shinkansen con vista sul Monte Fuji.', 'TRANSPORT', 'Stazione di Tokyo', 'Tokyo', 'Giappone', 35.6812, 139.7671, 3, 1, '08:30:00', '11:15:00', 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800', 'Posti prenotati in carrozza silenziosa.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, 4, 'Santuario Fushimi Inari Taisha e i Diecimila Torii', 'Salita spirituale lungo il sentiero montano scandito da migliaia di portali torii laccati di rosso vermiglio.', 'EXCURSION', 'Fushimi Inari Taisha', 'Kyoto', 'Giappone', 34.9671, 135.7727, 4, 1, '08:00:00', '12:30:00', 'https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=800', 'Partenza al mattino presto per evitare la folla.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(17, 4, 'I Cervi Sacri di Nara e il Grande Buddha', 'Gita in giornata all''antica capitale Nara, passeggiata nel parco popolato da cervi in libertà e ingresso al Tempio Todai-ji.', 'VISIT', 'Parco di Nara', 'Nara', 'Giappone', 34.6851, 135.8430, 5, 1, '09:30:00', '16:00:00', 'https://images.unsplash.com/photo-1527631746610-bca00a040d60?w=800', 'Biscotti per i cervi acquistabili in loco.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Tappe per Trip 5 (Andalusia)
(18, 5, 'Plaza de España e Quartiere Santa Cruz a Siviglia', 'Passeggiata nella grandiosa piazza semicircolare ornata di ceramiche azulejos e nei vicoli alberati di aranci.', 'VISIT', 'Plaza de España', 'Siviglia', 'Spagna', 37.3772, -5.9869, 1, 1, '10:00:00', '14:00:00', 'https://images.unsplash.com/photo-1583779457094-0cefcc400b41?w=800', 'Ingresso libero.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(19, 5, 'La Grande Mezquita-Cattedrale di Cordova', 'Visita al celebre bosco di colonne e archi bicolori, capolavoro assoluto dell''arte islamica medievale in Europa.', 'VISIT', 'Mezquita de Córdoba', 'Cordova', 'Spagna', 37.8790, -4.7795, 3, 1, '11:00:00', '15:00:00', 'https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800', 'Guida abilitata locale.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(20, 5, 'La Magica Fortezza dell''Alhambra e Generalife', 'Visita completa ai palazzi Nasridi, alla fortezza dell''Alcazaba e ai giardini fioriti con giochi d''acqua.', 'VISIT', 'Palazzo dell''Alhambra', 'Granada', 'Spagna', 37.1773, -3.5986, 4, 1, '09:00:00', '14:30:00', 'https://images.unsplash.com/photo-1511527661048-7fe73d85e9a4?w=800', 'Documento d''identità obbligatorio per l''accesso.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Tappe per Trip 6 (Dolomiti)
(21, 6, 'Arrivo a Cortina d''Ampezzo e Cena di Benvenuto', 'Accoglienza in hotel a Cortina, presentazione delle escursioni e tipica cena ladina.', 'HOTEL', 'Grand Hotel Savoia', 'Cortina d''Ampezzo', 'Italia', 46.5405, 12.1357, 1, 1, '15:00:00', '21:00:00', 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800', 'Briefing con le guide alpine alle 18:00.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(22, 6, 'Ciaspolata Spettacolare al Lago di Braies', 'Camminata sulla neve lungo il perimetro del lago incastonato tra le pareti della Croda del Becco.', 'EXCURSION', 'Lago di Braies', 'Braies', 'Italia', 46.6946, 12.0854, 2, 1, '09:30:00', '15:00:00', 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800', 'Ciaspole e bastoncini inclusi.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(23, 6, 'Anello delle Tre Cime e Pausa in Rifugio Alpino', 'Escursione panoramica con vista sulle celeberrime pareti nord delle Tre Cime e merenda con strudel caldo.', 'EXCURSION', 'Rifugio Locatelli', 'Auronzo di Cadore', 'Italia', 46.6331, 12.3094, 3, 1, '09:00:00', '16:00:00', 'https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?w=800', 'Livello escursionistico medio.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Tappe per Trip 7 (Sicilia)
(24, 7, 'Il Barocco Catanese e la Pescheria Storica', 'Visita a Piazza Duomo, via Etnea e immersione tra le bancarelle del pittoresco mercato ittico.', 'VISIT', 'Piazza del Duomo', 'Catania', 'Italia', 37.5025, 15.0872, 1, 1, '10:00:00', '13:30:00', 'https://images.unsplash.com/photo-1526778548025-fa2f459cd5c1?w=800', 'Degustazione seltz limone e sale al chiosco.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(25, 7, 'Ascesa in 4x4 e Trekking sui Crateri dell''Etna', 'Avventura sul vulcano attivo più alto d''Europa: salita in fuoristrada fino a quota 2.500m e passeggiata con guida vulcanologica.', 'EXCURSION', 'Rifugio Sapienza', 'Nicolosi', 'Italia', 37.7011, 15.0003, 2, 1, '08:30:00', '15:30:00', 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?w=800', 'Giacca antivento e scarponcini forniti se necessari.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(26, 7, 'Taormina e il Teatro Antico affacciato sul Mare', 'Passeggiata lungo Corso Umberto, visita al Teatro Greco-Romano con vista spettacolare sull''Etna e sul golfo di Naxos.', 'VISIT', 'Teatro Antico di Taormina', 'Taormina', 'Italia', 37.8524, 15.2922, 3, 1, '10:00:00', '14:00:00', 'https://images.unsplash.com/photo-1533105079780-92b9be482077?w=800', 'Biglietto teatro antico incluso.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(27, 7, 'Ortigia e Sapori Tipici Siracusani', 'Camminata tra i vicoli bianchi dell''isola di Ortigia, Fonte Aretusa e pranzo a base di pesce fresco e cannoli artigianali.', 'MEAL', 'Isola di Ortigia', 'Siracusa', 'Italia', 37.0594, 15.2933, 4, 1, '11:00:00', '15:30:00', 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800', 'Pranzo completo di 4 portate incluso.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Tappe per Trip 8 (New York)
(28, 8, 'Times Square e Broadway sotto le Luci di Capodanno', 'Primi passi nel cuore pulsante di Manhattan: grattacieli illuminati, schermi giganti e atmosfera festosa.', 'VISIT', 'Times Square', 'New York', 'Stati Uniti', 40.7580, -73.9855, 1, 1, '16:00:00', '21:00:00', 'https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800', 'Incontro a Duffy Square.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(29, 8, 'Attraversamento a Piedi del Ponte di Brooklyn', 'Camminata indimenticabile lungo la passerella di legno con vista su Manhattan skyline e arrivo a DUMBO.', 'EXCURSION', 'Brooklyn Bridge', 'New York', 'Stati Uniti', 40.7061, -73.9969, 2, 1, '10:00:00', '13:30:00', 'https://images.unsplash.com/photo-1514890547357-a9ee288728e0?w=800', 'Consigliata giacca pesante invernale.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(30, 8, 'Pattinaggio sul Ghiaccio a Central Park', 'Esperienza iconica alla pista Wollman Rink circondati dai maestosi grattacieli di Midtown innevati.', 'EXCURSION', 'Wollman Rink Central Park', 'New York', 'Stati Uniti', 40.7676, -73.9745, 3, 1, '14:00:00', '17:00:00', 'https://images.unsplash.com/photo-1506377247377-2a5b3b417ebb?w=800', 'Noleggio pattini incluso nel pass.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- 4. Immagini dei Viaggi (TRIP_IMAGE)
INSERT INTO "TRIP_IMAGE" (image_id, trip_id, image_url, order_index) VALUES
(1, 1, 'https://images.unsplash.com/photo-1594895287313-9a3b68fc3306?w=800', 0),
(2, 1, 'https://images.unsplash.com/photo-1448375240586-882707db888b?w=800', 1),
(3, 1, 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800', 2),
(4, 2, 'https://images.unsplash.com/photo-1543429776-2782fc8e1acd?w=800', 0),
(5, 2, 'https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800', 1),
(6, 2, 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=800', 2),
(7, 3, 'https://images.unsplash.com/photo-1504893524553-b855bce32c67?w=800', 0),
(8, 3, 'https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?w=800', 1),
(9, 3, 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?w=800', 2),
(10, 4, 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800', 0),
(11, 4, 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800', 1),
(12, 4, 'https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=800', 2),
(13, 5, 'https://images.unsplash.com/photo-1583779457094-0cefcc400b41?w=800', 0),
(14, 5, 'https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800', 1),
(15, 6, 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800', 0),
(16, 6, 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800', 1),
(17, 7, 'https://images.unsplash.com/photo-1526778548025-fa2f459cd5c1?w=800', 0),
(18, 7, 'https://images.unsplash.com/photo-1533105079780-92b9be482077?w=800', 1),
(19, 8, 'https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800', 0),
(20, 8, 'https://images.unsplash.com/photo-1514890547357-a9ee288728e0?w=800', 1);


-- 5. Recensioni (REVIEW)
-- Nota: Una recensione deve avere user_id e esattamente uno tra activity_id e trip_id
INSERT INTO "REVIEW" (
    review_id, user_id, activity_id, trip_id, rating, comment, created_at, deleted_at
) VALUES
-- Recensioni per Attività
(
    1,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'TRAVELER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    1, NULL, 5,
    'Esperienza pazzesca in mezzo ai boschi della Sila! Istruttori bravissimi e quad potenti e sicuri. Consigliatissimo!',
    CURRENT_TIMESTAMP, false
),
(
    2,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'TRAVELER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    2, NULL, 5,
    'La guida era preparatissima e simpatica. Saltare la fila chilometrica non ha prezzo! La Cappella Sistina toglie il fiato.',
    CURRENT_TIMESTAMP, false
),
(
    3,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'TRAVELER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    3, NULL, 5,
    'Acque trasparenti da sogno e aperitivo al tramonto da film. Lo skipper ci ha fatto sentire a casa.',
    CURRENT_TIMESTAMP, false
),
(
    4,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'TRAVELER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    7, NULL, 5,
    'Cibo spettacolare! La pizza a portafoglio e la frittatina erano divine. Guida appassionata dei vicoli di Napoli.',
    CURRENT_TIMESTAMP, false
),
(
    5,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'TRAVELER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    5, NULL, 5,
    'Paesaggi mozzafiato. La guida alpina ci ha spiegato ogni dettaglio geologico. Una delle migliori camminate mai fatte!',
    CURRENT_TIMESTAMP, false
),
-- Recensioni per Viaggi
(
    6,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'TRAVELER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    NULL, 1, 5,
    'Tour della Calabria organizzato nei minimi dettagli! Il contrasto tra la pace della Sila e il mare di Tropea è favoloso.',
    CURRENT_TIMESTAMP, false
),
(
    7,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'TRAVELER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    NULL, 2, 5,
    'Firenze non delude mai. Hotel centrale e confortevole, visite agli Uffizi perfette e cena super.',
    CURRENT_TIMESTAMP, false
),
(
    8,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'TRAVELER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    NULL, 3, 5,
    'Islanda da favola! Abbiamo visto l''aurora boreale per due sere consecutive. Organizzazione impeccabile.',
    CURRENT_TIMESTAMP, false
),
(
    9,
    (SELECT COALESCE((SELECT user_id FROM "USERS" WHERE role = 'TRAVELER' LIMIT 1), (SELECT user_id FROM "USERS" LIMIT 1))),
    NULL, 4, 5,
    'Il viaggio della vita! Il Giappone ti entra nel cuore. Guida sempre presente e tappe imperdibili.',
    CURRENT_TIMESTAMP, false
);


-- 6. Allineamento Sequence PostgreSQL
SELECT setval(pg_get_serial_sequence('"ACTIVITY"', 'activity_id'), (SELECT COALESCE(MAX(activity_id), 1) FROM "ACTIVITY"));
SELECT setval(pg_get_serial_sequence('"ACTIVITY_IMAGE"', 'image_id'), (SELECT COALESCE(MAX(image_id), 1) FROM "ACTIVITY_IMAGE"));
SELECT setval(pg_get_serial_sequence('"TRIP"', 'trip_id'), (SELECT COALESCE(MAX(trip_id), 1) FROM "TRIP"));
SELECT setval(pg_get_serial_sequence('"STAGE"', 'stage_id'), (SELECT COALESCE(MAX(stage_id), 1) FROM "STAGE"));
SELECT setval(pg_get_serial_sequence('"TRIP_IMAGE"', 'image_id'), (SELECT COALESCE(MAX(image_id), 1) FROM "TRIP_IMAGE"));
SELECT setval(pg_get_serial_sequence('"REVIEW"', 'review_id'), (SELECT COALESCE(MAX(review_id), 1) FROM "REVIEW"));
