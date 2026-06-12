CREATE TABLE food_logs (
    id                    UUID            PRIMARY KEY,
    log_date              DATE            NOT NULL,
    meal_type             VARCHAR(20)     NOT NULL,
    description_original  TEXT            NOT NULL,
    normalized_food_name  TEXT,
    quantity_amount       NUMERIC(12, 3),
    quantity_unit         VARCHAR(50),
    calories              NUMERIC(10, 2)  NOT NULL DEFAULT 0,
    protein_grams         NUMERIC(10, 2)  NOT NULL DEFAULT 0,
    carbs_grams           NUMERIC(10, 2)  NOT NULL DEFAULT 0,
    fat_grams             NUMERIC(10, 2)  NOT NULL DEFAULT 0,
    confidence            NUMERIC(3, 2)   NOT NULL DEFAULT 0,
    source                VARCHAR(30)     NOT NULL,
    notes                 TEXT,
    explanation           TEXT,
    created_at            TIMESTAMPTZ     NOT NULL,
    updated_at            TIMESTAMPTZ     NOT NULL,
    CONSTRAINT chk_food_logs_calories_non_negative CHECK (calories >= 0),
    CONSTRAINT chk_food_logs_protein_non_negative  CHECK (protein_grams >= 0),
    CONSTRAINT chk_food_logs_carbs_non_negative    CHECK (carbs_grams >= 0),
    CONSTRAINT chk_food_logs_fat_non_negative      CHECK (fat_grams >= 0),
    CONSTRAINT chk_food_logs_confidence_range      CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT chk_food_logs_meal_type             CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK', 'OTHER'))
);

CREATE INDEX idx_food_logs_log_date   ON food_logs (log_date);
CREATE INDEX idx_food_logs_created_at ON food_logs (created_at);
