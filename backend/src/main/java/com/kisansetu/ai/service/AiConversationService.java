package com.kisansetu.ai.service;

import com.kisansetu.ai.dto.ChatMessage;
import com.kisansetu.ai.entity.AiConversation;
import com.kisansetu.ai.entity.AiMessage;
import com.kisansetu.ai.repository.AiConversationRepository;
import com.kisansetu.ai.repository.AiMessageRepository;
import com.kisansetu.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Persistent agricultural chatbot conversations.
 */
@Service
@RequiredArgsConstructor
public class AiConversationService {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AiChatService chatService;

    @Transactional(readOnly = true)
    public List<AiConversation> getConversations(UUID userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional
    public AiConversation createConversation(UUID userId, String title) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        if (title != null && !title.isBlank()) {
            conversation.setTitle(title);
        }
        return conversationRepository.save(conversation);
    }

    @Transactional
    public void deleteConversation(UUID userId, UUID conversationId) {
        AiConversation conversation = getOwnedConversation(userId, conversationId);
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    @Transactional
    public void clearConversation(UUID userId, UUID conversationId) {
        getOwnedConversation(userId, conversationId);
        messageRepository.deleteByConversationId(conversationId);
    }

    @Transactional(readOnly = true)
    public List<AiMessage> getMessages(UUID userId, UUID conversationId) {
        getOwnedConversation(userId, conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * Send a user message; persists both sides and returns the reply.
     */
    @Transactional
    public SendResult sendMessage(UUID userId, UUID conversationId, String content) {
        AiConversation conversation = getOwnedConversation(userId, conversationId);

        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        messageRepository.save(userMessage);

        List<AiMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<ChatMessage> chatHistory = history.stream()
                .map(m -> new ChatMessage(m.getRole(), m.getContent()))
                .toList();

        String reply;
        try {
            reply = chatService.replyWithHistory(content, chatHistory);
        } catch (Exception e) {
            // Persist user message, rethrow for the client to retry
            throw e;
        }

        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply);
        messageRepository.save(assistantMessage);

        if (conversation.getTitle() == null || conversation.getTitle().isBlank()
                || "New conversation".equals(conversation.getTitle())) {
            conversation.setTitle(abbreviate(content, 60));
        }
        conversationRepository.save(conversation);
        return new SendResult(userMessage, assistantMessage, reply);
    }

    private AiConversation getOwnedConversation(UUID userId, UUID conversationId) {
        AiConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
        if (!conversation.getUserId().equals(userId)) {
            throw ApiException.forbidden("This conversation does not belong to you");
        }
        return conversation;
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "New conversation";
        }
        String singleLine = text.replaceAll("\\s+", " ").trim();
        return singleLine.length() > max ? singleLine.substring(0, max) + "..." : singleLine;
    }

    public record SendResult(AiMessage userMessage, AiMessage assistantMessage, String reply) {
    }
}