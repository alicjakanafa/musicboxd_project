package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @GetMapping("/profile/{id}")
    public String profiles(
            @PathVariable Long id,
            Model model
    ) {

        User user = new User();
        user.setUsername("charlie jackson");
        user.setCreatedAt(LocalDateTime.now());

        List<Review> reviews = reviewRepository.findByUserId(id);

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

        model.addAttribute("user", user);
        model.addAttribute("reviews", reviews);
        model.addAttribute("albums", albums);

        return "profile-page";
    }
}