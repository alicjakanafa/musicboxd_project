package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.ListItem;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ListItemRepository;
import com.example.MusicBoxd.Repository.ListRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.itunes.ItunesAlbumResponse;
import com.example.MusicBoxd.api.itunes.ItunesService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/lists")
public class ListController {

    private final ListRepository listRepository;
    private final ListItemRepository listItemRepository;
    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final ItunesService itunesService;

    public ListController(
            ListRepository listRepository,
            ListItemRepository listItemRepository,
            AlbumRepository albumRepository,
            UserRepository userRepository,
            ItunesService itunesService
    ) {
        this.listRepository = listRepository;
        this.listItemRepository = listItemRepository;
        this.albumRepository = albumRepository;
        this.userRepository = userRepository;
        this.itunesService = itunesService;
    }

    @GetMapping
    public String lists(Authentication authentication, Model model) {

        User user = getCurrentUser(authentication);

        model.addAttribute(
                "lists",
                listRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
        );

        return "placeholder-lists";
    }

    @PostMapping
    public String createList(Authentication authentication, @RequestParam String title, @RequestParam String description) {

        User user = getCurrentUser(authentication);

        com.example.MusicBoxd.Model.List list =
                new com.example.MusicBoxd.Model.List(user.getId(), title, description);

        listRepository.save(list);

        return "redirect:/lists";
    }

    @GetMapping("/{id}")
    public String showList(
            Authentication authentication,
            @PathVariable Long id,
            Model model
    ) {

        User user = getCurrentUser(authentication);

        com.example.MusicBoxd.Model.List list =
                listRepository.findById(id)
                        .orElseThrow();

        if (!list.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN
            );
        }

        model.addAttribute("list", list);
        model.addAttribute("items", listItemRepository.findByListIdOrderByPositionAsc(id));

        return "placeholder-list-items";
    }

    @GetMapping("/{id}/search")
    public String searchAlbums(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String q,
            Model model
    ) {

        User user = getCurrentUser(authentication);

        com.example.MusicBoxd.Model.List list =
                listRepository.findById(id)
                        .orElseThrow();

        if (!list.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN
            );
        }

        ItunesAlbumResponse response = itunesService.searchAlbums(q);

        model.addAttribute("listId", id);
        model.addAttribute("results", response != null ? response.getResults() : java.util.List.of());

        return "placeholder-list-items :: searchResults";
    }

    @PostMapping("/{id}/albums")
    public String addAlbum(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam Long albumId
    ) {

        User user = getCurrentUser(authentication);

        com.example.MusicBoxd.Model.List list =
                listRepository.findById(id)
                        .orElseThrow();

        if (!list.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN
            );
        }

        int nextPosition = listItemRepository.findMaxPosition(id) + 1;

        ListItem item =
                new ListItem(id, albumId, null, nextPosition);

        listItemRepository.save(item);

        return "redirect:/lists/" + id;
    }

    @PostMapping("/{id}/songs")
    public String addSong(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam Long songId
    ) {

        User user = getCurrentUser(authentication);

        com.example.MusicBoxd.Model.List list =
                listRepository.findById(id)
                        .orElseThrow();

        if (!list.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN
            );
        }

        int nextPosition = listItemRepository.findMaxPosition(id) + 1;

        ListItem item =
                new ListItem(id, null, songId, nextPosition);

        listItemRepository.save(item);

        return "redirect:/lists/" + id;
    }

    private User getCurrentUser(Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal()
                        instanceof DefaultOidcUser principal)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED
            );
        }

        return userRepository
                .findByOktaUserId(principal.getSubject())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED
                        )
                );
    }

    @GetMapping("/placeholder-list-form")
    public String placeholderListForm() {
        return "placeholder-list-form";
    }
}