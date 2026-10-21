package com.clinica.clinica.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegacyPageController {
    @GetMapping({"/Oftalmologia.html", "/Medico.html"})
    public String especialistas() {
        return "redirect:/Especialista.html?v=3";
    }

    @GetMapping("/DetalleReserva.html")
    public String detalleReserva() {
        return "redirect:/MisCitas.html";
    }

    @GetMapping("/VistaAdministrador.html")
    public String vistaAdministrador() {
        return "redirect:/admin";
    }
}
