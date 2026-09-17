package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
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


    @GetMapping("/users/{userId}/reviews")
    public String getUserReviews(
            @PathVariable Long userId,
            Model model
    ) {

        List<Review> reviews =
                reviewRepository.findByUserId(userId);

        Map<Long, Album> albums = new HashMap<>();

        for (Review review : reviews) {

            Long albumId = review.getAlbumId();

            if (albumId != null) {

                Album album = albumRepository
                        .findById(albumId)
                        .orElse(null);

                albums.put(albumId, album);
            }
        }

        model.addAttribute("reviews", reviews);
        model.addAttribute("albums", albums);

        return "profile-page";
    }


    @GetMapping("/reviews/{id}")
    public String reviewAlbum(
            @PathVariable Long id,
            Model model
    ) {

        Album album = albumRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("album not found")
                );


        model.addAttribute("album", album);



        if (album.getArtistId() != null) {

            Artist artist = artistRepository
                    .findById(album.getArtistId())
                    .orElse(null);

            model.addAttribute("artist", artist);
        }


        return "reviews";
    }


    @PostMapping("/reviews/{id}")
    public String saveReview(
            @PathVariable Long id,
            @RequestParam BigDecimal rating,
            @RequestParam String content
    ) {

        albumRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("album not found")
                );

        Review review = new Review();


        review.setUserId(1L);

        review.setAlbumId(id);
        review.setRating(rating);
        review.setContent(content);
        review.setCreatedAt(LocalDateTime.now());

        reviewRepository.save(review);

        return "redirect:/profile/1";
    }

}