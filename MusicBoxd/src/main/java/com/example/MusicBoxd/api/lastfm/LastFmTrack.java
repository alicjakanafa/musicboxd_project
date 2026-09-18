package com.example.MusicBoxd.api.lastfm;

public class LastFmTrack {

    private String name;
    private String duration;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getFormattedDuration() {

        if (duration == null || duration.isEmpty()) {
            return "";
        }

        try {
            int totalSeconds = Integer.parseInt(duration);

            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;

            return String.format(
                    "%d:%02d",
                    minutes,
                    seconds
            );

        } catch (NumberFormatException e) {
            return "";
        }
    }
}