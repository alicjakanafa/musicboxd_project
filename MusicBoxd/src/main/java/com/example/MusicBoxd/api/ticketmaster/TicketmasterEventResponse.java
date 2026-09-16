package com.example.MusicBoxd.api.ticketmaster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Setter
public class TicketmasterEventResponse {

    @JsonProperty("_embedded")
    private Embedded embedded;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter @Setter
    public static class Embedded {

        private List<Event> events;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter @Setter
    public static class Event {

        private String name;
        private String url;
        private Dates dates;

        @JsonProperty("_embedded")
        private EventEmbedded embedded;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter @Setter
    public static class Dates {

        private Start start;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter @Setter
    public static class Start {

        private String localDate;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter @Setter
    public static class EventEmbedded {

        private List<Venue> venues;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter @Setter
    public static class Venue {

        private String name;
        private City city;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter @Setter
    public static class City {

        private String name;

    }
}
