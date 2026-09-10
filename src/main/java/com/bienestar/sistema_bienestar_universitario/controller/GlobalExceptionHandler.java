package com.bienestar.sistema_bienestar_universitario.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        log.warn("Validación: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error/generico";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(IllegalStateException ex, Model model) {
        log.warn("Estado inválido: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error/generico";
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public String handleOptimisticLock(org.springframework.orm.ObjectOptimisticLockingFailureException ex, Model model) {
        log.warn("Conflicto de concurrencia: {}", ex.getMessage());
        model.addAttribute("error", "Otro proceso actualizó la configuración al mismo tiempo. Recarga e intenta nuevamente.");
        return "error/generico";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Error inesperado", ex);
        model.addAttribute("error", "Ocurrió un error inesperado. Intenta nuevamente.");
        return "error/generico";
    }
}
