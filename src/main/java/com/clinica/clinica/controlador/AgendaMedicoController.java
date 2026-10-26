package com.clinica.clinica.controlador;

import com.clinica.clinica.modelo.Medico;
import com.clinica.clinica.servicio.AgendaMedicoService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.sql.Date;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AgendaMedicoController {
    private final AgendaMedicoService agenda;

    public AgendaMedicoController(AgendaMedicoService agenda) {
        this.agenda = agenda;
    }

    @GetMapping("/medico")
    public String panel(HttpSession sesion, Model modelo) {
        Medico medico = medicoActual(sesion);
        if (medico == null) return "redirect:/InicioSesion.html";
        modelo.addAttribute("medico", medico);
        modelo.addAttribute("citas", agenda.citas(medico.getIdMedico()));
        return "medico";
    }

    @GetMapping("/medico/citas/{id}")
    public String detalle(@PathVariable int id, HttpSession sesion, Model modelo) {
        Medico medico = medicoActual(sesion);
        if (medico == null) return "redirect:/InicioSesion.html";
        var cita = agenda.cita(medico.getIdMedico(), id);
        if (cita == null) return "redirect:/medico";
        modelo.addAttribute("medico", medico);
        modelo.addAttribute("cita", cita);
        var fecha = ((Date) cita.get("fecha_orden")).toLocalDate();
        var hora = LocalTime.parse((String) cita.get("hora"));
        modelo.addAttribute("puedeRegistrar", "PENDIENTE".equals(cita.get("asistencia"))
                && !"CANCELADA".equals(cita.get("estado"))
                && !fecha.atTime(hora).isAfter(LocalDateTime.now()));
        return "medico-detalle";
    }

    @PostMapping("/medico/citas/{id}/asistencia")
    public String asistencia(@PathVariable int id, @RequestParam String asistencia,
                             HttpSession sesion, RedirectAttributes mensajes) {
        Medico medico = medicoActual(sesion);
        if (medico == null) return "redirect:/InicioSesion.html";
        try {
            agenda.registrarAsistencia(medico.getIdMedico(), id, asistencia);
            mensajes.addFlashAttribute("mensajeExito", "Asistencia registrada.");
        } catch (IllegalArgumentException error) {
            mensajes.addFlashAttribute("mensajeError", error.getMessage());
        }
        return "redirect:/medico/citas/" + id;
    }

    private Medico medicoActual(HttpSession sesion) {
        if (!"MEDICO".equals(sesion.getAttribute("rol"))) return null;
        Object id = sesion.getAttribute("usuarioId");
        if (!(id instanceof Integer idUsuario)) return null;
        Medico medico = agenda.medicoDeUsuario(idUsuario);
        return medico != null && "ACTIVO".equals(medico.getEstado()) ? medico : null;
    }
}
