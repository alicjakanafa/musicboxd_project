package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Repository.FriendRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/friends")
public class FriendController {

    private final FriendRepository friendRepository;

    public FriendController(
            FriendRepository friendRepository
    ) {
        this.friendRepository = friendRepository;
    }


    private Long getCurrentUserId() {
        return 1L;
    }


    @GetMapping
    public String friendsPage(Model model) {

        Long currentUserId = getCurrentUserId();



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

        List<Long> friendIds = new ArrayList<>();

        for (Friend friendship : acceptedRelationships) {

            if (currentUserId.equals(
                    friendship.getRequesterId()
            )) {

                friendIds.add(
                        friendship.getReceiverId()
                );

            } else if (currentUserId.equals(
                    friendship.getReceiverId()
            )) {

                friendIds.add(
                        friendship.getRequesterId()
                );
            }
        }

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

        return "friends";
    }


    @PostMapping("/request")
    public String sendFriendRequest(
            @RequestParam Long receiverId
    ) {

        Long requesterId =
                getCurrentUserId();


        if (requesterId.equals(receiverId)) {
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

        return "redirect:/friends";
    }




    @PostMapping("/accept/{id}")
    public String acceptFriendRequest(
            @PathVariable Long id
    ) {

        Long currentUserId =
                getCurrentUserId();

        Friend friend =
                friendRepository
                        .findById(id)
                        .orElse(null);

        if (friend == null) {
            return "redirect:/friends";
        }




        if (!currentUserId.equals(
                friend.getReceiverId()
        )) {

            return "redirect:/friends";
        }




        if (!"PENDING".equals(
                friend.getStatus()
        )) {

            return "redirect:/friends";
        }


        friend.setStatus("ACCEPTED");

        friendRepository.save(friend);

        return "redirect:/friends";
    }




    @PostMapping("/decline/{id}")
    public String declineFriendRequest(
            @PathVariable Long id
    ) {

        Long currentUserId =
                getCurrentUserId();

        Friend friend =
                friendRepository
                        .findById(id)
                        .orElse(null);

        if (friend == null) {
            return "redirect:/friends";
        }




        if (!currentUserId.equals(
                friend.getReceiverId()
        )) {

            return "redirect:/friends";
        }


        friend.setStatus("DECLINED");

        friendRepository.save(friend);

        return "redirect:/friends";
    }



    @PostMapping("/remove/{id}")
    public String removeFriend(
            @PathVariable Long id
    ) {

        Long currentUserId =
                getCurrentUserId();

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

        if (!isRequester && !isReceiver) {
            return "redirect:/friends";
        }


        friendRepository.delete(friend);

        return "redirect:/friends";
    }
}
