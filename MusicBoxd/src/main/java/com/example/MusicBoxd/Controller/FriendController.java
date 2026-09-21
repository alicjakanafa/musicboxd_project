package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.FriendRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/friends")
public class FriendController {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FriendController(
            FriendRepository friendRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    private User getCurrentUser(
            Authentication authentication
    ) {

        OidcUser principal =
                (OidcUser) authentication.getPrincipal();

        String oktaUserId =
                principal.getSubject();

        return userRepository
                .findByOktaUserId(oktaUserId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Current user not found"
                        )
                );
    }

    @GetMapping
    public String friendsPage(
            Model model,
            Authentication authentication
    ) {

        User currentUser =
                getCurrentUser(authentication);

        loadFriendsPage(
                model,
                currentUser
        );

        return "friends";
    }

    @GetMapping("/search")
    public String searchUsers(
            @RequestParam(required = false) String query,
            Model model,
            Authentication authentication
    ) {

        User currentUser =
                getCurrentUser(authentication);

        loadFriendsPage(
                model,
                currentUser
        );

        List<User> searchResults =
                new ArrayList<>();

        if (
                query != null &&
                        !query.isBlank()
        ) {

            searchResults =
                    userRepository
                            .findByUsernameContainingIgnoreCase(
                                    query.trim()
                            );

            searchResults.removeIf(
                    user ->
                            user.getId().equals(
                                    currentUser.getId()
                            )
            );
        }


        model.addAttribute(
                "searchResults",
                searchResults
        );

        model.addAttribute(
                "searchQuery",
                query
        );


        return "friends";
    }


    private void loadFriendsPage(
            Model model,
            User currentUser
    ) {

        Long currentUserId =
                currentUser.getId();


        List<Friend> pendingRequests =
                friendRepository
                        .findByReceiverIdAndStatus(
                                currentUserId,
                                "PENDING"
                        );


        List<Friend> sentRequests =
                friendRepository
                        .findByRequesterIdAndStatus(
                                currentUserId,
                                "PENDING"
                        );


        List<Friend> acceptedRelationships =
                friendRepository
                        .findByStatus("ACCEPTED");


        List<Long> friendIds =
                new ArrayList<>();


        for (
                Friend friendship :
                acceptedRelationships
        ) {

            if (
                    currentUserId.equals(
                            friendship.getRequesterId()
                    )
            ) {

                friendIds.add(
                        friendship.getReceiverId()
                );

            } else if (
                    currentUserId.equals(
                            friendship.getReceiverId()
                    )
            ) {

                friendIds.add(
                        friendship.getRequesterId()
                );
            }
        }


        Set<Long> excludedUserIds =
                new HashSet<>();


        excludedUserIds.add(
                currentUserId
        );


        excludedUserIds.addAll(
                friendIds
        );


        for (
                Friend request :
                sentRequests
        ) {

            excludedUserIds.add(
                    request.getReceiverId()
            );
        }


        for (
                Friend request :
                pendingRequests
        ) {

            excludedUserIds.add(
                    request.getRequesterId()
            );
        }


        List<User> allUsers =
                (List<User>) userRepository.findAll();


        List<User> suggestedUsers =
                new ArrayList<>();


        for (
                User user :
                allUsers
        ) {

            if (
                    !excludedUserIds.contains(
                            user.getId()
                    )
            ) {

                suggestedUsers.add(user);
            }
        }


        Map<Long, User> requesters =
                new HashMap<>();


        for (
                Friend request :
                pendingRequests
        ) {

            userRepository
                    .findById(
                            request.getRequesterId()
                    )
                    .ifPresent(
                            user ->
                                    requesters.put(
                                            user.getId(),
                                            user
                                    )
                    );
        }


        Map<Long, User> sentUsers =
                new HashMap<>();


        for (
                Friend request :
                sentRequests
        ) {

            userRepository
                    .findById(
                            request.getReceiverId()
                    )
                    .ifPresent(
                            user ->
                                    sentUsers.put(
                                            user.getId(),
                                            user
                                    )
                    );
        }


        Map<Long, User> friends =
                new HashMap<>();


        for (
                Long friendId :
                friendIds
        ) {

            userRepository
                    .findById(friendId)
                    .ifPresent(
                            user ->
                                    friends.put(
                                            user.getId(),
                                            user
                                    )
                    );
        }


        Set<Long> sentRequestIds =
                new HashSet<>();

        for (
                Friend request :
                sentRequests
        ) {

            sentRequestIds.add(
                    request.getReceiverId()
            );
        }


        Set<Long> receivedRequestIds =
                new HashSet<>();

        for (
                Friend request :
                pendingRequests
        ) {

            receivedRequestIds.add(
                    request.getRequesterId()
            );
        }


        model.addAttribute(
                "currentUser",
                currentUser
        );

        model.addAttribute(
                "pendingRequests",
                pendingRequests
        );

        model.addAttribute(
                "sentRequests",
                sentRequests
        );

        model.addAttribute(
                "friendIds",
                friendIds
        );

        model.addAttribute(
                "suggestedUsers",
                suggestedUsers
        );

        model.addAttribute(
                "requesters",
                requesters
        );

        model.addAttribute(
                "sentUsers",
                sentUsers
        );

        model.addAttribute(
                "friends",
                friends
        );

        model.addAttribute(
                "sentRequestIds",
                sentRequestIds
        );

        model.addAttribute(
                "receivedRequestIds",
                receivedRequestIds
        );

        // Empty search results by default
        model.addAttribute(
                "searchResults",
                new ArrayList<User>()
        );

        model.addAttribute(
                "searchQuery",
                null
        );
    }

    @PostMapping("/request")
    public String sendFriendRequest(
            @RequestParam Long receiverId,
            Authentication authentication
    ) {

        User currentUser =
                getCurrentUser(authentication);

        Long requesterId =
                currentUser.getId();


        if (
                requesterId.equals(receiverId)
        ) {

            return "redirect:/friends";
        }


        User receiver =
                userRepository
                        .findById(receiverId)
                        .orElse(null);

        if (receiver == null) {

            return "redirect:/friends";
        }


        boolean requestExists =
                friendRepository
                        .findByRequesterIdAndReceiverId(
                                requesterId,
                                receiverId
                        )
                        .isPresent();


        if (requestExists) {

            return "redirect:/friends";
        }


        boolean reverseRequestExists =
                friendRepository
                        .findByReceiverIdAndRequesterId(
                                requesterId,
                                receiverId
                        )
                        .isPresent();


        if (reverseRequestExists) {

            return "redirect:/friends";
        }


        Friend friend =
                new Friend(
                        requesterId,
                        receiverId,
                        "PENDING"
                );


        friendRepository.save(friend);

        notificationService.notifyFriendRequest(
                receiver.getId(),
                currentUser.getId(),
                currentUser.getUsername()
        );


        return "redirect:/friends";
    }

    @PostMapping("/accept/{id}")
    public String acceptFriendRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User currentUser =
                getCurrentUser(authentication);

        Long currentUserId =
                currentUser.getId();


        Friend friend =
                friendRepository
                        .findById(id)
                        .orElse(null);


        if (friend == null) {

            return "redirect:/friends";
        }


        if (
                !currentUserId.equals(
                        friend.getReceiverId()
                )
        ) {

            return "redirect:/friends";
        }


        if (
                !"PENDING".equals(
                        friend.getStatus()
                )
        ) {

            return "redirect:/friends";
        }


        friend.setStatus("ACCEPTED");

        friendRepository.save(friend);

        User requester =
                userRepository
                        .findById(
                                friend.getRequesterId()
                        )
                        .orElse(null);

        if (requester != null) {

            notificationService.notifyFriendAccepted(
                    requester.getId(),
                    currentUser.getId(),
                    currentUser.getUsername()
            );
        }


        return "redirect:/friends";
    }

    @PostMapping("/decline/{id}")
    public String declineFriendRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User currentUser =
                getCurrentUser(authentication);

        Long currentUserId =
                currentUser.getId();


        Friend friend =
                friendRepository
                        .findById(id)
                        .orElse(null);


        if (friend == null) {

            return "redirect:/friends";
        }


        if (
                !currentUserId.equals(
                        friend.getReceiverId()
                )
        ) {

            return "redirect:/friends";
        }


        if (
                !"PENDING".equals(
                        friend.getStatus()
                )
        ) {

            return "redirect:/friends";
        }


        friend.setStatus("DECLINED");

        friendRepository.save(friend);


        return "redirect:/friends";
    }

    @PostMapping("/remove/{id}")
    public String removeFriend(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User currentUser =
                getCurrentUser(authentication);

        Long currentUserId =
                currentUser.getId();


        Friend friend =
                friendRepository
                        .findById(id)
                        .orElse(null);


        if (friend == null) {

            return "redirect:/friends";
        }


        boolean isRequester =
                currentUserId.equals(
                        friend.getRequesterId()
                );


        boolean isReceiver =
                currentUserId.equals(
                        friend.getReceiverId()
                );


        if (
                !isRequester &&
                        !isReceiver
        ) {

            return "redirect:/friends";
        }


        friendRepository.delete(friend);


        return "redirect:/friends";
    }
}