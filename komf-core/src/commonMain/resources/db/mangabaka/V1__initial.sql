CREATE TABLE komga_series
(
    komga_id     TEXT NOT NULL,
    mangabaka_id TEXT NOT NULL,
    PRIMARY KEY (komga_id, mangabaka_id)
);

CREATE TABLE import_metadata
(
    key   TEXT NOT NULL PRIMARY KEY,
    value TEXT NOT NULL
)
