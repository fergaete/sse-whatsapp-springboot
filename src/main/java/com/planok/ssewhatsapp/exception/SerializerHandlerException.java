package com.planok.ssewhatsapp.exception;

public final class SerializerHandlerException extends RuntimeException {

    public SerializerHandlerException(String message) {
        super(String.format("Error al serializar el mensaje: %s", message));
    }

    public SerializerHandlerException(String message, Throwable cause) {
        super(String.format("Error al serializar el mensaje: %s", message), cause);
    }

    public SerializerHandlerException() {
        super("Error al serializar el mensaje");
    }
}
