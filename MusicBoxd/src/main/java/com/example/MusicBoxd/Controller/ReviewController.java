package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Model.Like;
import com.example.MusicBoxd.Repository.LikeRepository;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.FriendRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import com.example.MusicBoxd.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LastFmService lastFmService;

    @Autowired
    private LikeRepository likeRepository;


    @GetMapping("/users/{userId}/reviews")
    public String getUserReviews(
            @PathVariable Long userId,
            Model model
    ) {

        // i had to add a user repo thing here for it to actually list reviews on a user-by-user basis
        User user =
        userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found: " + userId
                        )
                );

        List<Review> reviews =
                reviewRepository.findByUserId(userId);

        Map<Long, Album> albums =
                new HashMap<>();

        for (Review review : reviews) {

            Long albumId =
                    review.getAlbumId();

            if (albumId != null) {

                Album album =
                        albumRepository
                                .findById(albumId)
                                .orElse(null);

                if (album != null) {

                    albums.put(
                            albumId,
                            album
                    );
                }
            }
        }

        // also had to add a model attribute here for it to find the right user!!!
        model.addAttribute(
        "user",
        user
);

        model.addAttribute(
                "reviews",
                reviews
        );

        model.addAttribute(
                "albums",
                albums
        );

        // changed profile-page here to the new page i made :)
        return "user-reviews";
    }


    @GetMapping("/reviews/{id}")
    public String reviewAlbum(
            @PathVariable Long id,
            Model model
    ) {

        Album album =
                albumRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Album not found"
                                )
                        );

        model.addAttribute(
                "album",
                album
        );

        Artist artist = null;

        if (album.getArtistId() != null) {

            artist =
                    artistRepository
                            .findById(
                                    album.getArtistId()
                            )
                            .orElse(null);
        }

        model.addAttribute(
                "artist",
                artist
        );

        if (artist != null) {

            try {

                var lastFmAlbum =
                        lastFmService.getAlbumInfo(
                                artist.getName(),
                                album.getTitle()
                        );

                model.addAttribute(
                        "lastFmAlbum",
                        lastFmAlbum
                );

            } catch (Exception e) {

                model.addAttribute(
                        "lastFmAlbum",
                        null
                );
            }

        } else {

            model.addAttribute(
                    "lastFmAlbum",
                    null
            );
        }

        return "reviews";
    }


    @PostMapping("/reviews/{id}")
    public String saveReview(
            @PathVariable Long id,
            @RequestParam BigDecimal rating,
            @RequestParam String content,
            Authentication authentication
    ) {


        Album album =
                albumRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Album not found"
                                )
                        );



        OidcUser principal =
                (OidcUser) authentication.getPrincipal();

        String oktaUserId =
                principal.getSubject();

        User user =
                userRepository
                        .findByOktaUserId(oktaUserId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found for Okta ID: "
                                                + oktaUserId
                                )
                        );



        Review review =
                reviewRepository
                        .findByUserIdAndAlbumId(
                                user.getId(),
                                album.getId()
                        )
                        .orElse(null);



        boolean isNewReview =
                review == null;


        if (isNewReview) {

            review =
                    new Review();

            review.setUserId(
                    user.getId()
            );

            review.setAlbumId(
                    album.getId()
            );

            review.setCreatedAt(
                    LocalDateTime.now()
            );
        }


        review.setRating(
                rating
        );

        review.setContent(
                content
        );



        Review savedReview =
                reviewRepository.save(
                        review
                );


        if (isNewReview) {

            List<Friend> friendships =
                    friendRepository
                            .findByStatus("ACCEPTED");

            for (
                    Friend friendship :
                    friendships
            ) {

                Long friendId = null;


                // Current user sent the friend request
                if (
                        user.getId().equals(
                                friendship.getRequesterId()
                        )
                ) {

                    friendId =
                            friendship.getReceiverId();
                }

                else if (
                        user.getId().equals(
                                friendship.getReceiverId()
                        )
                ) {

                    friendId =
                            friendship.getRequesterId();
                }


                if (friendId == null) {
                    continue;
                }


                notificationService.notifyFriendReviewed(
                        friendId,
                        user.getId(),
                        savedReview.getId(),
                        user.getUsername()
                );
            }
        }


        return "redirect:/profile/" + user.getId();
    }



    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(
            @PathVariable Long id,
            Authentication authentication
    ) {


        OidcUser principal =
                (OidcUser) authentication.getPrincipal();

        String oktaUserId =
                principal.getSubject();


        User user =
                userRepository
                        .findByOktaUserId(oktaUserId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found for Okta ID: "
                                                + oktaUserId
                                )
                        );



        Review review =
                reviewRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Review not found"
                                )
                        );


        if (
                !user.getId().equals(
                        review.getUserId()
                )
        ) {

            throw new RuntimeException(
                    "You cannot delete another user's review"
            );
        }


        reviewRepository.delete(
                review
        );


        return "redirect:/profile/" + user.getId();
    }

    @GetMapping("/reviews")
    public String getAllReviews(Model model) {

        List<Review> reviews =
                reviewRepository.findAllByOrderByCreatedAtDesc();

        Map<Long, Album> albums =
                new HashMap<>();

        Map<Long, User> users =
                new HashMap<>();

        for (Review review : reviews) {

            Long albumId =
                    review.getAlbumId();

            if (albumId != null) {

                Album album =
                        albumRepository
                                .findById(albumId)
                                .orElse(null);

                if (album != null) {
                    albums.put(
                            albumId,
                            album
                    );
                }
            }


            Long userId =
                    review.getUserId();

            if (userId != null) {

                User user =
                        userRepository
                                .findById(userId)
                                .orElse(null);

                if (user != null) {
                    users.put(
                            userId,
                            user
                    );
                }
            }
        }

        model.addAttribute(
                "reviews",
                reviews
        );

        model.addAttribute(
                "albums",
                albums
        );

        model.addAttribute(
                "users",
                users
        );

        return "all-reviews";
    }

    @PostMapping("/reviews/{reviewId}/like")
    public String likeReview(
            @PathVariable Long reviewId,
            Authentication authentication
    ) {
        OidcUser principal =
                (OidcUser) authentication.getPrincipal();

        String oktaUserId =
                principal.getSubject();

        User user =
                userRepository
                        .findByOktaUserId(oktaUserId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        Review review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Review not found"
                                )
                        );

        Long userId = user.getId();

        Optional<Like> existingLike =
                likeRepository.findByUserIdAndReviewId(
                        userId,
                        reviewId
                );

        if (existingLike.isPresent()) {

            likeRepository.delete(
                    existingLike.get()
            );

        } else {

            Like like =
                    new Like(
                            userId,
                            reviewId,
                            null
                    );

            likeRepository.save(like);
        }

        return "redirect:/albums/" + review.getAlbumId();
    }
}

