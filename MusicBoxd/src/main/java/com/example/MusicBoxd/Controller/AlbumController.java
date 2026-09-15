package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.SongRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/albums")
public class AlbumController {

    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;

    public AlbumController(
            AlbumRepository albumRepository,
            ArtistRepository artistRepository,
            SongRepository songRepository
    ) {
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.songRepository = songRepository;
    }

    @GetMapping("/{id}")
    public String showAlbum(
            @PathVariable Long id,
            Model model
    ) {

        Optional<Album> album =
                albumRepository.findById(id);

        if (album.isEmpty()) {
            return "redirect:/";
        }

        Album currentAlbum = album.get();

        Optional<Artist> artist =
                artistRepository.findById(
                        currentAlbum.getArtistId()
                );

        model.addAttribute(
                "album",
                currentAlbum
        );

        if (artist.isPresent()) {
            model.addAttribute(
                    "artist",
                    artist.get()
            );
        }

        model.addAttribute(
                "songs",
                songRepository.findByAlbumId(id)
        );

        return "album-profile";
    }
}