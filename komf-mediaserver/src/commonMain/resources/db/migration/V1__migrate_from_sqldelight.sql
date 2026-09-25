CREATE TABLE IF NOT EXISTS BookThumbnail
(
    bookId      TEXT NOT NULL PRIMARY KEY,
    seriesId    TEXT NOT NULL,
    thumbnailId TEXT,
    mediaServer TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS book_thumbnails_series_idx ON BookThumbnail (seriesId);
CREATE INDEX IF NOT EXISTS book_thumbnails_server_type_idx ON BookThumbnail (mediaServer);

CREATE TABLE IF NOT EXISTS SeriesThumbnail
(
    seriesId    TEXT NOT NULL PRIMARY KEY,
    thumbnailId TEXT,
    mediaServer TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS series_thumbnail_server_type_idx ON SeriesThumbnail (mediaServer);

CREATE TABLE IF NOT EXISTS SeriesMatch
(
    seriesId         TEXT NOT NULL,
    type             TEXT NOT NULL,
    mediaServer      TEXT NOT NULL,
    provider         TEXT NOT NULL,
    providerSeriesId TEXT NOT NULL,
    PRIMARY KEY (seriesId, mediaServer)
);
CREATE INDEX IF NOT EXISTS series_match_type_idx ON SeriesMatch (type);


CREATE TABLE IF NOT EXISTS KomfJobRecord
(
    id         TEXT    NOT NULL,
    seriesId   TEXT    NOT NULL,
    status     TEXT    NOT NULL,
    message    TEXT,
    startedAt  INTEGER NOT NULL,
    finishedAt INTEGER,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS komf_job_series_id_idx ON KomfJobRecord (seriesId);
CREATE INDEX IF NOT EXISTS komf_job_status_idx ON KomfJobRecord (status);
CREATE INDEX IF NOT EXISTS komf_job_started_at_idx ON KomfJobRecord (startedAt);
CREATE INDEX IF NOT EXISTS komf_job_finished_at_idx ON KomfJobRecord (finishedAt);
