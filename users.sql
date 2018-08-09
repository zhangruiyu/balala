-- auto-generated definition
CREATE TABLE users
(
  id          SERIAL NOT NULL
    CONSTRAINT users_pkey
    PRIMARY KEY,
  autos       JSONB,
  avatar      VARCHAR(255) DEFAULT '' :: CHARACTER VARYING,
  gender      SMALLINT     DEFAULT 0,
  nickname    VARCHAR(25)  DEFAULT '' :: CHARACTER VARYING,
  create_time TIMESTAMP,
  birthday    TIMESTAMP
);

CREATE UNIQUE INDEX user_autos_index
  ON users ((autos ->> 'identifier' :: TEXT));

CREATE INDEX autos_ginp
  ON users (autos);

