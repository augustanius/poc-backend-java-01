CREATE SCHEMA IF NOT EXISTS poc_user;

CREATE TABLE IF NOT EXISTS poc_user.users (
    id          VARCHAR(36)  NOT NULL,
    username    VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
