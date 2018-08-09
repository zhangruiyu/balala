-- auto-generated definition
CREATE TABLE haircut_root_type
(
  id          SERIAL NOT NULL
    CONSTRAINT haircut_root_type_pkey
    PRIMARY KEY,
  subtypes       JSONB,
  name      VARCHAR(255) DEFAULT '' :: CHARACTER VARYING,
  store_id      INT     DEFAULT 0 ,
  create_time TIMESTAMP
);

