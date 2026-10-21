
package com.clinica.clinica.controlador;

import com.clinica.clinica.modelo.Medico;
import com.clinica.clinica.servicio.MedicoService;
import com.clinica.clinica.repositorio.MedicoRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final MedicoRepository medicoRepository;
    private final MedicoService medicoService;

    public AdminController(
            MedicoRepository medicoRepository,
            MedicoService medicoService) {

        this.medicoRepository = medicoRepository;
        this.medicoService = medicoService;
    }

    // Mostrar el panel de administración
    @GetMapping("/admin")
    public String mostrarPanel(Model model, HttpSession session) {

        if (!"ADMINISTRADOR".equals(session.getAttribute("rol"))) {
            return "redirect:/InicioSesion.html";
        }

        model.addAttribute("medicos", medicoRepository.findAll());

        if (!model.containsAttribute("medicoForm")) {
            model.addAttribute("medicoForm", new Medico());
        }

        return "admin";
    }

    // Registrar un médico y su cuenta de acceso
    @PostMapping("/admin/medicos/guardar")
    public String guardarMedico(
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String especialidad,
            @RequestParam String telefono,
            @RequestParam String dni,
            @RequestParam String correo,
            @RequestParam String nombreUsuario,
            @RequestParam String contrasena,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        // Comprobar que haya iniciado sesión un administrador
        if (!"ADMINISTRADOR".equals(session.getAttribute("rol"))) {
            return "redirect:/InicioSesion.html";
        }

        try {
            Medico medico = new Medico();

            medico.setNombre(nombre);
            medico.setApellido(apellido);
            medico.setEspecialidad(especialidad);
            medico.setTelefono(telefono);
            medico.setDni(dni);
            medico.setCorreo(correo);
            medico.setAniosExperiencia((short) 0);
            medico.setEstado("ACTIVO");

            medicoService.registrarMedico(
                    medico,
                    nombreUsuario,
                    contrasena);

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Médico registrado correctamente.");

            redirectAttributes.addFlashAttribute(
                    "usuarioCreado",
                    nombreUsuario);

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage());

        } catch (org.springframework.dao.DataIntegrityViolationException e) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No se pudo registrar el médico. Comprueba que el DNI "
                            + "y el nombre de usuario no estén repetidos.");
        }

        return "redirect:/admin";
    }
}
