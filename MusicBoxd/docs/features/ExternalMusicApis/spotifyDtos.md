## Spotify DTOs

`com.example.MusicBoxd.api.spotify` contains plain data-transfer objects only —
`SpotifyAlbum`, `SpotifyArtist`, `SpotifyCurrentlyPlayingResponse`,
`SpotifyExternalUrls`, `SpotifyImage`, `SpotifyRecentlyPlayedResponse`,
`SpotifyTokenResponse` and `SpotifyTrack`. Unlike `api/itunes` and `api/lastfm`, there
is **no `SpotifyService`** in this package.

Spotify API calls (OAuth token exchange, currently-playing, recently-played, etc.) are
made directly from `Controller/SpotifyController.java`, which owns the HTTP calls and
deserializes responses into these DTOs. `SpotifyController` is out of scope for this
documentation pass and is not covered here.

### Related documentation

- [iTunes service](./itunesServiceDocumentation.md)
- [Last.fm service](./lastFmServiceDocumentation.md)
