-- V2__marketplaces_and_care_picks.sql
-- 1. A product can also be bought on Amazon / Flipkart ("Also on ..." buttons on the product page).
-- 2. Care picks: affiliate links to things that go with the shoes (socks, cleaners, insoles).
-- 3. Every click out to a marketplace is counted, so the dashboard can show what people follow.

ALTER TABLE products ADD COLUMN amazon_url   VARCHAR(600);
ALTER TABLE products ADD COLUMN flipkart_url VARCHAR(600);

CREATE TABLE care_picks
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    title        VARCHAR(120) NOT NULL,
    blurb        VARCHAR(300),
    image        VARCHAR(600),
    marketplace  VARCHAR(20)  NOT NULL CHECK (marketplace IN ('AMAZON', 'FLIPKART')),
    url          VARCHAR(600) NOT NULL,
    sort_order   INTEGER      NOT NULL DEFAULT 0,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,

    -- BaseEntity
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version      BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_care_picks_active_sort ON care_picks (sort_order) WHERE active;

-- No personal data: only what was clicked and where it went.
CREATE TABLE outbound_clicks
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    target_type  VARCHAR(20)  NOT NULL CHECK (target_type IN ('PRODUCT', 'PICK')),
    target_key   VARCHAR(80)  NOT NULL,
    target_name  VARCHAR(160) NOT NULL,
    marketplace  VARCHAR(20)  NOT NULL CHECK (marketplace IN ('AMAZON', 'FLIPKART')),

    -- BaseEntity
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version      BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_outbound_clicks_created ON outbound_clicks (created_at DESC);
