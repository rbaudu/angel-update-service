-- Script de migration initial pour Angel Update Service
-- Version: 1.0
-- Date: 2025-08-20

-- Table pour les pays
CREATE TABLE IF NOT EXISTS countries (
    code VARCHAR(2) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    native_name VARCHAR(255),
    language_code VARCHAR(10) NOT NULL,
    timezone VARCHAR(50),
    continent VARCHAR(50),
    active BOOLEAN DEFAULT TRUE,
    metadata JSONB DEFAULT '{}'
);

-- Table pour les langues officielles des pays
CREATE TABLE IF NOT EXISTS country_languages (
    country_code VARCHAR(2) NOT NULL,
    official_languages VARCHAR(10),
    PRIMARY KEY (country_code, official_languages),
    FOREIGN KEY (country_code) REFERENCES countries(code) ON DELETE CASCADE
);

-- Table pour les régions
CREATE TABLE IF NOT EXISTS regions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    region_code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    native_name VARCHAR(255),
    metadata JSONB,
    country_id VARCHAR(2),
    timezone VARCHAR(50),
    population INTEGER,
    active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (country_id) REFERENCES countries(code) ON DELETE SET NULL
);

-- Table pour les villes principales des régions
CREATE TABLE IF NOT EXISTS region_cities (
    region_id BIGINT NOT NULL,
    major_cities VARCHAR(255),
    PRIMARY KEY (region_id, major_cities),
    FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE CASCADE
);

-- Table pour stocker les différents types de contenu
CREATE TABLE IF NOT EXISTS contents (
    id BIGSERIAL PRIMARY KEY,
    content_type VARCHAR(50) NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    region_code VARCHAR(50),
    file_path VARCHAR(500) NOT NULL,
    content TEXT,
    version VARCHAR(50),
    priority VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    region_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP,
    published_at TIMESTAMP,
    expires_at TIMESTAMP,
    file_size BIGINT,
    checksum VARCHAR(64),
    FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE SET NULL
);

-- Table pour les tags de contenu
CREATE TABLE IF NOT EXISTS content_tags (
    content_id BIGINT NOT NULL,
    tags VARCHAR(255),
    FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE
);

-- Index pour la table contents
CREATE INDEX IF NOT EXISTS idx_contents_type ON contents(content_type);
CREATE INDEX IF NOT EXISTS idx_contents_country_region ON contents(country_code, region_code);
CREATE INDEX IF NOT EXISTS idx_contents_version ON contents(version);
CREATE INDEX IF NOT EXISTS idx_contents_status ON contents(status);

-- Table pour le statut des collecteurs
CREATE TABLE IF NOT EXISTS collector_status (
    id BIGSERIAL PRIMARY KEY,
    collector_name VARCHAR(100) NOT NULL UNIQUE,
    last_run TIMESTAMP,
    last_success TIMESTAMP,
    last_error TEXT,
    items_collected INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'IDLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table pour l'historique des mises à jour
CREATE TABLE IF NOT EXISTS update_history (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    from_version VARCHAR(50),
    to_version VARCHAR(50) NOT NULL,
    country VARCHAR(10),
    region VARCHAR(50),
    update_size BIGINT,
    download_time BIGINT,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index pour la table update_history
CREATE INDEX IF NOT EXISTS idx_update_history_client ON update_history(client_id);
CREATE INDEX IF NOT EXISTS idx_update_history_date ON update_history(created_at);

-- Table pour les packages de mise à jour générés
CREATE TABLE IF NOT EXISTS update_package (
    id BIGSERIAL PRIMARY KEY,
    package_id VARCHAR(255) NOT NULL UNIQUE,
    from_version VARCHAR(50),
    to_version VARCHAR(50) NOT NULL,
    country VARCHAR(10),
    region VARCHAR(50),
    package_data BYTEA,
    package_size BIGINT,
    checksum VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Index pour la table update_package
CREATE INDEX IF NOT EXISTS idx_package_version ON update_package(from_version, to_version);
CREATE INDEX IF NOT EXISTS idx_package_location ON update_package(country, region);

-- Table pour les sessions WebSocket d'administration
CREATE TABLE IF NOT EXISTS admin_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100),
    ip_address VARCHAR(45),
    connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMP
);

-- Fonction pour mettre à jour automatiquement le champ updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers pour mettre à jour automatiquement updated_at
CREATE TRIGGER update_contents_updated_at BEFORE UPDATE ON contents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_collector_status_updated_at BEFORE UPDATE ON collector_status
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Index pour les performances sur metadata des pays
CREATE INDEX IF NOT EXISTS idx_countries_metadata_gin ON countries USING gin(metadata);

-- Données d'exemple pour les pays avec metadata
INSERT INTO countries (code, name, language_code, continent, metadata) VALUES 
    ('FR', 'France', 'fr', 'Europe', '{"population": 67000000, "currency": "EUR", "capital": "Paris", "phoneCode": "+33"}'),
    ('US', 'United States', 'en', 'North America', '{"population": 331000000, "currency": "USD", "capital": "Washington D.C.", "phoneCode": "+1"}'),
    ('GB', 'United Kingdom', 'en', 'Europe', '{"population": 67000000, "currency": "GBP", "capital": "London", "phoneCode": "+44"}'),
    ('DE', 'Germany', 'de', 'Europe', '{"population": 83000000, "currency": "EUR", "capital": "Berlin", "phoneCode": "+49"}'),
    ('IT', 'Italy', 'it', 'Europe', '{"population": 60000000, "currency": "EUR", "capital": "Rome", "phoneCode": "+39"}'),
    ('ES', 'Spain', 'es', 'Europe', '{"population": 47000000, "currency": "EUR", "capital": "Madrid", "phoneCode": "+34"}')
ON CONFLICT (code) DO NOTHING;

-- Données d'exemple pour les régions avec metadata
INSERT INTO regions (code, language_code, country_code, region_code, name, metadata, country_id, active) VALUES 
    ('FR-IDF', 'fr', 'FR', 'IDF', 'Île-de-France', '{"population": 12000000, "area": 12012}', 'FR', true),
    ('FR-PACA', 'fr', 'FR', 'PACA', 'Provence-Alpes-Côte d''Azur', '{"population": 5000000, "area": 31400}', 'FR', true),
    ('US-CA', 'en', 'US', 'CA', 'California', '{"population": 40000000, "area": 423970}', 'US', true),
    ('US-NY', 'en', 'US', 'NY', 'New York', '{"population": 19000000, "area": 141300}', 'US', true),
    ('GB-LON', 'en', 'GB', 'LON', 'London', '{"population": 9000000, "area": 1572}', 'GB', true),
    ('DE-BY', 'de', 'DE', 'BY', 'Bavaria', '{"population": 13000000, "area": 70550}', 'DE', true)
ON CONFLICT (code) DO NOTHING;

-- Données d'exemple pour les langues officielles
INSERT INTO country_languages (country_code, official_languages) VALUES
    ('FR', 'fr'), ('US', 'en'), ('GB', 'en'), ('DE', 'de'), ('IT', 'it'), ('ES', 'es')
ON CONFLICT (country_code, official_languages) DO NOTHING;

-- Données d'exemple pour les villes principales
INSERT INTO region_cities (region_id, major_cities) VALUES
    ((SELECT id FROM regions WHERE code = 'FR-IDF'), 'Paris'),
    ((SELECT id FROM regions WHERE code = 'FR-IDF'), 'Boulogne-Billancourt'),
    ((SELECT id FROM regions WHERE code = 'FR-PACA'), 'Marseille'),
    ((SELECT id FROM regions WHERE code = 'FR-PACA'), 'Nice'),
    ((SELECT id FROM regions WHERE code = 'US-CA'), 'Los Angeles'),
    ((SELECT id FROM regions WHERE code = 'US-CA'), 'San Francisco'),
    ((SELECT id FROM regions WHERE code = 'US-NY'), 'New York City'),
    ((SELECT id FROM regions WHERE code = 'GB-LON'), 'London'),
    ((SELECT id FROM regions WHERE code = 'DE-BY'), 'Munich')
ON CONFLICT (region_id, major_cities) DO NOTHING;

-- Données d'exemple pour les statuts des collecteurs
INSERT INTO collector_status (collector_name, status, items_collected) VALUES 
    ('NewsCollector', 'IDLE', 0),
    ('WeatherCollector', 'IDLE', 0)
ON CONFLICT (collector_name) DO NOTHING;