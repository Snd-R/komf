CREATE TABLE MATCHED_SERIES
(
    SERIES_ID          varchar NOT NULL PRIMARY KEY,
    THUMBNAIL_ID       varchar,
    PROVIDER           varchar NOT NULL,
    PROVIDER_SERIES_ID varchar NOT NULL
);
