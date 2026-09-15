package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.api.spotify.SpotifyCurrentlyPlayingResponse;
import com.example.MusicBoxd.api.spotify.SpotifyRecentlyPlayedResponse;
import com.example.MusicBoxd.api.spotify.SpotifyTokenResponse;
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


    /*
     * ---------------------------------------------------------
     * SPOTIFY LOGIN
     * ---------------------------------------------------------
     */

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
                                "streaming"
                )
                .build()
                .toUriString();

        return "redirect:" + spotifyUrl;
    }


    /*
     * ---------------------------------------------------------
     * SPOTIFY CALLBACK
     * ---------------------------------------------------------
     */

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
     * ---------------------------------------------------------
     * GET SPOTIFY DATA
     * ---------------------------------------------------------
     */

    public void getSpotifyData(
            String accessToken,
            Model model
    ) {
        getRecentlyPlayed(accessToken, model);

        SpotifyCurrentlyPlayingResponse currentlyPlaying =
                getCurrentlyPlaying(accessToken);

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
     * ---------------------------------------------------------
     * RECENTLY PLAYED
     * ---------------------------------------------------------
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
                        "https://api.spotify.com/v1/me/player/recently-played?limit=1",
                        HttpMethod.GET,
                        request,
                        SpotifyRecentlyPlayedResponse.class
                );

        SpotifyRecentlyPlayedResponse recentlyPlayed =
                response.getBody();

        if (
                recentlyPlayed != null
                        && recentlyPlayed.getItems() != null
                        && !recentlyPlayed.getItems().isEmpty()
        ) {

            SpotifyRecentlyPlayedResponse.SpotifyRecentlyPlayedItem item =
                    recentlyPlayed
                            .getItems()
                            .get(0);

            model.addAttribute(
                    "recentTrack",
                    item
            );
        }
    }


    /*
     * ---------------------------------------------------------
     * CURRENTLY PLAYING
     * ---------------------------------------------------------
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


    /*
     * ---------------------------------------------------------
     * CURRENTLY PLAYING FOR JAVASCRIPT
     *
     * This endpoint allows the homepage to ask Spotify
     * what is currently playing without refreshing the page.
     * ---------------------------------------------------------
     */

    @GetMapping("/spotify/player/current")
    @ResponseBody
    public ResponseEntity<SpotifyCurrentlyPlayingResponse> current(
            HttpSession session
    ) {

        String accessToken =
                (String) session.getAttribute(
                        "spotifyAccessToken"
                );

        /*
         * User has not connected Spotify.
         */

        if (accessToken == null) {

            return ResponseEntity
                    .status(401)
                    .build();
        }

        SpotifyCurrentlyPlayingResponse currentlyPlaying =
                getCurrentlyPlaying(
                        accessToken
                );

        /*
         * Spotify has nothing currently playing.
         */

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
     * ---------------------------------------------------------
     * PLAY
     * ---------------------------------------------------------
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


    /*
     * ---------------------------------------------------------
     * PAUSE
     * ---------------------------------------------------------
     */

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


    /*
     * ---------------------------------------------------------
     * NEXT TRACK
     * ---------------------------------------------------------
     */

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


    /*
     * ---------------------------------------------------------
     * PREVIOUS TRACK
     * ---------------------------------------------------------
     */

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


    /*
     * ---------------------------------------------------------
     * SEND COMMAND TO SPOTIFY
     * ---------------------------------------------------------
     */

    private ResponseEntity<Void> sendPlayerCommand(
            HttpSession session,
            String url,
            HttpMethod method
    ) {

        String accessToken =
                (String) session.getAttribute(
                        "spotifyAccessToken"
                );


        /*
         * The user hasn't connected Spotify.
         */

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
}
