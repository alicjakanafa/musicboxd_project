package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Song;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.SongRepository;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.itunes.ItunesTrack;
import com.example.MusicBoxd.api.itunes.ItunesTrackResponse;
import com.example.MusicBoxd.api.lastfm.LastFmAlbum;
import com.example.MusicBoxd.api.lastfm.LastFmSearchResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class ItunesController {

    private final ItunesService itunesService;
    private final LastFmService lastFmService;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;

    public ItunesController(
            ItunesService itunesService,
            LastFmService lastFmService,
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            SongRepository songRepository
    ) {
        this.itunesService = itunesService;
        this.lastFmService = lastFmService;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
    }

    @GetMapping("/search")
    public String searchAlbums(
            @RequestParam String query,
            Model model
    ) {

        LastFmSearchResponse response =
                lastFmService.searchAlbums(query);

        model.addAttribute(
                "query",
                query
        );

        if (response != null &&
                response.getResults() != null &&
                response.getResults().getAlbummatches() != null &&
                response.getResults().getAlbummatches().getAlbum() != null) {

            model.addAttribute(
                    "albums",
                    response.getResults()
                            .getAlbummatches()
                            .getAlbum()
            );

        } else {

            model.addAttribute(
                    "albums",
                    List.of()
            );
        }

        return "album-search";
    }


    @GetMapping("/search/tracks")
    public String searchTracks(
            @RequestParam String query,
            Model model
    ) {

        ItunesTrackResponse response =
                itunesService.searchTracks(query);

        model.addAttribute(
                "tracks",
                response.getResults()
        );

        model.addAttribute(
                "query",
                query
        );

        return "track-search";
    }


    @GetMapping("/album/save")
    public String saveAlbum(
            @RequestParam Long collectionId,
            @RequestParam String artistName,
            @RequestParam String albumName,
            @RequestParam String artworkUrl,
            @RequestParam String releaseDate
    ) {

        Artist artist =
                artistRepository
                        .findByNameIgnoreCase(artistName)
                        .orElseGet(() ->
                                artistRepository.save(
                                        new Artist(artistName)
                                )
                        );


        Short releaseYear = null;

        if (releaseDate != null &&
                releaseDate.length() >= 4) {

            try {

                releaseYear =
                        Short.valueOf(
                                releaseDate.substring(0, 4)
                        );

            } catch (NumberFormatException ignored) {
            }
        }


        Optional<Album> existingAlbum =
                albumRepository.findByExternalId(
                        collectionId.toString()
                );


        Album album;


        if (existingAlbum.isPresent()) {

            album = existingAlbum.get();

            album.setTitle(albumName);
            album.setArtistId(artist.getId());


            if (artworkUrl != null &&
                    !artworkUrl.isBlank()) {

                album.setArtworkUrl(artworkUrl);
            }


            if (releaseYear != null) {

                album.setReleaseYear(
                        releaseYear
                );
            }


            album =
                    albumRepository.save(album);

        } else {

            album =
                    new Album(
                            collectionId.toString(),
                            artist.getId(),
                            albumName,
                            releaseYear,
                            artworkUrl
                    );


            album =
                    albumRepository.save(album);
        }


        saveSongs(
                album,
                collectionId
        );


        return "redirect:/albums/" +
                album.getId();
    }


    @GetMapping("/album/from-artist")
    public String getAlbumFromArtist(
            @RequestParam String artistName,
            @RequestParam String albumName
    ) {

        System.out.println(
                "ALBUM CLICKED"
        );

        System.out.println(
                "ARTIST: " +
                        artistName
        );

        System.out.println(
                "ALBUM: " +
                        albumName
        );


        Artist artist =
                artistRepository
                        .findByNameIgnoreCase(
                                artistName
                        )
                        .orElseGet(() ->
                                artistRepository.save(
                                        new Artist(
                                                artistName
                                        )
                                )
                        );


        Optional<Album> existingAlbum =
                albumRepository
                        .findByArtistId(
                                artist.getId()
                        )
                        .stream()
                        .filter(album ->
                                album.getTitle()
                                        .equalsIgnoreCase(
                                                albumName
                                        )
                        )
                        .findFirst();


        if (existingAlbum.isPresent()) {

            Album album =
                    existingAlbum.get();


            if (album.getArtworkUrl() == null ||
                    album.getArtworkUrl().isBlank()) {

                String artworkUrl =
                        getLastFmArtwork(
                                artistName,
                                albumName
                        );


                if (artworkUrl != null &&
                        !artworkUrl.isBlank()) {

                    album.setArtworkUrl(
                            artworkUrl
                    );

                    albumRepository.save(
                            album
                    );
                }
            }


            return "redirect:/albums/" +
                    album.getId();
        }


        String artworkUrl =
                getLastFmArtwork(
                        artistName,
                        albumName
                );


        System.out.println(
                "LAST.FM ARTWORK: " +
                        artworkUrl
        );

        Album album =
                new Album(
                        "lastfm:" +
                                artistName +
                                ":" +
                                albumName,

                        artist.getId(),

                        albumName,

                        null,

                        artworkUrl
                );


        Album savedAlbum =
                albumRepository.save(
                        album
                );


        return "redirect:/albums/" +
                savedAlbum.getId();
    }


    private String getLastFmArtwork(
            String artistName,
            String albumName
    ) {

        try {

            var response =
                    lastFmService.getAlbumInfo(
                            artistName,
                            albumName
                    );


            if (response == null ||
                    response.getAlbum() == null) {

                System.out.println(
                        "No Last.fm album found for: " +
                                artistName +
                                " - " +
                                albumName
                );

                return null;
            }


            LastFmAlbum album =
                    response.getAlbum();


            if (album.getImage() == null ||
                    album.getImage().isEmpty()) {

                System.out.println(
                        "Last.fm album has no images: " +
                                albumName
                );

                return null;
            }

            for (
                    int i =
                    album.getImage().size() - 1;

                    i >= 0;

                    i--
            ) {

                String imageUrl =
                        album
                                .getImage()
                                .get(i)
                                .getText();


                if (imageUrl != null &&
                        !imageUrl.isBlank()) {

                    return imageUrl;
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "LAST.FM ARTWORK ERROR: " +
                            e.getMessage()
            );
        }


        return null;
    }

    private void saveSongs(
            Album album,
            Long collectionId
    ) {

        ItunesTrackResponse trackResponse =
                itunesService.getAlbumTracks(
                        collectionId
                );


        if (trackResponse == null ||
                trackResponse.getResults() == null) {

            return;
        }


        for (ItunesTrack track :
                trackResponse.getResults()) {

            if (track.getTrackId() == null) {
                continue;
            }


            boolean songExists =
                    songRepository
                            .findByExternalId(
                                    track.getTrackId()
                                            .toString()
                            )
                            .isPresent();


            if (songExists) {
                continue;
            }


            Song song =
                    new Song(
                            track.getTrackId()
                                    .toString(),

                            album.getId(),

                            track.getTrackName(),

                            track.getTrackNumber(),

                            track.getPreviewUrl(),

                            track.getArtworkUrl100()
                    );


            songRepository.save(
                    song
            );
        }
    }

    private String normalise(
            String text
    ) {

        if (text == null) {
            return "";
        }


        return text
                .toLowerCase()
                .replace(
                        "&",
                        "and"
                )
                .replaceAll(
                        "\\([^)]*(remaster|deluxe|explicit|clean|expanded|anniversary)[^)]*\\)",
                        ""
                )
                .replaceAll(
                        "\\[[^\\]]*(remaster|deluxe|explicit|clean|expanded|anniversary)[^\\]]*\\]",
                        ""
                )
                .replaceAll(
                        "[-–—]",
                        " "
                )
                .replaceAll(
                        "[^a-z0-9 ]",
                        ""
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }
}
