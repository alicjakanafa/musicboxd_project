package com.example.MusicBoxd.api.itunes;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

@Service
public class ItunesService {

    private final RestTemplate restTemplate;

    public ItunesService() {

        this.restTemplate =
                new RestTemplate();

        for (var converter :
                restTemplate.getMessageConverters()) {

            if (converter instanceof
                    JacksonJsonHttpMessageConverter
                            jsonConverter) {

                var mediaTypes =
                        new ArrayList<>(
                                jsonConverter
                                        .getSupportedMediaTypes()
                        );

                mediaTypes.add(
                        MediaType.valueOf(
                                "text/javascript"
                        )
                );

                jsonConverter.setSupportedMediaTypes(
                        mediaTypes
                );
            }
        }
    }

    public ItunesAlbumResponse searchAlbums(
            String searchTerm
    ) {

        String url =
                UriComponentsBuilder
                        .fromUriString(
                                "https://itunes.apple.com/search"
                        )
                        .queryParam(
                                "term",
                                searchTerm
                        )
                        .queryParam(
                                "media",
                                "music"
                        )
                        .queryParam(
                                "entity",
                                "album"
                        )
                        .queryParam(
                                "limit",
                                20
                        )
                        .build()
                        .toUriString();

        return restTemplate.getForObject(
                url,
                ItunesAlbumResponse.class
        );
    }

    public ItunesAlbumResponse searchAlbumsByArtist(
            String artistName
    ) {

        String url =
                UriComponentsBuilder
                        .fromUriString(
                                "https://itunes.apple.com/search"
                        )
                        .queryParam(
                                "term",
                                artistName
                        )
                        .queryParam(
                                "media",
                                "music"
                        )
                        .queryParam(
                                "entity",
                                "album"
                        )
                        .queryParam(
                                "limit",
                                200
                        )
                        .build()
                        .toUriString();

        return restTemplate.getForObject(
                url,
                ItunesAlbumResponse.class
        );
    }

    public ItunesTrackResponse searchTracks(
            String searchTerm
    ) {

        String url =
                UriComponentsBuilder
                        .fromUriString(
                                "https://itunes.apple.com/search"
                        )
                        .queryParam(
                                "term",
                                searchTerm
                        )
                        .queryParam(
                                "media",
                                "music"
                        )
                        .queryParam(
                                "entity",
                                "song"
                        )
                        .queryParam(
                                "limit",
                                20
                        )
                        .build()
                        .toUriString();

        return restTemplate.getForObject(
                url,
                ItunesTrackResponse.class
        );
    }

    /*
     * Returns a daily album based on the current user.
     *
     * The same user gets the same album for the same day.
     * Different users get different random seeds.
     */
    public ItunesAlbum getDailyAlbum(
            Long userId
    ) {

        String[] searchTerms = {
                "pop",
                "rock",
                "indie",
                "alternative",
                "jazz",
                "hip hop",
                "electronic",
                "country",
                "r&b"
        };

        long seed;

        if (userId != null) {

            seed =
                    Objects.hash(
                            LocalDate.now(),
                            userId
                    );

        } else {

            seed =
                    LocalDate.now()
                            .toEpochDay();
        }

        Random random =
                new Random(seed);

        int randomIndex =
                random.nextInt(
                        searchTerms.length
                );

        String randomSearchTerm =
                searchTerms[randomIndex];

        String url =
                UriComponentsBuilder
                        .fromUriString(
                                "https://itunes.apple.com/search"
                        )
                        .queryParam(
                                "term",
                                randomSearchTerm
                        )
                        .queryParam(
                                "media",
                                "music"
                        )
                        .queryParam(
                                "entity",
                                "album"
                        )
                        .queryParam(
                                "limit",
                                50
                        )
                        .build()
                        .toUriString();

        ItunesAlbumResponse response =
                restTemplate.getForObject(
                        url,
                        ItunesAlbumResponse.class
                );

        if (response == null ||
                response.getResults() == null ||
                response.getResults().isEmpty()) {

            return null;
        }

        var albums =
                response.getResults();

        int randomAlbumIndex =
                random.nextInt(
                        albums.size()
                );

        return albums.get(
                randomAlbumIndex
        );
    }

    public ItunesTrackResponse getAlbumTracks(
            Long collectionId
    ) {

        String url =
                UriComponentsBuilder
                        .fromUriString(
                                "https://itunes.apple.com/lookup"
                        )
                        .queryParam(
                                "id",
                                collectionId
                        )
                        .queryParam(
                                "entity",
                                "song"
                        )
                        .build()
                        .toUriString();

        return restTemplate.getForObject(
                url,
                ItunesTrackResponse.class
        );
    }
}