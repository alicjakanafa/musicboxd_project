package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Like;
import com.example.MusicBoxd.Model.ListType;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Model.UserFavouriteAlbum;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.LikeRepository;
import com.example.MusicBoxd.Repository.ListItemRepository;
import com.example.MusicBoxd.Repository.ListRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import com.example.MusicBoxd.Repository.UserFavouriteAlbumRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.itunes.ItunesTrackResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import com.example.MusicBoxd.service.NotificationService;

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
    private final LikeRepository likeRepository;
    private final NotificationService notificationService;

    private final ListRepository listRepository;
    private final ListItemRepository listItemRepository;

    public AlbumController(
            AlbumRepository albumRepository,
            ArtistRepository artistRepository,
            UserFavouriteAlbumRepository favouriteAlbumRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository,
            LikeRepository likeRepository,
            NotificationService notificationService,
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
        this.likeRepository = likeRepository;
        this.notificationService = notificationService;
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

        User currentUser = null;

        if (
                authentication != null &&
                        authentication.getPrincipal() instanceof OidcUser principal
        ) {

            currentUser =
                    userRepository
                            .findByOktaUserId(
                                    principal.getSubject()
                            )
                            .orElse(null);

            if (currentUser != null) {

                List<com.example.MusicBoxd.Model.List> userLists =
                        listRepository
                                .findByUserIdOrderByCreatedAtDesc(
                                        currentUser.getId()
                                );

                model.addAttribute(
                        "userLists",
                        userLists
                );

                listRepository
                        .findByUserIdAndListType(
                                currentUser.getId(),
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
            }
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

        Map<Long, Long> reviewLikeCounts =
                new HashMap<>();

        Map<Long, Boolean> reviewLikedByCurrentUser =
                new HashMap<>();

        for (Review review : reviews) {

            reviewLikeCounts.put(
                    review.getId(),
                    likeRepository.countByReviewId(
                            review.getId()
                    )
            );

            if (currentUser != null) {

                boolean liked =
                        likeRepository
                                .findByUserIdAndReviewId(
                                        currentUser.getId(),
                                        review.getId()
                                )
                                .isPresent();

                reviewLikedByCurrentUser.put(
                        review.getId(),
                        liked
                );
            }
        }

        model.addAttribute(
                "reviewLikeCounts",
                reviewLikeCounts
        );

        model.addAttribute(
                "reviewLikedByCurrentUser",
                reviewLikedByCurrentUser
        );

        model.addAttribute(
                "currentUser",
                currentUser
        );

        return "album-profile";
    }

    @PostMapping("/reviews/{reviewId}/like")
    public String toggleReviewLike(
            @PathVariable Long reviewId,
            Authentication authentication
    ) {

        User currentUser =
                getCurrentUser(authentication);

        Review review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Review not found"
                                )
                        );

        var existingLike =
                likeRepository.findByUserIdAndReviewId(
                        currentUser.getId(),
                        reviewId
                );

        if (existingLike.isPresent()) {

            likeRepository.delete(
                    existingLike.get()
            );

        } else {

            Like like =
                    new Like(
                            currentUser.getId(),
                            reviewId,
                            null
                    );

            likeRepository.save(
                    like
            );

            if (
                    !currentUser.getId().equals(
                            review.getUserId()
                    )
            ) {

                notificationService.notifyReviewLiked(
                        review.getUserId(),
                        currentUser.getId(),
                        reviewId,
                        currentUser.getUsername()
                );
            }
        }

        return "redirect:/albums/" +
                review.getAlbumId();
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

        if (
                authentication == null ||
                        !authentication.isAuthenticated() ||
                        !(authentication.getPrincipal() instanceof OidcUser principal)
        ) {
            throw new RuntimeException(
                    "User is not authenticated"
            );
        }

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