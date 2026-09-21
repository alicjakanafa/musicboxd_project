package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.api.spotify.SpotifyCurrentlyPlayingResponse;
import com.example.MusicBoxd.api.spotify.SpotifyRecentlyPlayedResponse;
import com.example.MusicBoxd.api.spotify.SpotifyTokenResponse;
import com.example.MusicBoxd.api.spotify.SpotifyTopArtistsResponse;
import com.example.MusicBoxd.api.spotify.SpotifyArtist;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Controller
public class SpotifyController {

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.redirect.uri}")
    private String redirectUri;

    @GetMapping("/spotify/login")
    public String spotifyLogin() {

        String spotifyUrl = UriComponentsBuilder
                .fromUriString(
                        "https://accounts.spotify.com/authorize"
                )
                .queryParam(
                        "client_id",
                        clientId
                )
                .queryParam(
                        "response_type",
                        "code"
                )
                .queryParam(
                        "redirect_uri",
                        redirectUri
                )
                .queryParam(
                        "scope",
                        "user-read-recently-played " +
                                "user-read-currently-playing " +
                                "user-read-playback-state " +
                                "user-modify-playback-state " +
                                "streaming " +
                                "user-top-read"
                )
                .build()
                .toUriString();

        return "redirect:" + spotifyUrl;
    }

    @GetMapping("/spotify/callback")
    public String spotifyCallback(
            String code,
            HttpSession session
    ) {

        RestTemplate restTemplate =
                new RestTemplate();

        MultiValueMap<String, String> body =
                new LinkedMultiValueMap<>();

        body.add(
                "grant_type",
                "authorization_code"
        );

        body.add(
                "code",
                code
        );

        body.add(
                "redirect_uri",
                redirectUri
        );

        String credentials =
                clientId + ":" + clientSecret;

        String encodedCredentials =
                Base64.getEncoder()
                        .encodeToString(
                                credentials.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

        HttpHeaders headers =
                new HttpHeaders();

        headers.set(
                "Authorization",
                "Basic " + encodedCredentials
        );

        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        ResponseEntity<SpotifyTokenResponse> tokenResponse =
                restTemplate.postForEntity(
                        "https://accounts.spotify.com/api/token",
                        request,
                        SpotifyTokenResponse.class
                );

        SpotifyTokenResponse token =
                tokenResponse.getBody();

        if (token == null) {
            throw new IllegalStateException(
                    "Spotify did not return a token"
            );
        }

        session.setAttribute(
                "spotifyAccessToken",
                token.getAccessToken()
        );

        return "redirect:/";
    }

    /*
     * =========================
     * SPOTIFY DATA
     * =========================
     */

    public void getSpotifyData(
            String accessToken,
            Model model
    ) {

        getRecentlyPlayed(
                accessToken,
                model
        );

        SpotifyCurrentlyPlayingResponse currentlyPlaying =
                getCurrentlyPlaying(
                        accessToken
                );

        if (
                currentlyPlaying != null
                        && currentlyPlaying.getItem() != null
        ) {

            model.addAttribute(
                    "currentlyPlaying",
                    currentlyPlaying
            );
        }
    }

    /*
     * =========================
     * RECENTLY PLAYED
     * =========================
     */

    private void getRecentlyPlayed(
            String accessToken,
            Model model
    ) {

        RestTemplate restTemplate =
                new RestTemplate();

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(
                accessToken
        );

        HttpEntity<Void> request =
                new HttpEntity<>(
                        headers
                );

        ResponseEntity<SpotifyRecentlyPlayedResponse> response =
                restTemplate.exchange(
                        "https://api.spotify.com/v1/me/player/recently-played?limit=50",
                        HttpMethod.GET,
                        request,
                        SpotifyRecentlyPlayedResponse.class
                );

        SpotifyRecentlyPlayedResponse recentlyPlayed =
                response.getBody();

        if (
                recentlyPlayed == null
                        || recentlyPlayed.getItems() == null
        ) {
            return;
        }

        model.addAttribute(
                "recentTrack",
                recentlyPlayed.getItems().isEmpty()
                        ? null
                        : recentlyPlayed.getItems().get(0)
        );

        int recentTracks =
                recentlyPlayed.getItems().size();

        long uniqueTracks =
                recentlyPlayed.getItems()
                        .stream()
                        .map(item ->
                                item.getTrack().getId()
                        )
                        .distinct()
                        .count();

        long uniqueArtists =
                recentlyPlayed.getItems()
                        .stream()
                        .flatMap(item ->
                                item.getTrack()
                                        .getArtists()
                                        .stream()
                        )
                        .map(SpotifyArtist::getName)
                        .distinct()
                        .count();

        long totalDurationMs =
                recentlyPlayed.getItems()
                        .stream()
                        .mapToLong(item ->
                                item.getTrack()
                                        .getDurationMs()
                        )
                        .sum();

        long totalMinutes =
                totalDurationMs / 1000 / 60;

        model.addAttribute(
                "recentTracksCount",
                recentTracks
        );

        model.addAttribute(
                "uniqueTracksCount",
                uniqueTracks
        );

        model.addAttribute(
                "uniqueArtistsCount",
                uniqueArtists
        );

        model.addAttribute(
                "listeningMinutes",
                totalMinutes
        );
    }

    /*
     * =========================
     * CURRENTLY PLAYING
     * =========================
     */

    private SpotifyCurrentlyPlayingResponse getCurrentlyPlaying(
            String accessToken
    ) {

        RestTemplate restTemplate =
                new RestTemplate();

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(
                accessToken
        );

        HttpEntity<Void> request =
                new HttpEntity<>(
                        headers
                );

        ResponseEntity<SpotifyCurrentlyPlayingResponse> response =
                restTemplate.exchange(
                        "https://api.spotify.com/v1/me/player",
                        HttpMethod.GET,
                        request,
                        SpotifyCurrentlyPlayingResponse.class
                );

        return response.getBody();
    }

    @GetMapping("/spotify/player/current")
    @ResponseBody
    public ResponseEntity<SpotifyCurrentlyPlayingResponse> current(
            HttpSession session
    ) {

        String accessToken =
                (String) session.getAttribute(
                        "spotifyAccessToken"
                );

        if (accessToken == null) {

            return ResponseEntity
                    .status(401)
                    .build();
        }

        SpotifyCurrentlyPlayingResponse currentlyPlaying =
                getCurrentlyPlaying(
                        accessToken
                );

        if (
                currentlyPlaying == null
                        || currentlyPlaying.getItem() == null
        ) {

            return ResponseEntity
                    .noContent()
                    .build();
        }

        return ResponseEntity.ok(
                currentlyPlaying
        );
    }

    /*
     * =========================
     * PLAYER CONTROLS
     * =========================
     */

    @PostMapping("/spotify/player/play")
    @ResponseBody
    public ResponseEntity<Void> play(
            HttpSession session
    ) {

        return sendPlayerCommand(
                session,
                "https://api.spotify.com/v1/me/player/play",
                HttpMethod.PUT
        );
    }

    @PostMapping("/spotify/player/pause")
    @ResponseBody
    public ResponseEntity<Void> pause(
            HttpSession session
    ) {

        return sendPlayerCommand(
                session,
                "https://api.spotify.com/v1/me/player/pause",
                HttpMethod.PUT
        );
    }

    @PostMapping("/spotify/player/next")
    @ResponseBody
    public ResponseEntity<Void> next(
            HttpSession session
    ) {

        return sendPlayerCommand(
                session,
                "https://api.spotify.com/v1/me/player/next",
                HttpMethod.POST
        );
    }

    @PostMapping("/spotify/player/previous")
    @ResponseBody
    public ResponseEntity<Void> previous(
            HttpSession session
    ) {

        return sendPlayerCommand(
                session,
                "https://api.spotify.com/v1/me/player/previous",
                HttpMethod.POST
        );
    }

    private ResponseEntity<Void> sendPlayerCommand(
            HttpSession session,
            String url,
            HttpMethod method
    ) {

        String accessToken =
                (String) session.getAttribute(
                        "spotifyAccessToken"
                );

        if (accessToken == null) {

            return ResponseEntity
                    .status(401)
                    .build();
        }

        RestTemplate restTemplate =
                new RestTemplate();

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(
                accessToken
        );

        HttpEntity<Void> request =
                new HttpEntity<>(
                        headers
                );

        return restTemplate.exchange(
                url,
                method,
                request,
                Void.class
        );
    }

    /*
     * =========================
     * TOP ARTISTS
     * =========================
     *
     * timeRange can be:
     *
     * short_term  = Last 4 weeks
     * medium_term = Last 6 months
     * long_term   = All time
     */

    public SpotifyTopArtistsResponse getTopArtists(
            String accessToken,
            String timeRange
    ) {

        /*
         * Only allow Spotify's three
         * supported time ranges.
         */

        if (
                timeRange == null
                        || (
                        !timeRange.equals("short_term")
                                && !timeRange.equals("medium_term")
                                && !timeRange.equals("long_term")
                )
        ) {

            timeRange = "medium_term";
        }

        RestTemplate restTemplate =
                new RestTemplate();

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(
                accessToken
        );

        HttpEntity<Void> request =
                new HttpEntity<>(
                        headers
                );

        String url =
                "https://api.spotify.com/v1/me/top/artists"
                        + "?time_range="
                        + timeRange
                        + "&limit=6";

        ResponseEntity<SpotifyTopArtistsResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        request,
                        SpotifyTopArtistsResponse.class
                );

        return response.getBody();
    }
}
