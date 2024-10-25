package com.planok.ssewhatsapp.controller;

import com.planok.ssewhatsapp.service.SseEmitterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
public final class SseController {

    private final SseEmitterService sseEmitterService;

    public SseController(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    @GetMapping(value = "/sse/{environmentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSse(@PathVariable UUID environmentId) {
        return sseEmitterService.subscribe(environmentId);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<String> handleAsyncRequestTimeoutException() {
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("La solicitud ha expirado");
    }
}
