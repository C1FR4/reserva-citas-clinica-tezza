package com.clinica.clinica.controlador;

import com.clinica.clinica.servicio.AdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {
    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/admin")
    public String panel(@RequestParam(required = false) Integer medico, Model model,
                        HttpSession session) {
        if (!esAdministrador(session)) return "redirect:/InicioSesion.html";
        var medicos = admin.medicos();
        var horarios = admin.horarios();
        model.addAttribute("medicos", medicos);
        model.addAttribute("horarios", horarios);
        model.addAttribute("citas", admin.citas());
        model.addAttribute("totalMedicos", medicos.size());
        model.addAttribute("totalHorarios", horarios.size());
        model.addAttribute("totalCitas", admin.totalCitas());
        if (medico != null) model.addAttribute("medicoEditar", admin.medico(medico));
        return "admin";
    }

    @PostMapping("/admin/medicos/guardar")
    public String guardarMedico(@RequestParam(required = false) Integer idMedico,
                                @RequestParam String nombre,
                                @RequestParam String apellido,
                                @RequestParam String dni,
                                @RequestParam String correo,
                                @RequestParam String especialidad,
                                @RequestParam(required = false) String telefono,
                                @RequestParam(defaultValue = "ACTIVO") String estado,
                                RedirectAttributes mensajes, HttpSession session) {
        if (!esAdministrador(session)) return "redirect:/InicioSesion.html";
        try {
            admin.guardarMedico(idMedico, nombre, apellido, dni, correo, especialidad,
                    telefono, estado);
            mensajes.addFlashAttribute("mensajeExito", idMedico == null
                    ? "Médico registrado. Ahora puedes asignarle horarios."
                    : "Datos del médico actualizados.");
        } catch (IllegalArgumentException error) {
            mensajes.addFlashAttribute("mensajeError", error.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/horarios/guardar")
    public String agregarHorario(@RequestParam int idMedico, @RequestParam String dia,
                                 @RequestParam String horaInicio, @RequestParam String horaFin,
                                 RedirectAttributes mensajes, HttpSession session) {
        if (!esAdministrador(session)) return "redirect:/InicioSesion.html";
        try {
            admin.agregarHorario(idMedico, dia, horaInicio, horaFin);
            mensajes.addFlashAttribute("mensajeExito", "Horario asignado correctamente.");
        } catch (IllegalArgumentException error) {
            mensajes.addFlashAttribute("mensajeError", error.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/horarios/quitar")
    public String quitarHorario(@RequestParam int idHorario, RedirectAttributes mensajes,
                                HttpSession session) {
        if (!esAdministrador(session)) return "redirect:/InicioSesion.html";
        try {
            admin.quitarHorario(idHorario);
            mensajes.addFlashAttribute("mensajeExito", "Horario retirado.");
        } catch (IllegalArgumentException error) {
            mensajes.addFlashAttribute("mensajeError", error.getMessage());
        }
        return "redirect:/admin";
    }

    private boolean esAdministrador(HttpSession session) {
        return "ADMINISTRADOR".equals(session.getAttribute("rol"));
    }
}
