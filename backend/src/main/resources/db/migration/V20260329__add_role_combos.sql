CREATE TABLE role_combos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    color VARCHAR(7),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_combos_business FOREIGN KEY (business_id) REFERENCES businesses(id)
);

CREATE TABLE role_combo_roles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_combo_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_rcr_combo FOREIGN KEY (role_combo_id) REFERENCES role_combos(id) ON DELETE CASCADE,
    CONSTRAINT fk_rcr_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT uq_combo_role UNIQUE (role_combo_id, role_id)
);

-- Add optional role_combo_id to schedule_template_shifts
ALTER TABLE schedule_template_shifts ADD COLUMN role_combo_id BIGINT;
ALTER TABLE schedule_template_shifts ADD CONSTRAINT fk_template_shifts_combo
    FOREIGN KEY (role_combo_id) REFERENCES role_combos(id) ON DELETE CASCADE;

-- Make role_id nullable (template entry can be either a role OR a combo, not both)
ALTER TABLE schedule_template_shifts ALTER COLUMN role_id DROP NOT NULL;

-- Ensure exactly one of role_id or role_combo_id is set
ALTER TABLE schedule_template_shifts ADD CONSTRAINT chk_role_or_combo
    CHECK ((role_id IS NOT NULL AND role_combo_id IS NULL)
        OR (role_id IS NULL AND role_combo_id IS NOT NULL));
