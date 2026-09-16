package com.example.MusicBoxd.api.ticketmaster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Setter
public class TicketmasterAttractionResponse {

    @JsonProperty("_embedded")
    private Embedded embedded;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter @Setter
    public static class Embedded {

        private List<Attraction> attractions;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter @Setter
    public static class Attraction {

        private String id;
        private String name;

    }
}
