package com.bienestar.sistema_bienestar_universitario.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/estudiante/registro";
    }
}
