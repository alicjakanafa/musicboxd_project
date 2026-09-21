package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Model.UserFavouriteAlbum;
import com.example.MusicBoxd.Model.UserFavouriteArtist;

import com.example.MusicBoxd.Repository.UserFavouriteArtistRepository;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.FriendRepository;
import com.example.MusicBoxd.Repository.ListItemRepository;
import com.example.MusicBoxd.Repository.ListRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import com.example.MusicBoxd.Repository.UserFavouriteAlbumRepository;
import com.example.MusicBoxd.Repository.UserRepository;

import com.example.MusicBoxd.api.lastfm.LastFmAlbum;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import com.example.MusicBoxd.api.spotify.SpotifyTopArtistsResponse;
import com.example.MusicBoxd.api.spotify.SpotifyTopTracksResponse;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
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

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private UserFavouriteAlbumRepository favouriteAlbumRepository;

    @Autowired
    private LastFmService lastFmService;

    @Autowired
    private SpotifyController spotifyController;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private UserFavouriteArtistRepository favouriteArtistRepository;

    @Autowired
    private ListRepository listRepository;

    @Autowired
    private ListItemRepository listItemRepository;


    @GetMapping("/profile/{id}")
    public String profiles(
            @PathVariable Long id,
            Model model,
            HttpSession session,

            @RequestParam(
                    name = "artistRange",
                    defaultValue = "medium_term"
            )
            String artistRange,

            @RequestParam(
                    name = "topType",
                    defaultValue = "artists"
            )
            String topType
    ) {

        User user =
                userRepository
                        .findById(id)
                        .orElse(null);

        if (user == null) {

            System.out.println(
                    "PROFILE ERROR: User not found: " + id
            );

            return "redirect:/";
        }


        System.out.println(
                "LOADING PROFILE FOR USER: " +
                        user.getUsername() +
                        " ID: " +
                        user.getId()
        );


        List<Review> reviews =
                reviewRepository
                        .findByUserIdOrderByCreatedAtDesc(id);

        System.out.println(
                "REVIEWS FOUND: " + reviews.size()
        );


        Map<Long, Album> albums =
                new HashMap<>();

        Map<Long, String> artwork =
                new HashMap<>();


        for (Review review : reviews) {

            System.out.println(
                    "REVIEW ID: " +
                            review.getId() +
                            " USER ID: " +
                            review.getUserId() +
                            " ALBUM ID: " +
                            review.getAlbumId()
            );


            Long albumId =
                    review.getAlbumId();


            if (albumId == null) {

                System.out.println(
                        "REVIEW " +
                                review.getId() +
                                " HAS NO ALBUM ID"
                );

                continue;
            }


            Album album =
                    albumRepository
                            .findById(albumId)
                            .orElse(null);


            if (album == null) {

                System.out.println(
                        "ALBUM NOT FOUND: " +
                                albumId
                );

                continue;
            }


            albums.put(
                    albumId,
                    album
            );


            String artworkUrl =
                    getArtwork(album);


            if (
                    artworkUrl != null &&
                            !artworkUrl.isBlank()
            ) {

                artwork.put(
                        albumId,
                        artworkUrl
                );
            }
        }


        List<UserFavouriteAlbum> favouriteRecords =
                favouriteAlbumRepository
                        .findByUserIdOrderByPositionAsc(id);


        List<Album> favouriteAlbums =
                new ArrayList<>();


        for (
                UserFavouriteAlbum favourite :
                favouriteRecords
        ) {

            albumRepository
                    .findById(
                            favourite.getAlbumId()
                    )
                    .ifPresent(album -> {

                        favouriteAlbums.add(
                                album
                        );


                        String artworkUrl =
                                getArtwork(album);


                        if (
                                artworkUrl != null &&
                                        !artworkUrl.isBlank()
                        ) {

                            artwork.put(
                                    album.getId(),
                                    artworkUrl
                            );
                        }

                    });
        }


        List<UserFavouriteArtist> favouriteArtistRecords =
                favouriteArtistRepository
                        .findByUserIdOrderByIdAsc(id);

        List<Artist> favouriteArtists =
                new ArrayList<>();


        for (
                UserFavouriteArtist favourite :
                favouriteArtistRecords
        ) {

            artistRepository
                    .findById(
                            favourite.getArtistId()
                    )
                    .ifPresent(
                            favouriteArtists::add
                    );
        }


        model.addAttribute(
                "favouriteArtistCount",
                favouriteArtists.size()
        );


        List<com.example.MusicBoxd.Model.List> profileLists =
                listRepository
                        .findByUserIdOrderByCreatedAtDesc(id);


        Map<Long, Integer> profileListItemCounts =
                new HashMap<>();


        for (
                com.example.MusicBoxd.Model.List list :
                profileLists
        ) {

            int itemCount =
                    listItemRepository
                            .findByListIdOrderByPositionAsc(
                                    list.getId()
                            )
                            .size();


            profileListItemCounts.put(
                    list.getId(),
                    itemCount
            );
        }


        model.addAttribute(
                "profileLists",
                profileLists
        );


        model.addAttribute(
                "profileListCount",
                profileLists.size()
        );


        model.addAttribute(
                "profileListItemCounts",
                profileListItemCounts
        );


        System.out.println(
                "PROFILE LISTS FOUND: " +
                        profileLists.size()
        );



        long followingCount = 0;
        long followerCount = 0;

        Iterable<Friend> allFriendships =
                friendRepository.findAll();

        for (Friend friendship : allFriendships) {

            if (friendship.getStatus() == null) {
                continue;
            }

            if (friendship.getStatus().equals("REJECTED")) {
                continue;
            }

            Long requesterId = friendship.getRequesterId();
            Long receiverId = friendship.getReceiverId();

            if (requesterId == null || receiverId == null) {
                continue;
            }


            if (friendship.getStatus().equals("ACCEPTED")) {

                if (requesterId.equals(id) || receiverId.equals(id)) {

                    followerCount++;
                    followingCount++;
                }

                continue;
            }

            if (friendship.getStatus().equals("PENDING")) {

                if (receiverId.equals(id)) {
                    followerCount++;
                }

                if (requesterId.equals(id)) {
                    followingCount++;
                }
            }
        }

        model.addAttribute(
                "followingCount",
                followingCount
        );

        model.addAttribute(
                "followerCount",
                followerCount
        );


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

        model.addAttribute(
                "lastFmArtwork",
                artwork
        );

        model.addAttribute(
                "favouriteAlbums",
                favouriteAlbums
        );


        if (
                !artistRange.equals("short_term")
                        && !artistRange.equals("medium_term")
                        && !artistRange.equals("long_term")
        ) {

            artistRange = "medium_term";
        }


        if (
                !topType.equals("artists")
                        && !topType.equals("tracks")
        ) {

            topType = "artists";
        }


        String spotifyAccessToken =
                (String) session.getAttribute(
                        "spotifyAccessToken"
                );



        model.addAttribute(
                "spotifyConnected",
                false
        );


        model.addAttribute(
                "recentTracksCount",
                0
        );


        model.addAttribute(
                "uniqueTracksCount",
                0
        );


        model.addAttribute(
                "uniqueArtistsCount",
                0
        );


        model.addAttribute(
                "listeningMinutes",
                0
        );


        model.addAttribute(
                "topArtists",
                null
        );


        model.addAttribute(
                "topTracks",
                null
        );


        model.addAttribute(
                "artistRange",
                artistRange
        );


        model.addAttribute(
                "topType",
                topType
        );


        if (
                spotifyAccessToken != null &&
                        !spotifyAccessToken.isBlank()
        ) {

            try {

                System.out.println(
                        "SPOTIFY CONNECTED - " +
                                "LOADING PROFILE DATA"
                );


                spotifyController.getSpotifyData(
                        spotifyAccessToken,
                        model,
                        artistRange
                );


                if (
                        topType.equals("artists")
                ) {

                    System.out.println(
                            "LOADING TOP ARTISTS - " +
                                    artistRange
                    );


                    SpotifyTopArtistsResponse
                            topArtistsResponse =
                            spotifyController
                                    .getTopArtists(
                                            spotifyAccessToken,
                                            artistRange
                                    );


                    if (
                            topArtistsResponse != null &&
                                    topArtistsResponse
                                            .getItems() != null
                    ) {

                        model.addAttribute(
                                "topArtists",
                                topArtistsResponse
                                        .getItems()
                        );
                    }

                }


                else {

                    System.out.println(
                            "LOADING TOP TRACKS - " +
                                    artistRange
                    );


                    SpotifyTopTracksResponse
                            topTracksResponse =
                            spotifyController
                                    .getTopTracks(
                                            spotifyAccessToken,
                                            artistRange
                                    );


                    if (
                            topTracksResponse != null &&
                                    topTracksResponse
                                            .getItems() != null
                    ) {

                        model.addAttribute(
                                "topTracks",
                                topTracksResponse
                                        .getItems()
                        );
                    }

                }


                model.addAttribute(
                        "spotifyConnected",
                        true
                );


                System.out.println(
                        "SPOTIFY PROFILE DATA LOADED"
                );


                System.out.println(
                        "TOP TYPE: " +
                                topType
                );


                System.out.println(
                        "TOP RANGE: " +
                                artistRange
                );


            } catch (Exception e) {

                System.out.println(
                        "SPOTIFY PROFILE ERROR: " +
                                e.getMessage()
                );


                e.printStackTrace();


                model.addAttribute(
                        "spotifyConnected",
                        false
                );

            }

        }


        else {

            System.out.println(
                    "SPOTIFY NOT CONNECTED"
            );
        }



        List<Friend> acceptedFriendships =
                friendRepository
                        .findByRequesterIdAndStatusOrReceiverIdAndStatus(
                                id,
                                "ACCEPTED",
                                id,
                                "ACCEPTED"
                        );


        List<User> followingUsers =
                new ArrayList<>();


        for (
                Friend friendship :
                acceptedFriendships
        ) {

            Long friendId;


            if (
                    friendship
                            .getRequesterId()
                            .equals(id)
            ) {

                friendId =
                        friendship
                                .getReceiverId();

            }

            else {

                friendId =
                        friendship
                                .getRequesterId();
            }


            userRepository
                    .findById(friendId)
                    .ifPresent(
                            followingUsers::add
                    );
        }


        model.addAttribute(
                "followingUsers",
                followingUsers
        );



        System.out.println(
                "FOLLOWING COUNT: " +
                        followingCount
        );


        System.out.println(
                "FOLLOWER COUNT: " +
                        followerCount
        );


        System.out.println(
                "PROFILE REVIEWS SENT TO THYMELEAF: " +
                        reviews.size()
        );


        System.out.println(
                "PROFILE ARTWORK FOUND: " +
                        artwork.size()
        );


        System.out.println(
                "PROFILE TOP 4: " +
                        favouriteAlbums.size()
        );


        System.out.println(
                "PROFILE LIST COUNT: " +
                        profileLists.size()
        );


        return "profile-page";
    }



    @GetMapping("/placeholder-list-form")
    public String placeholderListForm() {

        return "placeholder-list-form";
    }


    private String getArtwork(
            Album album
    ) {

        if (
                album.getArtworkUrl() != null &&
                        !album.getArtworkUrl().isBlank()
        ) {

            System.out.println(
                    "USING DATABASE ARTWORK FOR: " +
                            album.getTitle()
            );


            return album.getArtworkUrl();
        }


        if (
                album.getArtistId() == null
        ) {

            System.out.println(
                    "NO ARTIST ID FOR: " +
                            album.getTitle()
            );


            return null;
        }

        Artist artist =
                artistRepository
                        .findById(
                                album.getArtistId()
                        )
                        .orElse(null);


        if (artist == null) {

            System.out.println(
                    "ARTIST NOT FOUND FOR: " +
                            album.getTitle()
            );


            return null;
        }


        try {

            System.out.println(
                    "GETTING LAST.FM ARTWORK FOR: " +
                            artist.getName() +
                            " - " +
                            album.getTitle()
            );


            var response =
                    lastFmService.getAlbumInfo(
                            artist.getName(),
                            album.getTitle()
                    );


            if (
                    response == null ||
                            response.getAlbum() == null
            ) {

                System.out.println(
                        "LAST.FM RETURNED NO ALBUM FOR: " +
                                album.getTitle()
                );


                return null;
            }


            LastFmAlbum lastFmAlbum =
                    response.getAlbum();


            if (
                    lastFmAlbum.getImage() == null ||
                            lastFmAlbum.getImage().isEmpty()
            ) {

                System.out.println(
                        "LAST.FM RETURNED NO IMAGES FOR: " +
                                album.getTitle()
                );


                return null;
            }


            for (
                    int i =
                    lastFmAlbum
                            .getImage()
                            .size() - 1;

                    i >= 0;

                    i--
            ) {

                String imageUrl =
                        lastFmAlbum
                                .getImage()
                                .get(i)
                                .getText();


                if (
                        imageUrl != null &&
                                !imageUrl.isBlank()
                ) {

                    System.out.println(
                            "LAST.FM ARTWORK FOUND: " +
                                    imageUrl
                    );


                    album.setArtworkUrl(
                            imageUrl
                    );


                    albumRepository.save(
                            album
                    );


                    return imageUrl;
                }
            }

        }

        catch (Exception e) {

            System.out.println(
                    "LAST.FM ARTWORK ERROR FOR " +
                            album.getTitle() +
                            ": " +
                            e.getMessage()
            );
        }


        return null;
    }


    @GetMapping("/profile/{id}/favourite-artists")
    public String favouriteArtists(
            @PathVariable Long id,
            Model model
    ) {

        User user =
                userRepository
                        .findById(id)
                        .orElse(null);


        if (user == null) {

            return "redirect:/";
        }


        List<UserFavouriteArtist> favouriteRecords =
                favouriteArtistRepository
                        .findByUserIdOrderByIdAsc(id);


        List<Artist> favouriteArtists =
                new ArrayList<>();


        for (
                UserFavouriteArtist favourite :
                favouriteRecords
        ) {

            artistRepository
                    .findById(
                            favourite.getArtistId()
                    )
                    .ifPresent(
                            favouriteArtists::add
                    );
        }


        model.addAttribute(
                "user",
                user
        );


        model.addAttribute(
                "favouriteArtists",
                favouriteArtists
        );


        model.addAttribute(
                "favouriteArtistCount",
                favouriteArtists.size()
        );


        return "favourite-artists";
    }

}
