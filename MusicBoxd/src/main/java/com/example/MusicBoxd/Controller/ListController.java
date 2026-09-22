package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.ListItem;
import com.example.MusicBoxd.Model.ListType;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ListItemRepository;
import com.example.MusicBoxd.Repository.ListRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.lastfm.LastFmAlbum;
import com.example.MusicBoxd.api.lastfm.LastFmAlbumResponse;
import com.example.MusicBoxd.api.lastfm.LastFmSearchResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Repository.ArtistRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/lists")
public class ListController {

    private final ListRepository listRepository;
    private final ListItemRepository listItemRepository;
    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final LastFmService lastFmService;
    private final ArtistRepository artistRepository;

    public ListController(
            ListRepository listRepository,
            ListItemRepository listItemRepository,
            AlbumRepository albumRepository,
            UserRepository userRepository,
            LastFmService lastFmService,
            ArtistRepository artistRepository
    ) {
        this.listRepository = listRepository;
        this.listItemRepository = listItemRepository;
        this.albumRepository = albumRepository;
        this.userRepository = userRepository;
        this.lastFmService = lastFmService;
        this.artistRepository = artistRepository;
    }


    @GetMapping
    public String lists(
            Authentication authentication,
            Model model
    ) {
        User user = getCurrentUser(authentication);
        model.addAttribute("lists", listRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        return "placeholder-lists";
    }

    @PostMapping
    public String createList(Authentication authentication, @RequestParam String title, @RequestParam String description) {
        User user = getCurrentUser(authentication);
        com.example.MusicBoxd.Model.List list =
                new com.example.MusicBoxd.Model.List(
                        user.getId(),
                        title,
                        description,
                        ListType.CUSTOM
                );
        listRepository.save(list);
        return "redirect:/lists";
    }


    @GetMapping("/{id}")
    public String showList(Authentication authentication, @PathVariable Long id, Model model) {
        User user = getCurrentUser(authentication);
        com.example.MusicBoxd.Model.List list =
                listRepository.findById(id).orElseThrow();
        if (!list.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        List<ListItem> items =
                listItemRepository.findByListIdOrderByPositionAsc(id);
        Map<Long, Album> albums = new HashMap<>();
        for (ListItem item : items) {
            if (item.getAlbumId() != null) {
                Album album = albumRepository
                        .findById(item.getAlbumId())
                        .orElse(null);
                if (album != null) {
                    albums.put(item.getAlbumId(), album);
                }
            }
        }
        model.addAttribute("list", list);
        model.addAttribute("items", items);
        model.addAttribute("albums", albums);
        return "placeholder-list-items";
    }

    @GetMapping("/{id}/search")
    public String searchAlbums(Authentication authentication, @PathVariable Long id, @RequestParam String query, Model model
    ) {
        User user = getCurrentUser(authentication);
        com.example.MusicBoxd.Model.List list = listRepository.findById(id).orElseThrow();

        if (!list.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        LastFmSearchResponse response =
                lastFmService.searchAlbums(query);
        model.addAttribute("listId", id);
        if (response != null
                && response.getResults() != null
                && response.getResults().getAlbummatches() != null
                && response.getResults().getAlbummatches().getAlbum() != null) {
            model.addAttribute("results", response.getResults().getAlbummatches().getAlbum());
        } else {
            model.addAttribute("results", List.of());
        }

        return "placeholder-list-items :: searchResults";
    }


    @PostMapping("/{id}/albums")
    public String addAlbum(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String artist
    ) {

        User user = getCurrentUser(authentication);

        com.example.MusicBoxd.Model.List list =
                listRepository.findById(id).orElseThrow();

        if (!list.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        int nextPosition =
                listItemRepository.findMaxPosition(id) + 1;

        LastFmAlbumResponse response =
                lastFmService.getAlbumInfo(artist, title);

        LastFmAlbum lastFmAlbum = null;

        if (response != null) {
            lastFmAlbum = response.getAlbum();
        }

        String finalTitle = title;
        Short releaseYear = null;
        String artworkUrl = null;

        if (lastFmAlbum != null) {

            if (lastFmAlbum.getName() != null &&
                    !lastFmAlbum.getName().isBlank()) {

                finalTitle = lastFmAlbum.getName();
            }

            if (lastFmAlbum.getReleasedate() != null &&
                    lastFmAlbum.getReleasedate().length() >= 4) {

                try {
                    releaseYear = Short.valueOf(
                            lastFmAlbum.getReleasedate().substring(0, 4)
                    );
                } catch (NumberFormatException ignored) {
                }
            }

            if (lastFmAlbum.getImage() != null &&
                    !lastFmAlbum.getImage().isEmpty()) {

                artworkUrl =
                        lastFmAlbum
                                .getImage()
                                .get(lastFmAlbum.getImage().size() - 1)
                                .getText();
            }
        }

        // Find the artist
        // Find the artist, or create it if it doesn't exist
        Artist artistEntity = artistRepository
                .findByNameIgnoreCase(artist)
                .orElseGet(() -> artistRepository.save(new Artist(artist)));

// Store the artist ID with the album
        Album album = new Album(
                artist.hashCode() + "_" + title.hashCode(),
                artistEntity.getId(),
                finalTitle,
                releaseYear,
                artworkUrl
        );

        album = albumRepository.save(album);

        ListItem item =
                new ListItem(
                        id,
                        album.getId(),
                        null,
                        nextPosition
                );

        listItemRepository.save(item);

        return "redirect:/lists/" + id;
    }

    @PostMapping("/{id}/songs")
    public String addSong(Authentication authentication, @PathVariable Long id, @RequestParam Long songId) {
        User user = getCurrentUser(authentication);

        com.example.MusicBoxd.Model.List list =
                listRepository.findById(id).orElseThrow();

        if (!list.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        int nextPosition =
                listItemRepository.findMaxPosition(id) + 1;

        ListItem item =
                new ListItem(
                        id,
                        null,
                        songId,
                        nextPosition
                );

        listItemRepository.save(item);

        return "redirect:/lists/" + id;
    }


    @GetMapping("/placeholder-list-form")
    public String placeholderListForm() {
        return "placeholder-list-form";
    }


    @GetMapping("/placeholder-lists")
    public String placeholderLists() {
        return "redirect:/lists";
    }


    @GetMapping("/album/{id}")
    public String getAlbum(Authentication authentication, @PathVariable Long id, Model model) {
        Album album = albumRepository.findById(id).orElse(null);
        //if album donesnt exists just return lists
        if (album == null) {
            return "redirect:/lists";
        }
        model.addAttribute("album", album);
        // Check if album is already in Want to Listen
        boolean alreadyInWantToListen = false;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = getCurrentUser(authentication);
            Optional<com.example.MusicBoxd.Model.List> wantToListen =
                    listRepository.findByUserIdAndListType(user.getId(), ListType.WANT_TO_LISTEN);
            if (wantToListen.isPresent()) {
                alreadyInWantToListen = listItemRepository.existsByListIdAndAlbumId(wantToListen.get().getId(), album.getId());
            }
        }
        model.addAttribute("alreadyInWantToListen", alreadyInWantToListen);
        LastFmAlbum lastFmAlbum = null;
        if (album.getArtistId() != null) {
            Artist artist = artistRepository.findById(album.getArtistId()).orElse(null);
            if (artist != null) {
                model.addAttribute("artist", artist);
                LastFmAlbumResponse response = lastFmService.getAlbumInfo(artist.getName(), album.getTitle());
                if (response != null) {
                    lastFmAlbum = response.getAlbum();
                }
            }
        }
        model.addAttribute("lastFmAlbum", lastFmAlbum);
        return "album-profile";
    }


    private User getCurrentUser(
            Authentication authentication
    ) {
        if (authentication == null ||
                !(authentication.getPrincipal()
                        instanceof DefaultOidcUser principal)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED
            );
        }

        return userRepository
                .findByOktaUserId(principal.getSubject())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED
                        )
                );
    }

    @PostMapping("/{id}/albums/{albumId}")
    public String addExistingAlbum(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long albumId
    ) {
        User user = getCurrentUser(authentication);

        com.example.MusicBoxd.Model.List list =
                listRepository.findById(id).orElseThrow();

        if (!list.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Album album =
                albumRepository.findById(albumId).orElseThrow();

        boolean alreadyExists =
                listItemRepository.existsByListIdAndAlbumId(
                        id,
                        albumId
                );

        if (!alreadyExists) {
            int nextPosition =
                    listItemRepository.findMaxPosition(id) + 1;

            listItemRepository.save(
                    new ListItem(
                            id,
                            album.getId(),
                            null,
                            nextPosition
                    )
            );
        }

        return "redirect:/albums/" + albumId;
    }

    @PostMapping("/want-to-listen/{albumId}")
    public String addToWantToListen(
            Authentication authentication,
            @PathVariable Long albumId
    ) {
        User user = getCurrentUser(authentication);

        Album album =
                albumRepository.findById(albumId).orElseThrow();

        com.example.MusicBoxd.Model.List wantToListen =
                listRepository
                        .findByUserIdAndListType(
                                user.getId(),
                                ListType.WANT_TO_LISTEN
                        )
                        .orElseGet(() -> {
                            com.example.MusicBoxd.Model.List newList =
                                    new com.example.MusicBoxd.Model.List(
                                            user.getId(),
                                            "Want to Listen",
                                            "Albums I want to listen to",
                                            ListType.WANT_TO_LISTEN
                                    );

                            return listRepository.save(newList);
                        });

        boolean alreadyExists =
                listItemRepository.existsByListIdAndAlbumId(
                        wantToListen.getId(),
                        albumId
                );

        if (!alreadyExists) {
            int nextPosition =
                    listItemRepository.findMaxPosition(
                            wantToListen.getId()
                    ) + 1;

            listItemRepository.save(
                    new ListItem(
                            wantToListen.getId(),
                            album.getId(),
                            null,
                            nextPosition
                    )
            );
        }

        return "redirect:/albums/" + albumId;
    }
}
