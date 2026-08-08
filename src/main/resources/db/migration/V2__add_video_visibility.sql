-- Adds the visibility dimension for the YouTube-like model: a public feed plus
-- owner-only private videos. Existing rows were uploaded under the old
-- owner-only model, so they are backfilled to PRIVATE (never silently exposed).
-- New uploads set visibility explicitly from the application.

alter table videos
    add column visibility varchar(255) not null default 'PRIVATE'
        check (visibility in ('PUBLIC', 'UNLISTED', 'PRIVATE'));

-- Feed query hits (visibility, status) and orders by created_at desc.
create index idx_videos_feed on videos (visibility, status, created_at desc);
