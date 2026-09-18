package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.lastfm.LastFmService;
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
    private LastFmService lastFmService;



    @GetMapping("/users/{userId}/reviews")
    public String getUserReviews(
            @PathVariable Long userId,
            Model model
    ) {

        List<Review> reviews =
                reviewRepository
                        .findByUserIdOrderByCreatedAtDesc(userId);

        Map<Long, Album> albums =
                new HashMap<>();

        for (Review review : reviews) {

            Long albumId =
                    review.getAlbumId();

            if (albumId == null) {
                continue;
            }

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

        model.addAttribute(
                "reviews",
                reviews
        );

        model.addAttribute(
                "albums",
                albums
        );

        return "profile-page";
    }


    // =========================
    // SHOW REVIEW PAGE
    // =========================

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

                var lastFmAlbumResponse =
                        lastFmService.getAlbumInfo(
                                artist.getName(),
                                album.getTitle()
                        );

                if (lastFmAlbumResponse != null) {

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

            } catch (Exception e) {

                System.out.println(
                        "LAST.FM REVIEW PAGE ERROR: " +
                                e.getMessage()
                );

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
            @RequestParam(required = false) String header,
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

        System.out.println(
                "AUTHENTICATED OKTA USER ID: " +
                        oktaUserId
        );



        User user =
                userRepository
                        .findByOktaUserId(oktaUserId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found for Okta ID: " +
                                                oktaUserId
                                )
                        );


        System.out.println(
                "REVIEW USER ID: " +
                        user.getId()
        );

        System.out.println(
                "REVIEW USERNAME: " +
                        user.getUsername()
        );

        System.out.println(
                "REVIEW ALBUM ID: " +
                        album.getId()
        );


        Review review =
                new Review();

        review.setUserId(
                user.getId()
        );

        review.setAlbumId(
                album.getId()
        );

        review.setRating(
                rating
        );

        review.setContent(
                content
        );

        review.setHeader(
                header
        );

        review.setCreatedAt(
                LocalDateTime.now()
        );



        Review savedReview =
                reviewRepository.save(
                        review
                );


        System.out.println(
                "=============================="
        );

        System.out.println(
                "REVIEW SAVED SUCCESSFULLY"
        );

        System.out.println(
                "Review ID: " +
                        savedReview.getId()
        );

        System.out.println(
                "User ID: " +
                        savedReview.getUserId()
        );

        System.out.println(
                "Album ID: " +
                        savedReview.getAlbumId()
        );

        System.out.println(
                "Rating: " +
                        savedReview.getRating()
        );

        System.out.println(
                "=============================="
        );



        return "redirect:/albums/" + id;
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
                                        "User not found for Okta ID: " +
                                                oktaUserId
                                )
                        );

        Review review =
                reviewRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Review not found: " + id
                                )
                        );

        if (!review.getUserId().equals(user.getId())) {
            throw new RuntimeException(
                    "You cannot delete another user's review"
            );
        }

        reviewRepository.delete(review);

        System.out.println("==============================");
        System.out.println("REVIEW DELETED");
        System.out.println("Review ID: " + review.getId());
        System.out.println("User ID: " + review.getUserId());
        System.out.println("Album ID: " + review.getAlbumId());
        System.out.println("==============================");

        return "redirect:/profile/" + user.getId();
    }
}
