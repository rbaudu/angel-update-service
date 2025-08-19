-- Initialize Angel Update Service Database

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS angel;

-- Set search path
SET search_path TO angel, public;

-- Countries table
CREATE TABLE IF NOT EXISTS countries (
    code VARCHAR(2) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    native_name VARCHAR(100),
    language_code VARCHAR(5) NOT NULL,
    timezone VARCHAR(50),
    currency VARCHAR(3),
    capital VARCHAR(100),
    continent VARCHAR(50),
    population INTEGER,
    active BOOLEAN DEFAULT true,
    phone_code VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Regions table
CREATE TABLE IF NOT EXISTS regions (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    language_code VARCHAR(5) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    region_code VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    native_name VARCHAR(100),
    metadata JSONB,
    country_id VARCHAR(2) REFERENCES countries(code),
    timezone VARCHAR(50),
    population INTEGER,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Contents table
CREATE TABLE IF NOT EXISTS contents (
    id SERIAL PRIMARY KEY,
    content_type VARCHAR(50) NOT NULL,
    language_code VARCHAR(5) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    region_code VARCHAR(10),
    file_path VARCHAR(500) NOT NULL,
    content TEXT,
    version VARCHAR(20),
    priority VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    region_id INTEGER REFERENCES regions(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    expires_at TIMESTAMP,
    file_size BIGINT,
    checksum VARCHAR(64),
    CONSTRAINT uk_content_path UNIQUE (file_path, version)
);

-- Content tags table
CREATE TABLE IF NOT EXISTS content_tags (
    content_id INTEGER REFERENCES contents(id) ON DELETE CASCADE,
    tags VARCHAR(50),
    PRIMARY KEY (content_id, tags)
);

-- Country languages table
CREATE TABLE IF NOT EXISTS country_languages (
    country_code VARCHAR(2) REFERENCES countries(code) ON DELETE CASCADE,
    official_languages VARCHAR(5),
    PRIMARY KEY (country_code, official_languages)
);

-- Region cities table
CREATE TABLE IF NOT EXISTS region_cities (
    region_id INTEGER REFERENCES regions(id) ON DELETE CASCADE,
    major_cities VARCHAR(100),
    PRIMARY KEY (region_id, major_cities)
);

-- Versions table
CREATE TABLE IF NOT EXISTS versions (
    id SERIAL PRIMARY KEY,
    version VARCHAR(20) UNIQUE NOT NULL,
    country_code VARCHAR(2),
    region_code VARCHAR(10),
    release_date TIMESTAMP NOT NULL,
    release_notes TEXT,
    mandatory BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Collectors metadata table
CREATE TABLE IF NOT EXISTS collector_metadata (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    schedule VARCHAR(100),
    last_run TIMESTAMP,
    next_run TIMESTAMP,
    success_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_contents_type ON contents(content_type);
CREATE INDEX IF NOT EXISTS idx_contents_location ON contents(country_code, region_code);
CREATE INDEX IF NOT EXISTS idx_contents_status ON contents(status);
CREATE INDEX IF NOT EXISTS idx_contents_published ON contents(published_at);
CREATE INDEX IF NOT EXISTS idx_regions_country ON regions(country_code);
CREATE INDEX IF NOT EXISTS idx_versions_location ON versions(country_code, region_code);

-- Insert default data
INSERT INTO countries (code, name, native_name, language_code, timezone, currency) VALUES
    ('FR', 'France', 'France', 'fr', 'Europe/Paris', 'EUR'),
    ('GB', 'United Kingdom', 'United Kingdom', 'en', 'Europe/London', 'GBP'),
    ('US', 'United States', 'United States', 'en', 'America/New_York', 'USD'),
    ('ES', 'Spain', 'España', 'es', 'Europe/Madrid', 'EUR'),
    ('DE', 'Germany', 'Deutschland', 'de', 'Europe/Berlin', 'EUR')
ON CONFLICT (code) DO NOTHING;

INSERT INTO regions (code, language_code, country_code, region_code, name, country_id) VALUES
    ('FR-IDF', 'fr', 'FR', 'IDF', 'Île-de-France', 'FR'),
    ('FR-PACA', 'fr', 'FR', 'PACA', 'Provence-Alpes-Côte d''Azur', 'FR'),
    ('FR-BRE', 'fr', 'FR', 'BRE', 'Bretagne', 'FR'),
    ('GB-ENG', 'en', 'GB', 'ENG', 'England', 'GB'),
    ('GB-SCO', 'en', 'GB', 'SCO', 'Scotland', 'GB'),
    ('US-CA', 'en', 'US', 'CA', 'California', 'US'),
    ('US-NY', 'en', 'US', 'NY', 'New York', 'US')
ON CONFLICT (code) DO NOTHING;
