package com.clinica.clinica;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clinica.clinica.modelo.Paciente;
import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.PacienteRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tezza_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class FlujoLoginIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PacienteRepository pacientes;
    @Autowired BCryptPasswordEncoder encoder;

    @Test
    void pacienteAccedeSoloConSesionYPuedeSalir() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("ana@tezza.test");
        usuario.setContrasena(encoder.encode("ClaveSegura123"));
        usuario.setRol("PACIENTE");
        usuario = usuarios.save(usuario);

        Paciente paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellido("Pérez");
        paciente.setDni("12345678");
        paciente.setGenero("FEMENINO");
        paciente.setFechaNacimiento(LocalDate.of(1995, 5, 20));
        paciente.setUsuario(usuario);
        pacientes.save(paciente);

        mvc.perform(get("/PantallaPrincipal.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/InicioSesion.html"));
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/InicioSesion.html"));

        var login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@tezza.test\",\"password\":\"ClaveSegura123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destino").value("/PantallaPrincipal.html"))
                .andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"));
        mvc.perform(get("/PantallaPrincipal.html").session(session))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }
}
