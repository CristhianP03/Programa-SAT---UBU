package com.bienestar.sistema_bienestar_universitario.controller;

import com.bienestar.sistema_bienestar_universitario.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    
    // -------------------------------------------------------
    // GET /auth/login
    // Muestra la pantalla de login del personal administrativo.
    // Si viene con ?error muestra mensaje de credenciales incorrectas.
    // Si viene con ?logout muestra mensaje de cierre de sesión.
    // -------------------------------------------------------
    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error",  required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "perfil", required = false) String perfil,
            Model model) {

        if (error  != null) model.addAttribute("error",  "Usuario o contraseña incorrectos.");
        if (logout != null) model.addAttribute("mensaje", "Sesión cerrada correctamente.");
        if (perfil != null) model.addAttribute("mensaje",
                "Perfil actualizado. Inicia sesión con tu nuevo usuario.");

        return "auth/login";
    }


}