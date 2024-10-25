package com.planok.ssewhatsapp.service;

import com.planok.ssewhatsapp.event.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public final class SseEmitterService {

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID environmentId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(environmentId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("New subscription added for environmentId: {}. Total subscribers: {}", environmentId, emitters.get(environmentId).size());
        emitter.onCompletion(() -> {
            removeEmitter(environmentId, emitter);
            log.info("Emitter completed and removed for environmentId: {}. Remaining subscribers: {}", environmentId, emitters.getOrDefault(environmentId, List.of()).size());
        });
        emitter.onTimeout(() -> {
            removeEmitter(environmentId, emitter);
            log.warn("Emitter timed out and removed for environmentId: {}. Remaining subscribers: {}", environmentId, emitters.getOrDefault(environmentId, List.of()).size());
        });
        emitter.onError(e -> {
            removeEmitter(environmentId, emitter);
            log.error("Error in emitter for environmentId: {}. Removed emitter. Remaining subscribers: {}", environmentId, emitters.getOrDefault(environmentId, List.of()).size(), e);
        });
        return emitter;
    }

    private void removeEmitter(UUID environmentId, SseEmitter emitter) {
        List<SseEmitter> environmentEmitters = emitters.get(environmentId);
        if (environmentEmitters != null) {
            environmentEmitters.remove(emitter);
            if (environmentEmitters.isEmpty()) {
                emitters.remove(environmentId);
                log.info("All emitters removed for environmentId: {}", environmentId);
            }
        }
    }

    public void sendMessage(MessageEvent message) {
        UUID environmentId = message.getConversation().getEnvironment().getId();
        List<SseEmitter> environmentEmitters = emitters.get(environmentId);
        if (environmentEmitters != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : environmentEmitters) {
                try {
                    emitter.send(SseEmitter.event().name(message.getType()).data(message));
                    log.info("Message sent to environmentId: {}", environmentId);
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                    log.error("Failed to send message to emitter for environmentId: {}. Marking emitter as dead.", environmentId, e);
                }
            }
            environmentEmitters.removeAll(deadEmitters);
            log.info("Removed {} dead emitters for environmentId: {}", deadEmitters.size(), environmentId);
        } else {
            log.warn("No subscribers found for environmentId: {}. Message not sent.", environmentId);
        }
    }
}
