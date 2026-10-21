
package com.clinica.clinica.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    @GetMapping({"/", "/inicio"})
    public String mostrarInicio() {
        return "redirect:/index.html";
    }
}
