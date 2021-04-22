CREATE TABLE IF NOT EXISTS many2one_employee (
    company_id BIGINT,
    employee_no BIGINT,
    CONSTRAINT pk_employee PRIMARY KEY (company_id, employee_no)
);

CREATE TABLE IF NOT EXISTS many2one_phone (
    phone_no VARCHAR(255) PRIMARY KEY,
    company_id BIGINT NULL,
    employee_no BIGINT NULL,
    CONSTRAINT fk_phone_employee FOREIGN KEY (company_id, employee_no)
        REFERENCES many2one_employee(company_id, employee_no) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE IF NOT EXISTS player (
    id BIGSERIAL PRIMARY KEY, "name" VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS "character" (
    id BIGINT,
    season INT,
    "name" VARCHAR(255) NOT NULL,
     CONSTRAINT pk_character PRIMARY KEY (id, season)
);

CREATE TABLE IF NOT EXISTS player_characters (
    id_source_id BIGINT NOT NULL,
    "characterIdId_target_id" BIGINT NOT NULL,
    "characterIdSeason_target_id" INT NOT NULL,
    CONSTRAINT fk_player_characters_player FOREIGN KEY (id_source_id) REFERENCES player(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_player_characters_character FOREIGN KEY ("characterIdId_target_id", "characterIdSeason_target_id")
        REFERENCES "character"(id, season) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE IF NOT EXISTS reasons (
    id serial primary key,
    reason varchar,
    details json
)
