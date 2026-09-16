package com.example.MusicBoxd.api.ticketmaster;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class TicketmasterService {

    private final RestClient restClient;
    private final String apiKey;

    public TicketmasterService(
            @Value("${ticketmaster.api.base-url}") String baseUrl,
            @Value("${ticketmaster.api.key}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
    }

    public String getAttractionId(String artistName) {

        TicketmasterAttractionResponse response =
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/attractions.json")
                                .queryParam("keyword", artistName)
                                .queryParam("apikey", apiKey)
                                .build())
                        .retrieve()
                        .body(TicketmasterAttractionResponse.class);

        if (response == null ||
                response.getEmbedded() == null ||
                response.getEmbedded().getAttractions() == null) {
            return null;
        }

        return response.getEmbedded()
                .getAttractions()
                .stream()
                .filter(attraction ->
                        attraction.getName().equalsIgnoreCase(artistName))
                .map(TicketmasterAttractionResponse.Attraction::getId)
                .findFirst()
                .orElse(null);
    }

    public List<Concert> getShowsByAttractionId(String attractionId) {

        TicketmasterEventResponse response =
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/events.json")
                                .queryParam("attractionId", attractionId)
                                .queryParam("sort", "date,asc")
                                .queryParam("apikey", apiKey)
                                .build())
                        .retrieve()
                        .body(TicketmasterEventResponse.class);

        if (response == null ||
                response.getEmbedded() == null ||
                response.getEmbedded().getEvents() == null) {
            return List.of();
        }

        return response.getEmbedded()
                .getEvents()
                .stream()
                .map(this::convertToConcert)
                .toList();
    }

    private Concert convertToConcert(TicketmasterEventResponse.Event event) {

        String venue = null;
        String city = null;

        if (event.getEmbedded() != null &&
                event.getEmbedded().getVenues() != null &&
                !event.getEmbedded().getVenues().isEmpty()) {

            TicketmasterEventResponse.Venue eventVenue =
                    event.getEmbedded().getVenues().getFirst();

            venue = eventVenue.getName();

            if (eventVenue.getCity() != null) {
                city = eventVenue.getCity().getName();
            }
        }

        return new Concert(
                event.getName(),
                event.getDates() != null &&
                        event.getDates().getStart() != null
                        ? event.getDates().getStart().getLocalDate()
                        : null,
                venue,
                city,
                event.getUrl()
        );
    }
}
