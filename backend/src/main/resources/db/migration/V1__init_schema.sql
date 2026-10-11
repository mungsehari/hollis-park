-- V1__init_schema.sql
-- Hollis Park store: the whole schema in one migration.
-- Every table has the BaseEntity columns: id (UUID), created_at, updated_at and version (optimistic locking).
-- Add the next change as V2__... ; never edit this file once it has run on a database.

-- Collections (Running, Basketball, ...), edited in the admin panel
CREATE TABLE collections
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    slug        VARCHAR(60)  NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    tagline     VARCHAR(300),
    intro       VARCHAR(2000),
    image       VARCHAR(600),
    alt         VARCHAR(500),
    choose      JSONB,
    guide       JSONB,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    -- BaseEntity
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version     BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_collections_active_sort ON collections (sort_order) WHERE active;

-- Products
CREATE TABLE products
(
    id               UUID PRIMARY KEY          DEFAULT gen_random_uuid(),
    slug             VARCHAR(80)      NOT NULL UNIQUE,
    name             VARCHAR(120)     NOT NULL,
    collection_slug  VARCHAR(60)      NOT NULL REFERENCES collections (slug) ON UPDATE CASCADE,
    line_name        VARCHAR(120),
    colourway        VARCHAR(120),
    swatch           VARCHAR(300),
    price            INTEGER          NOT NULL CHECK (price >= 0),
    rating           DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (rating BETWEEN 0 AND 5),
    review_count     INTEGER          NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    flag             VARCHAR(60),
    sort_rank        INTEGER          NOT NULL DEFAULT 100,
    added_on         VARCHAR(10),
    img              VARCHAR(600),
    context_img      VARCHAR(600),
    context_caption  VARCHAR(300),
    alt              VARCHAR(500),
    blurb            VARCHAR(1000),
    description      VARCHAR(3000),
    care             VARCHAR(1000),
    keywords         VARCHAR(1000),
    colours          JSONB,
    wide             JSONB,
    specs            JSONB,
    materials        JSONB,
    fit              JSONB,
    sold_out         JSONB,
    low              JSONB,
    gallery          JSONB,
    active           BOOLEAN          NOT NULL DEFAULT TRUE,

    -- BaseEntity
    created_at       TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    version          BIGINT           NOT NULL DEFAULT 0
);
CREATE INDEX idx_products_collection_slug ON products (collection_slug);
CREATE INDEX idx_products_active_rank ON products (sort_rank) WHERE active;

-- Reviews
CREATE TABLE reviews
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    product_slug  VARCHAR(80)  NOT NULL REFERENCES products (slug) ON DELETE CASCADE ON UPDATE CASCADE,
    name          VARCHAR(120),
    place         VARCHAR(120),
    shoe_size     VARCHAR(20),
    fit_score     INTEGER      NOT NULL DEFAULT 0,
    rating        INTEGER      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_date   VARCHAR(20),
    body          VARCHAR(2000),
    sort_order    INTEGER      NOT NULL DEFAULT 0,

    -- BaseEntity
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version       BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_reviews_product_slug ON reviews (product_slug, sort_order);

-- Orders. Customers see order_number (HP-24001); the UUID is internal.
CREATE SEQUENCE order_number_seq START WITH 24001;

CREATE TABLE orders
(
    id                   UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    order_number         VARCHAR(20)    NOT NULL UNIQUE,
    email                VARCHAR(200)   NOT NULL,
    first_name           VARCHAR(80)    NOT NULL,
    last_name            VARCHAR(80)    NOT NULL,
    address              VARCHAR(300)   NOT NULL,
    city                 VARCHAR(100)   NOT NULL,
    pin                  VARCHAR(10)    NOT NULL,
    ship_method          VARCHAR(20)    NOT NULL CHECK (ship_method IN ('standard', 'express', 'pickup')),
    promo_code           VARCHAR(40),
    subtotal             NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    discount             NUMERIC(12, 2) NOT NULL CHECK (discount >= 0),
    shipping             NUMERIC(12, 2) NOT NULL CHECK (shipping >= 0),
    tax                  NUMERIC(12, 2) NOT NULL CHECK (tax >= 0),
    total                NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    status               VARCHAR(20)    NOT NULL CHECK (status IN
                                                        ('PENDING_PAYMENT', 'RECEIVED', 'CONFIRMED', 'SHIPPED',
                                                         'DELIVERED', 'CANCELLED')),
    payment_status       VARCHAR(20)    NOT NULL DEFAULT 'NONE' CHECK (payment_status IN ('NONE', 'PENDING', 'PAID', 'FAILED')),
    razorpay_order_id    VARCHAR(60),
    razorpay_payment_id  VARCHAR(60),
    paid_at              TIMESTAMPTZ,
    admin_note           VARCHAR(2000),
    items                JSONB          NOT NULL,
    history              JSONB,

    -- BaseEntity
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    version              BIGINT         NOT NULL DEFAULT 0
);
CREATE INDEX idx_orders_created_at ON orders (created_at DESC);
CREATE INDEX idx_orders_status_created ON orders (status, created_at DESC);
CREATE INDEX idx_orders_payment_status ON orders (payment_status) WHERE payment_status <> 'NONE';
CREATE UNIQUE INDEX uk_orders_razorpay_order ON orders (razorpay_order_id) WHERE razorpay_order_id IS NOT NULL;

-- Contact form messages
CREATE TABLE contact_messages
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name          VARCHAR(120)  NOT NULL,
    email         VARCHAR(200)  NOT NULL,
    topic         VARCHAR(40)   NOT NULL,
    order_number  VARCHAR(40),
    fitting_slot  VARCHAR(80),
    body          VARCHAR(4000) NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'HANDLED')),

    -- BaseEntity
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version       BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_contact_messages_status ON contact_messages (status, created_at DESC);

-- Newsletter subscribers
CREATE TABLE subscribers
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    email       VARCHAR(200) NOT NULL UNIQUE,
    topics      JSONB,

    -- BaseEntity
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version     BIGINT       NOT NULL DEFAULT 0
);

-- Staff accounts for the admin panel
CREATE TABLE staff_users
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    email          VARCHAR(200) NOT NULL,
    name           VARCHAR(120) NOT NULL,
    password_hash  VARCHAR(200) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'STAFF')),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  TIMESTAMPTZ,

    -- BaseEntity
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version        BIGINT       NOT NULL DEFAULT 0
);
-- Sign-in looks the email up case-insensitively; this also stops two accounts that differ only by case.
CREATE UNIQUE INDEX uk_staff_users_email_lower ON staff_users (LOWER(email));

-- Tax and delivery settings: exactly one row
CREATE TABLE store_settings
(
    id              UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    tax_mode        VARCHAR(10)   NOT NULL CHECK (tax_mode IN ('included', 'added')),
    tax_rate        NUMERIC(5, 4) NOT NULL CHECK (tax_rate >= 0 AND tax_rate < 1),
    tax_name        VARCHAR(40)   NOT NULL,
    free_ship_from  INTEGER       NOT NULL CHECK (free_ship_from >= 0),
    ship_fee        INTEGER       NOT NULL CHECK (ship_fee >= 0),
    express_fee     INTEGER       NOT NULL CHECK (express_fee >= 0),
    singleton       BOOLEAN       NOT NULL DEFAULT TRUE UNIQUE CHECK (singleton),

    -- BaseEntity
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version         BIGINT        NOT NULL DEFAULT 0
);

-- Discount codes (stored upper case)
CREATE TABLE promo_codes
(
    id          UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    code        VARCHAR(40)   NOT NULL UNIQUE,
    rate        NUMERIC(5, 4) NOT NULL CHECK (rate > 0 AND rate < 1),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,

    -- BaseEntity
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version     BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_promo_codes_upper_code ON promo_codes (UPPER(code));

-- Uploaded images (product photos, collection photos, homepage hero)
CREATE TABLE media
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    storage_key    VARCHAR(300) NOT NULL UNIQUE,
    url            VARCHAR(600) NOT NULL,
    original_name  VARCHAR(300),
    content_type   VARCHAR(60)  NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp', 'image/avif')),
    size_bytes     BIGINT       NOT NULL CHECK (size_bytes >= 0),
    alt            VARCHAR(300),
    created_by     VARCHAR(200),

    -- BaseEntity
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version        BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_media_created_at ON media (created_at DESC);

-- Editable page content, one JSON document per page ('home')
CREATE TABLE site_content
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    page_key    VARCHAR(40)  NOT NULL UNIQUE,
    data        JSONB        NOT NULL,

    -- BaseEntity
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version     BIGINT       NOT NULL DEFAULT 0
);

-- Spring Session: admin sign-ins survive restarts and work across several API instances.
-- (Table layout is fixed by Spring Session, so it does not follow BaseEntity.)
CREATE TABLE spring_session
(
    primary_id             CHAR(36) NOT NULL,
    session_id             CHAR(36) NOT NULL,
    creation_time          BIGINT   NOT NULL,
    last_access_time       BIGINT   NOT NULL,
    max_inactive_interval  INT      NOT NULL,
    expiry_time            BIGINT   NOT NULL,
    principal_name         VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);
CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes
(
    session_primary_id  CHAR(36)     NOT NULL,
    attribute_name      VARCHAR(200) NOT NULL,
    attribute_bytes     BYTEA        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session (primary_id) ON DELETE CASCADE
);
