ALTER TABLE MATCHED_SERIES
    RENAME TO MATCHED_SERIES_OLD;

CREATE TABLE MATCHED_SERIES
(
    SERIES_ID    varchar NOT NULL PRIMARY KEY,
    THUMBNAIL_ID varchar
);

INSERT INTO MATCHED_SERIES (SERIES_ID, THUMBNAIL_ID)
SELECT SERIES_ID, THUMBNAIL_ID
FROM MATCHED_SERIES_OLD;

DROP TABLE MATCHED_SERIES_OLD
