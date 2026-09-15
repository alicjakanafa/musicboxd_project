package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Song;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.SongRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/artists")
public class ArtistController {

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;

    public ArtistController(
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            SongRepository songRepository
    ) {
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
    }

    @GetMapping("/{id}")
    public String showArtist(
            @PathVariable Long id,
            Model model
    ) {

        Optional<Artist> artist =
                artistRepository.findById(id);

        if (artist.isEmpty()) {
            return "redirect:/";
        }

        List<Album> albums =
                albumRepository.findByArtistId(id);

        List<Song> songs = new ArrayList<>();

        for (Album album : albums) {

            songs.addAll(
                    songRepository.findByAlbumId(album.getId())
            );
        }

        model.addAttribute(
                "artist",
                artist.get()
        );

        model.addAttribute(
                "albums",
                albums
        );

        model.addAttribute(
                "songs",
                songs
        );

        return "artist-profile";
    }
}