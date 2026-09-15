TRUNCATE TABLE
    public.comments,
    public.reviews,
    public.songs,
    public.albums,
    public.tags,
    public.artists,
    public.users
RESTART IDENTITY;

INSERT INTO public.users (id, google_user_id, username, email, bio, profile_picture_url) VALUES
 (1, 'test-google-001', 'vinylfan', 'vinylfan@example.com', 'Always looking for my next favourite album.', NULL),
 (2, 'test-google-002', 'indiekid', 'indiekid@example.com', 'Indie and alternative music enthusiast.', NULL),
 (3, 'test-google-003', 'jazzhands', 'jazzhands@example.com', 'Jazz, soul and late-night listening.', NULL),
 (4, 'test-google-004', 'beatseeker', 'beatseeker@example.com', 'Electronic music and great production.', NULL),
 (5, 'test-google-005', 'newlistener', 'newlistener@example.com', NULL, NULL);

INSERT INTO public.artists (id, name) VALUES
(1, 'The Midnight Signals'),
(2, 'Paper Satellites'),
(3, 'Ella Blue'),
(4, 'Neon Harbour');

-- Standalone tags: the supplied schema has no tag association table.
INSERT INTO public.tags (id, name) VALUES
(1, 'Rock'), (2, 'Indie'), (3, 'Jazz'),
(4, 'Electronic'), (5, 'Soul'), (6, 'Pop');

INSERT INTO public.albums (id, external_id, artist_id, title, release_year, artwork_url) VALUES
(1, 'test-album-001', 1, 'After the Last Train', 2022, NULL),
(2, 'test-album-002', 2, 'Folded Maps', 2023, NULL),
(3, 'test-album-003', 3, 'Sunday Sessions', 2021, NULL),
(4, 'test-album-004', 4, 'City Lights', 2024, NULL),
(5, 'test-album-005', 1, 'Morning Static', 2025, NULL);

INSERT INTO public.songs (id, external_id, album_id, title, track_number, song_url, song_image_url) VALUES
(1, 'test-song-001', 1, 'Platform Nine', 1, NULL, NULL),
(2, 'test-song-002', 1, 'Streetlights', 2, NULL, NULL),
(3, 'test-song-003', 1, 'Last Train Home', 3, NULL, NULL),
(4, 'test-song-004', 2, 'Postcards', 1, NULL, NULL),
(5, 'test-song-005', 2, 'Small Town Echoes', 2, NULL, NULL),
(6, 'test-song-006', 2, 'Paper Planes', 3, NULL, NULL),
(7, 'test-song-007', 3, 'Coffee at Noon', 1, NULL, NULL),
(8, 'test-song-008', 3, 'Blue Window', 2, NULL, NULL),
(9, 'test-song-009', 3, 'Slow Sunday', 3, NULL, NULL),
(10, 'test-song-010', 4, 'Neon Tide', 1, NULL, NULL),
(11, 'test-song-011', 4, 'Digital Skyline', 2, NULL, NULL),
(12, 'test-song-012', 4, 'Dawn Transmission', 3, NULL, NULL),
(13, 'test-song-013', 5, 'Wake Up Call', 1, NULL, NULL),
(14, 'test-song-014', 5, 'Morning Static', 2, NULL, NULL),
(15, 'test-song-015', 5, 'Open Windows', 3, NULL, NULL);

INSERT INTO public.reviews (id, user_id, album_id, song_id, header, content, rating, created_at) VALUES
 (1, 1, 1, NULL, 'A brilliant late-night album', 'Great atmosphere from start to finish. Streetlights is a highlight.', 4.5, CURRENT_TIMESTAMP - INTERVAL '10 days'),
 (2, 2, 2, NULL, 'An indie favourite', 'Catchy melodies and thoughtful lyrics. Worth a second listen.', 4.0, CURRENT_TIMESTAMP - INTERVAL '9 days'),
 (3, 3, 3, NULL, 'Perfect Sunday listening', 'Warm vocals and a relaxed sound. Exactly what I hoped for.', 5.0, CURRENT_TIMESTAMP - INTERVAL '8 days'),
 (4, 4, 4, NULL, 'Production steals the show', 'Some excellent beats, although the middle tracks feel similar.', 3.5, CURRENT_TIMESTAMP - INTERVAL '7 days'),
 (5, 2, 1, NULL, 'Good, but a little repetitive', 'A strong opening, but I wanted more variety towards the end.', 3.0, CURRENT_TIMESTAMP - INTERVAL '6 days'),
 (6, 1, NULL, 2, 'Cannot stop replaying this', 'The chorus stays with you long after the song ends.', 5.0, CURRENT_TIMESTAMP - INTERVAL '5 days'),
 (7, 2, NULL, 4, 'A lovely opening track', 'Simple instrumentation with a memorable melody.', 4.0, CURRENT_TIMESTAMP - INTERVAL '4 days'),
 (8, 3, NULL, 8, 'Beautiful vocals', 'The vocal performance makes this my favourite track on the album.', 4.5, CURRENT_TIMESTAMP - INTERVAL '3 days'),
 (9, 4, NULL, 10, 'Great energy', 'A track that sounds even better through headphones.', 4.0, CURRENT_TIMESTAMP - INTERVAL '2 days'),
 (10, 1, NULL, 11, 'Not for me', 'Interesting production, but the melody did not grab me.', 2.0, CURRENT_TIMESTAMP - INTERVAL '1 day');

INSERT INTO public.comments (id, user_id, review_id, content, created_at) VALUES
  (1, 2, 1, 'Streetlights was my favourite too!', CURRENT_TIMESTAMP - INTERVAL '9 days'),
  (2, 4, 1, 'I need to give this another listen.', CURRENT_TIMESTAMP - INTERVAL '8 days'),
  (3, 1, 2, 'Paper Planes is such a good closing track.', CURRENT_TIMESTAMP - INTERVAL '8 days'),
  (4, 2, 3, 'Adding this to my weekend playlist.', CURRENT_TIMESTAMP - INTERVAL '7 days'),
  (5, 1, 4, 'Agreed about the production.', CURRENT_TIMESTAMP - INTERVAL '6 days'),
  (6, 3, 5, 'Fair point, although I enjoyed the slower tracks.', CURRENT_TIMESTAMP - INTERVAL '5 days'),
  (7, 4, 6, 'That chorus is excellent.', CURRENT_TIMESTAMP - INTERVAL '4 days'),
  (8, 3, 7, 'A great introduction to the album.', CURRENT_TIMESTAMP - INTERVAL '3 days'),
  (9, 1, 8, 'Ella Blue deserves more attention.', CURRENT_TIMESTAMP - INTERVAL '2 days'),
  (10, 2, 9, 'This would be brilliant live.', CURRENT_TIMESTAMP - INTERVAL '1 day');

SELECT setval('public.users_id_seq', (SELECT MAX(id) FROM public.users), true);
SELECT setval('public.artists_id_seq', (SELECT MAX(id) FROM public.artists), true);
SELECT setval('public.tags_id_seq', (SELECT MAX(id) FROM public.tags), true);
SELECT setval('public.albums_id_seq', (SELECT MAX(id) FROM public.albums), true);
SELECT setval('public.songs_id_seq', (SELECT MAX(id) FROM public.songs), true);
SELECT setval('public.reviews_id_seq', (SELECT MAX(id) FROM public.reviews), true);
SELECT setval('public.comments_id_seq', (SELECT MAX(id) FROM public.comments), true);