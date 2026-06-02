
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password TEXT NOT NULL,
    first_name VARCHAR(80),
    last_name VARCHAR(80),
    address VARCHAR(200),
    postal_code VARCHAR(10),
    city VARCHAR(80),
    email VARCHAR(100) UNIQUE,
    phonenumber VARCHAR(20),
    role VARCHAR(20)
);

CREATE TABLE materials (
    material_id SERIAL PRIMARY KEY,
    material_type VARCHAR(50),
    material_name VARCHAR(100) NOT NULL,
    material_price NUMERIC(10, 2) NOT NULL,
    material_length NUMERIC(10, 2),
    material_amount INTEGER
);

CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    user_id INTEGER,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    roof_type VARCHAR(50),
    width_cm INTEGER NOT NULL,
    length_cm INTEGER NOT NULL,
    with_shed BOOLEAN NOT NULL DEFAULT FALSE,
    shed_width_cm INTEGER NOT NULL DEFAULT 0,
    shed_length_cm INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    total_price NUMERIC(10, 2),
    admin_note TEXT,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    sketch_released BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE order_contacts (
    contact_id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL UNIQUE,
    first_name VARCHAR(80),
    last_name VARCHAR(80),
    address VARCHAR(200),
    postal_code VARCHAR(10),
    city VARCHAR(80),
    email VARCHAR(120),
    phone VARCHAR(40),
    message TEXT,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

CREATE TABLE order_quantity (
    quantity_id SERIAL PRIMARY KEY,
    material_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE CASCADE
);

CREATE TABLE order_lines (
    orderline_id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL,
    quantity_id INTEGER NOT NULL,
    order_price NUMERIC(10, 2),
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (quantity_id) REFERENCES order_quantity(quantity_id) ON DELETE CASCADE
);

INSERT INTO users (username, password, email, role) VALUES
  ('admin', '$2a$10$hp6LegEr8lG7LaCb88lc..hH9FbtIkvPvleJ9M73QHkM0Bb3sVUWW', 'admin@fog.dk', 'admin');

INSERT INTO materials (material_type, material_name, material_price, material_length, material_amount) VALUES
  ('spaer',      '45x195 mm spærtræ ubh.',           189.00, 600, NULL),
  ('stolpe',     '97x97 mm trykimp. stolpe',         179.00, 300, NULL),
  ('stern',      '25x200 mm trykimp. brædt',          95.00, 540, NULL),
  ('stern',      '25x125 mm trykimp. brædt',          69.00, 540, NULL),
  ('vandbraet',  '19x100 mm trykimp. brædt',          39.00, 540, NULL),
  ('loesholt',   '45x95 mm reglar ub.',               49.00, 270, NULL),
  ('tagplade',   'Plastmo Ecolite blåtonet',         249.00, 600, NULL),
  ('beslag',     'Universal 190 mm højre',            18.00, NULL, NULL),
  ('beslag',     'Universal 190 mm venstre',          18.00, NULL, NULL),
  ('beslag',     'Bræddebolt 10x120 mm',              12.00, NULL, NULL),
  ('beslag',     'Firkantskiver 40x40x11 mm',          4.00, NULL, NULL),
  ('beslag',     'Hulbånd 1x20 mm (10 m)',            39.00, NULL, NULL),
  ('beslag',     'Vinkelbeslag 35',                    6.00, NULL, NULL),
  ('skrue',      'Plastmo bundskruer (200 stk)',     159.00, NULL, 200),
  ('skrue',      '4,0x50 mm beslagskruer (250 stk)',  99.00, NULL, 250),
  ('skrue',      '4,5x60 mm skruer (200 stk)',        89.00, NULL, 200),
  ('skrue',      '4,5x70 mm skruer (400 stk)',       129.00, NULL, 400),
  ('skrue',      '4,5x50 mm skruer (300 stk)',       109.00, NULL, 300);

--Jeg har glemt at tilføje first_name, last_name, address, postal_code, og city når jeg gik i gang med login validering.
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name  VARCHAR(80);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name   VARCHAR(80);
ALTER TABLE users ADD COLUMN IF NOT EXISTS address     VARCHAR(200);
ALTER TABLE users ADD COLUMN IF NOT EXISTS postal_code VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS city        VARCHAR(80);

