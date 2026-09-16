package com.example.MusicBoxd.api.ticketmaster;

public record Concert(
        String name,
        String date,
        String venue,
        String city,
        String ticketUrl
) {
}
