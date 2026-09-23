package com.example.MusicBoxd.api.ticketmaster;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TicketmasterServiceTest {

    private static final String BASE_URL = "https://app.ticketmaster.com/discovery/v2";
    private static final String API_KEY = "test-tm-key";

    private TicketmasterService ticketmasterService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        ticketmasterService = new TicketmasterService(BASE_URL, API_KEY);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        ReflectionTestUtils.setField(ticketmasterService, "restClient", restClient);
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    void getAttractionIdReturnsIdWhenAttractionNameMatchesExactly() {
        String responseBody = """
                {
                  "_embedded": {
                    "attractions": [
                      { "id": "K8vZ917oCV0", "name": "Radiohead" },
                      { "id": "K8vZ917xyz1", "name": "Radiohead Tribute Band" }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(startsWith(BASE_URL + "/attractions.json")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("keyword", "Radiohead"))
                .andExpect(queryParam("apikey", API_KEY))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        String attractionId = ticketmasterService.getAttractionId("Radiohead");

        assertThat(attractionId).isEqualTo("K8vZ917oCV0");
    }

    @Test
    void getAttractionIdIsCaseInsensitiveWhenMatchingArtistName() {
        String responseBody = """
                {
                  "_embedded": {
                    "attractions": [
                      { "id": "K8vZ917oCV0", "name": "RADIOHEAD" }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(startsWith(BASE_URL + "/attractions.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        String attractionId = ticketmasterService.getAttractionId("radiohead");

        assertThat(attractionId).isEqualTo("K8vZ917oCV0");
    }

    @Test
    void getAttractionIdReturnsNullWhenNoAttractionNameMatches() {
        String responseBody = """
                {
                  "_embedded": {
                    "attractions": [
                      { "id": "K8vZ917oCV0", "name": "Some Other Artist" }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(startsWith(BASE_URL + "/attractions.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        String attractionId = ticketmasterService.getAttractionId("Radiohead");

        assertThat(attractionId).isNull();
    }

    @Test
    void getAttractionIdReturnsNullWhenEmbeddedIsMissing() {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/attractions.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        String attractionId = ticketmasterService.getAttractionId("Radiohead");

        assertThat(attractionId).isNull();
    }

    @Test
    void getAttractionIdReturnsNullWhenAttractionsListIsMissing() {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/attractions.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{ \"_embedded\": {} }", MediaType.APPLICATION_JSON));

        String attractionId = ticketmasterService.getAttractionId("Radiohead");

        assertThat(attractionId).isNull();
    }

    @Test
    void getAttractionIdReturnsNullWhenResponseBodyIsEmpty() {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/attractions.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        String attractionId = ticketmasterService.getAttractionId("Radiohead");

        assertThat(attractionId).isNull();
    }

    @Test
    void getShowsByAttractionIdReturnsEmptyListWhenResponseBodyIsEmpty() {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        List<Concert> shows = ticketmasterService.getShowsByAttractionId("K8vZ917oCV0");

        assertThat(shows).isEmpty();
    }

    @Test
    void getShowsByAttractionIdReturnsEmptyListWhenEventsFieldIsExplicitlyNull() {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{ \"_embedded\": { \"events\": null } }", MediaType.APPLICATION_JSON));

        List<Concert> shows = ticketmasterService.getShowsByAttractionId("K8vZ917oCV0");

        assertThat(shows).isEmpty();
    }

    @Test
    void getShowsByAttractionIdLeavesCityNullWhenVenueHasNoCity() {
        String responseBody = """
                {
                  "_embedded": {
                    "events": [
                      {
                        "name": "Warehouse Show",
                        "url": "https://ticketmaster.com/event/3",
                        "_embedded": {
                          "venues": [
                            { "name": "Secret Warehouse" }
                          ]
                        }
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(startsWith(BASE_URL + "/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<Concert> shows = ticketmasterService.getShowsByAttractionId("K8vZ917oCV0");

        assertThat(shows).hasSize(1);
        Concert concert = shows.get(0);
        assertThat(concert.venue()).isEqualTo("Secret Warehouse");
        assertThat(concert.city()).isNull();
    }

    @Test
    void getShowsByAttractionIdTreatsExplicitlyNullVenuesListAsNoVenue() {
        String responseBody = """
                {
                  "_embedded": {
                    "events": [
                      {
                        "name": "TBA Show",
                        "url": "https://ticketmaster.com/event/5",
                        "_embedded": { "venues": null }
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(startsWith(BASE_URL + "/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<Concert> shows = ticketmasterService.getShowsByAttractionId("K8vZ917oCV0");

        assertThat(shows).hasSize(1);
        assertThat(shows.get(0).venue()).isNull();
        assertThat(shows.get(0).city()).isNull();
    }

    @Test
    void getShowsByAttractionIdLeavesDateNullWhenStartDateIsMissing() {
        String responseBody = """
                {
                  "_embedded": {
                    "events": [
                      {
                        "name": "Date TBD Show",
                        "url": "https://ticketmaster.com/event/6",
                        "dates": { "start": null }
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(startsWith(BASE_URL + "/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<Concert> shows = ticketmasterService.getShowsByAttractionId("K8vZ917oCV0");

        assertThat(shows).hasSize(1);
        assertThat(shows.get(0).date()).isNull();
    }

    @Test
    void getShowsByAttractionIdTreatsEmptyVenuesListAsNoVenue() {
        String responseBody = """
                {
                  "_embedded": {
                    "events": [
                      {
                        "name": "TBA Show",
                        "url": "https://ticketmaster.com/event/4",
                        "_embedded": {
                          "venues": []
                        }
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(startsWith(BASE_URL + "/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<Concert> shows = ticketmasterService.getShowsByAttractionId("K8vZ917oCV0");

        assertThat(shows).hasSize(1);
        assertThat(shows.get(0).venue()).isNull();
        assertThat(shows.get(0).city()).isNull();
    }

    @Test
    void getShowsByAttractionIdReturnsConcertsWithVenueCityAndDate() {
        String responseBody = """
                {
                  "_embedded": {
                    "events": [
                      {
                        "name": "Radiohead Live",
                        "url": "https://ticketmaster.com/event/1",
                        "dates": {
                          "start": { "localDate": "2026-05-01" }
                        },
                        "_embedded": {
                          "venues": [
                            {
                              "name": "Madison Square Garden",
                              "city": { "name": "New York" }
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(startsWith(BASE_URL + "/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("attractionId", "K8vZ917oCV0"))
                .andExpect(queryParam("sort", "date,asc"))
                .andExpect(queryParam("apikey", API_KEY))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<Concert> shows = ticketmasterService.getShowsByAttractionId("K8vZ917oCV0");

        assertThat(shows).hasSize(1);
        Concert concert = shows.get(0);
        assertThat(concert.name()).isEqualTo("Radiohead Live");
        assertThat(concert.date()).isEqualTo("2026-05-01");
        assertThat(concert.venue()).isEqualTo("Madison Square Garden");
        assertThat(concert.city()).isEqualTo("New York");
        assertThat(concert.ticketUrl()).isEqualTo("https://ticketmaster.com/event/1");
    }

    @Test
    void getShowsByAttractionIdHandlesMissingVenueAndDateGracefully() {
        String responseBody = """
                {
                  "_embedded": {
                    "events": [
                      { "name": "Mystery Show", "url": "https://ticketmaster.com/event/2" }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(startsWith(BASE_URL + "/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<Concert> shows = ticketmasterService.getShowsByAttractionId("K8vZ917oCV0");

        assertThat(shows).hasSize(1);
        Concert concert = shows.get(0);
        assertThat(concert.name()).isEqualTo("Mystery Show");
        assertThat(concert.date()).isNull();
        assertThat(concert.venue()).isNull();
        assertThat(concert.city()).isNull();
    }

    @Test
    void getShowsByAttractionIdReturnsEmptyListWhenNoEventsPresent() {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{ \"_embedded\": { \"events\": [] } }", MediaType.APPLICATION_JSON));

        List<Concert> shows = ticketmasterService.getShowsByAttractionId("K8vZ917oCV0");

        assertThat(shows).isEmpty();
    }

    @Test
    void getShowsByAttractionIdReturnsEmptyListWhenEmbeddedIsMissing() {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<Concert> shows = ticketmasterService.getShowsByAttractionId("K8vZ917oCV0");

        assertThat(shows).isEmpty();
    }

    @Test
    void getShowsByAttractionIdPropagatesExceptionOnServerError() {
        mockServer.expect(requestTo(startsWith(BASE_URL + "/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> ticketmasterService.getShowsByAttractionId("K8vZ917oCV0"))
                .isInstanceOf(HttpServerErrorException.class);
    }
}
