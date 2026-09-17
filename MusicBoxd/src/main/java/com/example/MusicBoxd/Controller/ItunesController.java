package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Song;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.SongRepository;
import com.example.MusicBoxd.api.itunes.ItunesAlbumResponse;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.itunes.ItunesTrack;
import com.example.MusicBoxd.api.itunes.ItunesTrackResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class ItunesController {

    private final ItunesService itunesService;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;

    public ItunesController(
            ItunesService itunesService,
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            SongRepository songRepository
    ) {
        this.itunesService = itunesService;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
    }

    @GetMapping("/search")
    public String searchAlbums(
            @RequestParam String query,
            Model model
    ) {

        ItunesAlbumResponse response =
                itunesService.searchAlbums(query);

        model.addAttribute(
                "albums",
                response.getResults()
        );

        model.addAttribute(
                "query",
                query
        );

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


        Optional<Artist> existingArtist =
                artistRepository.findByNameIgnoreCase(
                        artistName
                );

        Artist artist;

        if (existingArtist.isPresent()) {

            artist = existingArtist.get();

        } else {

            artist = new Artist(artistName);

            artist =
                    artistRepository.save(artist);
        }



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

            album =
                    existingAlbum.get();



            if (artworkUrl != null &&
                    !artworkUrl.isBlank()) {

                album.setArtworkUrl(
                        artworkUrl
                );
            }

            album.setTitle(albumName);

            album.setArtistId(
                    artist.getId()
            );

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


        ItunesTrackResponse trackResponse =
                itunesService.getAlbumTracks(
                        collectionId
                );



        if (trackResponse != null &&
                trackResponse.getResults() != null) {

            for (ItunesTrack track :
                    trackResponse.getResults()) {



                if (track.getTrackId() == null) {
                    continue;
                }



                boolean songExists =
                        songRepository
                                .findByExternalId(
                                        track
                                                .getTrackId()
                                                .toString()
                                )
                                .isPresent();

                if (songExists) {
                    continue;
                }


                Song song =
                        new Song(
                                track
                                        .getTrackId()
                                        .toString(),

                                album.getId(),

                                track.getTrackName(),

                                track.getTrackNumber(),

                                track.getPreviewUrl(),

                                track.getArtworkUrl100()
                        );

                songRepository.save(song);
            }
        }




        return "redirect:/albums/" + album.getId();
    }


    @GetMapping("/album/from-artist")
    public String getAlbumFromArtist(
            @RequestParam String artistName,
            @RequestParam String albumName
    ) {

        ItunesAlbumResponse response =
                itunesService.searchAlbums(
                        artistName + " " + albumName
                );

        if (response == null ||
                response.getResults() == null ||
                response.getResults().isEmpty()) {

            return "redirect:/artists";
        }


        var album = response.getResults().get(0);


        return "redirect:/album/save"
                + "?collectionId=" + album.getCollectionId()
                + "&artistName=" + artistName
                + "&albumName=" + album.getCollectionName()
                + "&artworkUrl=" + album.getHighResolutionArtworkUrl()
                + "&releaseDate=" + album.getReleaseDate();
    }


}

