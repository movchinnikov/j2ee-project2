-- ========================================================
-- V2: Add CLIENT and CLEANER roles
-- ========================================================
INSERT INTO roles (name, description) VALUES
    ('CLIENT',  'Platform customer — creates cleaning orders'),
    ('CLEANER', 'Cleaning professional — fulfills orders')
ON CONFLICT (name) DO NOTHING;
