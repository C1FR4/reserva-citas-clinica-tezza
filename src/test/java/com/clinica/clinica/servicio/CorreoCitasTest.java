package com.clinica.clinica.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.*;
import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionTemplate;

class CorreoCitasTest {
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    JavaMailSender mail;
    PlanificadorCorreo plan;
    EnvioCorreoService envio;
    Clock reloj;

    @BeforeEach void preparar() {
        var fuente = new DriverManagerDataSource("jdbc:h2:mem:" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(fuente);
        tx = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        jdbc.execute("CREATE TABLE usuario(id_usuario INT PRIMARY KEY, usuario VARCHAR(120))");
        jdbc.execute("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,id_usuario INT,nombre VARCHAR(80))");
        jdbc.execute("CREATE TABLE medico(id_medico INT PRIMARY KEY,nombre VARCHAR(80),apellido VARCHAR(100),especialidad VARCHAR(100))");
        jdbc.execute("CREATE TABLE cita(id_cita INT PRIMARY KEY,id_paciente INT,id_medico INT,fecha DATE,hora TIME,estado VARCHAR(20),tipo_consulta VARCHAR(12),version_notificacion INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE recordatorio(id_recordatorio INT AUTO_INCREMENT PRIMARY KEY,id_cita INT,tipo VARCHAR(20),version_cita INT,mensaje VARCHAR(255),programado_para TIMESTAMP,proximo_intento TIMESTAMP,fecha_envio TIMESTAMP,intentos INT DEFAULT 0,estado VARCHAR(20) DEFAULT 'PENDIENTE', UNIQUE(id_cita,version_cita,tipo))");
        jdbc.update("INSERT INTO usuario VALUES(1,'paciente@example.com')");
        jdbc.update("INSERT INTO paciente VALUES(1,1,'Paciente')");
        jdbc.update("INSERT INTO medico VALUES(1,'Ana','Vargas','Cardiología')");
        jdbc.update("INSERT INTO cita VALUES(1,1,1,'2026-10-30','10:00:00','RESERVADA','PRESENCIAL',0)");
        reloj = Clock.fixed(Instant.parse("2026-10-28T14:00:00Z"), ZoneId.of("America/Lima"));
        mail = mock(JavaMailSender.class);
        plan = new PlanificadorCorreo(jdbc, reloj);
        envio = new EnvioCorreoService(jdbc, mail, reloj, "clinica@example.com");
    }

    void planificar(String tipo) { tx.executeWithoutResult(s -> plan.planificar(new CitaNotificable(1,tipo))); }
    int id(String tipo) { return jdbc.queryForObject("SELECT id_recordatorio FROM recordatorio WHERE tipo=? ORDER BY id_recordatorio DESC LIMIT 1", Integer.class,tipo); }
    void enviar(int id) { tx.executeWithoutResult(s -> envio.enviar(id,1)); }

    @Test void programaFichaYRecordatorioExactamente24HorasAntes() {
        planificar("FICHA");
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM recordatorio",Integer.class));
        assertEquals(LocalDateTime.parse("2026-10-29T10:00:00"),jdbc.queryForObject("SELECT programado_para FROM recordatorio WHERE tipo='RECORDATORIO'",Timestamp.class).toLocalDateTime());
        enviar(id("RECORDATORIO"));
        verifyNoInteractions(mail);
        enviar(id("FICHA"));
        var mensaje=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(mensaje.capture());
        assertTrue(mensaje.getValue().getText().contains("30/10/2026 10:00"));
        assertTrue(mensaje.getValue().getText().contains("Ana Vargas"));
    }

    @Test void ejecutaRecordatorioAlLlegarElPlazoYSinDuplicar() {
        planificar("FICHA");
        envio=new EnvioCorreoService(jdbc,mail,Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"),reloj.getZone()),"clinica@example.com");
        int id=id("RECORDATORIO");
        enviar(id); enviar(id);
        verify(mail,times(1)).send(any(SimpleMailMessage.class));
        assertEquals("ENVIADO",jdbc.queryForObject("SELECT estado FROM recordatorio WHERE id_recordatorio=?",String.class,id));
    }

    @Test void cambioSustituyeNotificacionesPendientesYReprograma() {
        planificar("FICHA");
        jdbc.update("UPDATE cita SET fecha='2026-11-01',hora='11:00:00' WHERE id_cita=1");
        planificar("CAMBIO");
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM recordatorio WHERE tipo='FICHA'",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM recordatorio",Integer.class));
        assertEquals(LocalDateTime.parse("2026-10-31T11:00:00"),jdbc.queryForObject("SELECT programado_para FROM recordatorio WHERE tipo='RECORDATORIO'",Timestamp.class).toLocalDateTime());
    }

    @Test void cancelacionRetiraRecordatorioYNotificaSinInvitarAAsistir() {
        planificar("FICHA");
        int anterior=id("RECORDATORIO");
        jdbc.update("UPDATE cita SET estado='CANCELADA' WHERE id_cita=1");
        planificar("CANCELACION");
        enviar(anterior); enviar(id("CANCELACION"));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM recordatorio",Integer.class));
        var mensaje=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(mensaje.capture());
        assertTrue(mensaje.getValue().getText().contains("ya no está activa"));
    }

    @Test void fallaSmtpPersisteYReintentaDespuesSinPerderLaCita() {
        planificar("FICHA"); int id=id("FICHA");
        doThrow(new MailSendException("offline")).when(mail).send(any(SimpleMailMessage.class));
        enviar(id); enviar(id);
        assertEquals("FALLIDO",jdbc.queryForObject("SELECT estado FROM recordatorio WHERE id_recordatorio=?",String.class,id));
        verify(mail,times(1)).send(any(SimpleMailMessage.class));
        reset(mail);
        envio=new EnvioCorreoService(jdbc,mail,Clock.offset(reloj,Duration.ofMinutes(2)),"clinica@example.com");
        enviar(id);
        assertEquals("ENVIADO",jdbc.queryForObject("SELECT estado FROM recordatorio WHERE id_recordatorio=?",String.class,id));
    }

    @Test void reservaConMenosDe24HorasSoloRecibeFicha() {
        jdbc.update("UPDATE cita SET fecha='2026-10-28',hora='15:00:00' WHERE id_cita=1");
        planificar("FICHA");
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM recordatorio",Integer.class));
        enviar(id("FICHA")); verify(mail).send(any(SimpleMailMessage.class));
    }

    @Test void noEnviaRecordatoriosDeCitasYaPasadasNiAVersionAntigua() {
        planificar("FICHA");
        envio=new EnvioCorreoService(jdbc,mail,Clock.offset(reloj,Duration.ofDays(4)),"clinica@example.com");
        enviar(id("RECORDATORIO")); verifyNoInteractions(mail);
        jdbc.update("UPDATE cita SET version_notificacion=99 WHERE id_cita=1");
        enviar(id("FICHA")); verifyNoInteractions(mail);
    }

    @Test void rollbackNoDejaMensajesParaUnaOperacionFallida() {
        tx.executeWithoutResult(s -> { plan.planificar(new CitaNotificable(1,"FICHA")); s.setRollbackOnly(); });
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM recordatorio",Integer.class));
    }

    @Test void dosProcesadoresNoEnvianDosVecesElMismoMensaje() throws Exception {
        planificar("FICHA"); int id=id("FICHA");
        try(var tareas=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=tareas.submit(() -> enviar(id)); var b=tareas.submit(() -> enviar(id));
            a.get(); b.get();
        }
        verify(mail,times(1)).send(any(SimpleMailMessage.class));
    }
}
