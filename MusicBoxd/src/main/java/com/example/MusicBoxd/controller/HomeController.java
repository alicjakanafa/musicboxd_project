package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.Notification;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.FriendRepository;
import com.example.MusicBoxd.Repository.NotificationRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.itunes.ItunesAlbum;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.lastfm.LastFmResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final LastFmService lastFmService;
    private final SpotifyController spotifyController;
    private final ItunesService itunesService;

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FriendRepository friendRepository;
    private final ReviewRepository reviewRepository;
    private final AlbumRepository albumRepository;

    public HomeController(
            LastFmService lastFmService,
            SpotifyController spotifyController,
            ItunesService itunesService,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            FriendRepository friendRepository,
            ReviewRepository reviewRepository,
            AlbumRepository albumRepository
    ) {
        this.lastFmService = lastFmService;
        this.spotifyController = spotifyController;
        this.itunesService = itunesService;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.friendRepository = friendRepository;
        this.reviewRepository = reviewRepository;
        this.albumRepository = albumRepository;
    }

    @GetMapping("/")
    public String index(
            Model model,
            HttpSession session,
            Authentication authentication
    ) {
        LastFmResponse response =
                lastFmService.getTopArtists();

        model.addAttribute(
                "topArtists",
                response.getArtists()
                        .getArtist()
                        .subList(0, 10)
        );

        User currentUser =
                getCurrentUser(authentication);

        Long userId = null;

        if (currentUser != null) {
            userId = currentUser.getId();
        }

        ItunesAlbum suggestedAlbum =
                itunesService.getDailyAlbum(userId);

        model.addAttribute(
                "suggestedAlbum",
                suggestedAlbum
        );

        if (currentUser != null) {

            List<Notification> notifications =
                    notificationRepository
                            .findTop6ByUserIdOrderByCreatedAtDesc(
                                    currentUser.getId()
                            );

            long unreadNotificationCount =
                    notificationRepository
                            .countByUserIdAndReadFalse(
                                    currentUser.getId()
                            );

            model.addAttribute(
                    "notifications",
                    notifications
            );

            model.addAttribute(
                    "unreadNotificationCount",
                    unreadNotificationCount
            );

            model.addAttribute(
                    "currentUser",
                    currentUser
            );
        }

        if (currentUser != null) {

            loadFriendsActivity(
                    currentUser,
                    model
            );

        } else {

            model.addAttribute(
                    "friendReviews",
                    List.of()
            );

            model.addAttribute(
                    "friendReviewUsers",
                    Map.of()
            );

            model.addAttribute(
                    "friendReviewAlbums",
                    Map.of()
            );

            model.addAttribute(
                    "friendReviewArtwork",
                    Map.of()
            );
        }

        String spotifyAccessToken =
                (String) session.getAttribute(
                        "spotifyAccessToken"
                );

        if (spotifyAccessToken != null) {

            spotifyController.getSpotifyData(
                    spotifyAccessToken,
                    model
            );
        }

        return "index";
    }

    private User getCurrentUser(
            Authentication authentication
    ) {
        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {
            return null;
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal instanceof OidcUser oidcUser)) {
            return null;
        }

        String oktaUserId =
                oidcUser.getSubject();

        return userRepository
                .findByOktaUserId(oktaUserId)
                .orElse(null);
    }

    private void loadFriendsActivity(
            User currentUser,
            Model model
    ) {
        List<Friend> friendships =
                friendRepository.findByStatus(
                        "ACCEPTED"
                );

        List<Long> friendIds =
                new ArrayList<>();

        for (Friend friendship : friendships) {

            if (
                    currentUser.getId().equals(
                            friendship.getRequesterId()
                    )
            ) {

                friendIds.add(
                        friendship.getReceiverId()
                );

            } else if (
                    currentUser.getId().equals(
                            friendship.getReceiverId()
                    )
            ) {

                friendIds.add(
                        friendship.getRequesterId()
                );
            }
        }

        List<Review> friendReviews =
                new ArrayList<>();

        for (Long friendId : friendIds) {

            List<Review> reviews =
                    reviewRepository
                            .findByUserIdOrderByCreatedAtDesc(
                                    friendId
                            );

            friendReviews.addAll(
                    reviews
            );
        }

        friendReviews.sort(
                Comparator.comparing(
                        Review::getCreatedAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        if (friendReviews.size() > 10) {

            friendReviews =
                    new ArrayList<>(
                            friendReviews.subList(
                                    0,
                                    10
                            )
                    );
        }

        Map<Long, User> friendReviewUsers =
                new HashMap<>();

        Map<Long, Album> friendReviewAlbums =
                new HashMap<>();

        Map<Long, String> friendReviewArtwork =
                new HashMap<>();

        for (Review review : friendReviews) {

            User reviewer =
                    userRepository
                            .findById(
                                    review.getUserId()
                            )
                            .orElse(null);

            if (reviewer != null) {

                friendReviewUsers.put(
                        review.getUserId(),
                        reviewer
                );
            }

            if (review.getAlbumId() == null) {
                continue;
            }

            Album album =
                    albumRepository
                            .findById(
                                    review.getAlbumId()
                            )
                            .orElse(null);

            if (album == null) {
                continue;
            }

            friendReviewAlbums.put(
                    review.getAlbumId(),
                    album
            );

            if (
                    album.getArtworkUrl() != null
                            && !album.getArtworkUrl().isEmpty()
            ) {

                friendReviewArtwork.put(
                        review.getAlbumId(),
                        album.getArtworkUrl()
                );
            }
        }

        model.addAttribute(
                "friendReviews",
                friendReviews
        );

        model.addAttribute(
                "friendReviewUsers",
                friendReviewUsers
        );

        model.addAttribute(
                "friendReviewAlbums",
                friendReviewAlbums
        );

        model.addAttribute(
                "friendReviewArtwork",
                friendReviewArtwork
        );
    }

    @GetMapping("/profile")
    public String profile(
            Authentication authentication
    ) {
        User user =
                getCurrentUser(authentication);

        if (user == null) {
            throw new RuntimeException(
                    "User not found"
            );
        }

        return "redirect:/profile/"
                + user.getId();
    }
}