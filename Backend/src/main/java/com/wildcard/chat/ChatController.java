package com.wildcard.chat;

import com.wildcard.chat.dto.ChatMessageResponse;
import com.wildcard.chat.dto.ConversationResponse;
import com.wildcard.chat.dto.CreateGroupRequest;
import com.wildcard.chat.dto.SendMessageRequest;
import com.wildcard.common.ApiResponse;
import com.wildcard.common.PageResponse;
import com.wildcard.common.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> myConversations() {
        return ApiResponse.success(chatService.myConversations(SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/direct/{userId}")
    public ApiResponse<ConversationResponse> openDirect(@PathVariable Long userId) {
        return ApiResponse.success("Conversation created",
                chatService.openDirect(SecurityUtils.getCurrentUser(), userId));
    }

    @PostMapping("/group")
    public ApiResponse<ConversationResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.success("Group created",
                chatService.createGroup(SecurityUtils.getCurrentUser(), request));
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> get(@PathVariable Long conversationId) {
        return ApiResponse.success(chatService.getConversation(SecurityUtils.getCurrentUser(), conversationId));
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<PageResponse<ChatMessageResponse>> messages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        return ApiResponse.success(chatService.messages(
                SecurityUtils.getCurrentUser(), conversationId, PageRequest.of(page, size)));
    }

    @PostMapping("/{conversationId}/messages")
    public ApiResponse<ChatMessageResponse> send(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request) {

        return ApiResponse.success("Message sent",
                chatService.send(SecurityUtils.getCurrentUser(), conversationId, request));
    }

    @PutMapping("/{conversationId}/read")
    public ApiResponse<Long> markRead(@PathVariable Long conversationId) {
        return ApiResponse.success(
                chatService.markRead(SecurityUtils.getCurrentUser(), conversationId));
    }
}