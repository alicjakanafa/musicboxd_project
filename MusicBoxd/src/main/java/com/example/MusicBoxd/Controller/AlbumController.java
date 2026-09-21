package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.ListType;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Model.UserFavouriteAlbum;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.ListItemRepository;
import com.example.MusicBoxd.Repository.ListRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import com.example.MusicBoxd.Repository.UserFavouriteAlbumRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.itunes.ItunesTrackResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/albums")
public class AlbumController {

    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final UserFavouriteAlbumRepository favouriteAlbumRepository;
    private final UserRepository userRepository;
    private final LastFmService lastFmService;
    private final ItunesService itunesService;
    private final ReviewRepository reviewRepository;

    private final ListRepository listRepository;
    private final ListItemRepository listItemRepository;

    public AlbumController(
            AlbumRepository albumRepository,
            ArtistRepository artistRepository,
            UserFavouriteAlbumRepository favouriteAlbumRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository,
            LastFmService lastFmService,
            ItunesService itunesService,
            ListRepository listRepository,
            ListItemRepository listItemRepository
    ) {
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.favouriteAlbumRepository = favouriteAlbumRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.lastFmService = lastFmService;
        this.itunesService = itunesService;
        this.listRepository = listRepository;
        this.listItemRepository = listItemRepository;
    }


    @GetMapping("/save")
    public String saveItunesAlbum(
            @RequestParam Long collectionId,
            @RequestParam String artistName,
            @RequestParam String albumName,
            @RequestParam String artworkUrl,
            @RequestParam String releaseDate
    ) {

        String externalId =
                collectionId.toString();

        var existingAlbum =
                albumRepository
                        .findByExternalId(externalId);

        if (existingAlbum.isPresent()) {

            return "redirect:/albums/" +
                    existingAlbum.get().getId();
        }

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

        Short releaseYear = null;

        if (
                releaseDate != null &&
                        !releaseDate.isBlank()
        ) {

            try {

                releaseYear =
                        Short.valueOf(
                                releaseDate.substring(
                                        0,
                                        4
                                )
                        );

            } catch (Exception ignored) {
            }
        }

        Album album =
                new Album(
                        externalId,
                        artist.getId(),
                        albumName,
                        releaseYear,
                        artworkUrl
                );

        Album savedAlbum =
                albumRepository.save(album);

        return "redirect:/albums/" +
                savedAlbum.getId();
    }


    @GetMapping("/{id}")
    public String showAlbum(
            @PathVariable Long id,
            Model model,
            Authentication authentication
    ) {

        var album =
                albumRepository.findById(id);

        if (album.isEmpty()) {
            return "redirect:/";
        }

        Album currentAlbum =
                album.get();

        if (currentAlbum.getArtistId() == null) {
            return "redirect:/";
        }

        var artist =
                artistRepository.findById(
                        currentAlbum.getArtistId()
                );

        if (artist.isEmpty()) {
            return "redirect:/";
        }

        Artist currentArtist =
                artist.get();


        var lastFmAlbumResponse =
                lastFmService.getAlbumInfo(
                        currentArtist.getName(),
                        currentAlbum.getTitle()
                );

        model.addAttribute(
                "album",
                currentAlbum
        );

        model.addAttribute(
                "artist",
                currentArtist
        );

        if (
                lastFmAlbumResponse != null &&
                        lastFmAlbumResponse.getAlbum() != null
        ) {

            model.addAttribute(
                    "lastFmAlbum",
                    lastFmAlbumResponse.getAlbum()
            );

        } else {

            model.addAttribute(
                    "lastFmAlbum",
                    null
            );
        }

        model.addAttribute(
                "alreadyInWantToListen",
                false
        );

        if (
                authentication != null &&
                        authentication.getPrincipal() instanceof OidcUser principal
        ) {

            userRepository
                    .findByOktaUserId(principal.getSubject())
                    .ifPresent(user -> {

                        List<com.example.MusicBoxd.Model.List> userLists =
                                listRepository
                                        .findByUserIdOrderByCreatedAtDesc(
                                                user.getId()
                                        );

                        model.addAttribute(
                                "userLists",
                                userLists
                        );

                        listRepository
                                .findByUserIdAndListType(
                                        user.getId(),
                                        ListType.WANT_TO_LISTEN
                                )
                                .ifPresent(wantToListen -> {

                                    boolean alreadyInWantToListen =
                                            listItemRepository
                                                    .findByListIdAndAlbumId(
                                                            wantToListen.getId(),
                                                            currentAlbum.getId()
                                                    )
                                                    .isPresent();

                                    model.addAttribute(
                                            "alreadyInWantToListen",
                                            alreadyInWantToListen
                                    );
                                });
                    });
        }

        Map<String, String> trackPreviews =
                new HashMap<>();

        if (
                currentAlbum.getExternalId() != null &&
                        !currentAlbum.getExternalId().isBlank()
        ) {

            try {

                Long collectionId =
                        Long.valueOf(
                                currentAlbum.getExternalId()
                        );

                ItunesTrackResponse response =
                        itunesService.getAlbumTracks(
                                collectionId
                        );

                if (
                        response != null &&
                                response.getResults() != null
                ) {

                    response.getResults()
                            .forEach(track -> {

                                if (
                                        track.getTrackName() != null &&
                                                track.getPreviewUrl() != null
                                ) {

                                    trackPreviews.put(
                                            track.getTrackName(),
                                            track.getPreviewUrl()
                                    );
                                }

                            });
                }

            } catch (NumberFormatException e) {

                System.out.println(
                        "INVALID ITUNES COLLECTION ID FOR ALBUM: " +
                                currentAlbum.getTitle()
                );

            } catch (Exception e) {

                System.out.println(
                        "ITUNES TRACK ERROR FOR ALBUM: " +
                                currentAlbum.getTitle() +
                                " - " +
                                e.getMessage()
                );
            }
        }

        model.addAttribute(
                "trackPreviews",
                trackPreviews
        );


        List<Review> reviews =
                reviewRepository
                        .findByAlbumIdOrderByCreatedAtDesc(
                                currentAlbum.getId()
                        );

        model.addAttribute(
                "reviews",
                reviews
        );


        List<Long> reviewUserIds =
                reviews.stream()
                        .map(Review::getUserId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();

        Map<Long, User> reviewUsers =
                new HashMap<>();

        if (!reviewUserIds.isEmpty()) {

            userRepository
                    .findByIdIn(reviewUserIds)
                    .forEach(user ->
                            reviewUsers.put(
                                    user.getId(),
                                    user
                            )
                    );
        }

        model.addAttribute(
                "reviewUsers",
                reviewUsers
        );

        return "album-profile";
    }

    @PostMapping("/{id}/favourite")
    public String addFavouriteAlbum(
            @PathVariable Long id,
            @RequestParam Integer position,
            Authentication authentication
    ) {

        User user =
                getCurrentUser(authentication);

        favouriteAlbumRepository
                .findByUserIdAndAlbumId(
                        user.getId(),
                        id
                )
                .ifPresent(
                        favouriteAlbumRepository::delete
                );

        favouriteAlbumRepository
                .findByUserIdOrderByPositionAsc(
                        user.getId()
                )
                .stream()
                .filter(favourite ->
                        favourite.getPosition()
                                .equals(position)
                )
                .findFirst()
                .ifPresent(
                        favouriteAlbumRepository::delete
                );

        UserFavouriteAlbum favourite =
                new UserFavouriteAlbum(
                        user.getId(),
                        id,
                        position
                );

        favouriteAlbumRepository.save(
                favourite
        );

        return "redirect:/profile/" +
                user.getId();
    }

    @PostMapping("/{id}/favourite/remove")
    public String removeFavouriteAlbum(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User user =
                getCurrentUser(authentication);

        favouriteAlbumRepository
                .findByUserIdAndAlbumId(
                        user.getId(),
                        id
                )
                .ifPresent(
                        favouriteAlbumRepository::delete
                );

        return "redirect:/profile/" +
                user.getId();
    }

    private User getCurrentUser(
            Authentication authentication
    ) {

        OidcUser principal =
                (OidcUser)
                        authentication.getPrincipal();

        String oktaUserId =
                principal.getSubject();

        return userRepository
                .findByOktaUserId(oktaUserId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Current user not found"
                        )
                );
    }
}