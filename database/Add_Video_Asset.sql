-- Add support for video asset (YouTube embed link only)
-- Run this after Create_Database.sql

CREATE TABLE IF NOT EXISTS video_asset (
    mediaid       BIGINT,
    youtube_url   TEXT NOT NULL,
    CONSTRAINT pk_video_asset PRIMARY KEY (mediaid),
    CONSTRAINT fk_video_asset_media
        FOREIGN KEY (mediaid) REFERENCES media_asset(mediaid)
);

