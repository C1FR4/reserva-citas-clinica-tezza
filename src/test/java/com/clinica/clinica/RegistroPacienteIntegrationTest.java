package com.clinica.clinica;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import com.clinica.clinica.repositorio.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:registro_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "clinica.correo.habilitado=false"})
@AutoConfigureMockMvc
class RegistroPacienteIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PacienteRepository pacientes;
    @Autowired BCryptPasswordEncoder encoder;
    private static final String DATOS = """
        {"nombre":"Paciente","apellido":"Nuevo","correo":"persona@example.com",
         "dni":"11223344","fechaNacimiento":"1999-01-01","genero":"FEMENINO",
         "telefono":"999888777","contrasena":"Paciente2026!","confirmarContrasena":"Paciente2026!"}
        """;

    @BeforeEach void limpiar() { pacientes.deleteAll(); usuarios.deleteAll(); }

    @Test void registraPacienteConHashYPuedeIniciarSesion() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isCreated());
        var usuario = usuarios.findByNombreUsuario("persona@example.com").orElseThrow();
        assertEquals("PACIENTE", usuario.getRol());
        assertNotEquals("Paciente2026!", usuario.getContrasena());
        assertTrue(encoder.matches("Paciente2026!", usuario.getContrasena()));
        assertTrue(pacientes.findByUsuario(usuario).isPresent());
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"persona@example.com\",\"password\":\"Paciente2026!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.destino").value("/PantallaPrincipal.html"))
                .andReturn();
        var sesion = (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/admin").session(sesion)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/medico").session(sesion)).andExpect(status().is3xxRedirection());
    }

    @Test void noAceptaRolAdministradorOMedicoEnRegistroPublico() throws Exception {
        for (String rol : new String[]{"ADMINISTRADOR", "MEDICO"}) {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                    .content(DATOS.replace("{", "{\"rol\":\"" + rol + "\",")))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, usuarios.count());
    }

    @Test void rechazaClaveDebilYConfirmacionDistinta() throws Exception {
        for (String clave : new String[]{"abc", "paciente2026!", "PACIENTE2026!", "PacienteClave!", "Paciente2026", "Paciente 2026!"}) {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                    .content(DATOS.replace("Paciente2026!", clave))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(DATOS.replace("\"confirmarContrasena\":\"Paciente2026!\"", "\"confirmarContrasena\":\"OtraClave2026!\"")))
                .andExpect(status().isBadRequest());
        assertEquals(0, usuarios.count());
    }

    @Test void noDejaCuentaHuerfanaAlDuplicarDni() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(DATOS.replace("persona@example.com", "otra@example.com"))).andExpect(status().isConflict());
        assertEquals(1, usuarios.count());
        assertEquals(1, pacientes.count());
    }

    @Test void noDuplicaCorreoIgnorandoMayusculas() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(DATOS.replace("persona@example.com", "PERSONA@EXAMPLE.COM")))
                .andExpect(status().isBadRequest());
        assertEquals(1, usuarios.count());
    }

    @Test void rechazaDatosInvalidosSinCrearCuenta() throws Exception {
        for (String contenido : new String[]{DATOS.replace("persona@example.com", "admin"),
                DATOS.replace("11223344", "123"), DATOS.replace("1999-01-01", "2999-01-01"),
                DATOS.replace("999888777", "")}) {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(contenido))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, usuarios.count());
    }
}
