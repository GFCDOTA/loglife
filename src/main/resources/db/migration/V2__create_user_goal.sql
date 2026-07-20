-- The user's daily nutrition goal. Single-user app: the CHECK pins the table to one row.
CREATE TABLE user_goal (
    id             SMALLINT        PRIMARY KEY DEFAULT 1,
    calories       NUMERIC(10, 2)  NOT NULL,
    protein_grams  NUMERIC(10, 2),
    carbs_grams    NUMERIC(10, 2),
    fat_grams      NUMERIC(10, 2),
    updated_at     TIMESTAMPTZ     NOT NULL,
    CONSTRAINT chk_user_goal_single_row        CHECK (id = 1),
    CONSTRAINT chk_user_goal_calories_positive CHECK (calories > 0),
    CONSTRAINT chk_user_goal_protein_non_neg   CHECK (protein_grams IS NULL OR protein_grams >= 0),
    CONSTRAINT chk_user_goal_carbs_non_neg     CHECK (carbs_grams IS NULL OR carbs_grams >= 0),
    CONSTRAINT chk_user_goal_fat_non_neg       CHECK (fat_grams IS NULL OR fat_grams >= 0)
);
