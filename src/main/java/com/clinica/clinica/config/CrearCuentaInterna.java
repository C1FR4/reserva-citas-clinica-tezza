package com.clinica.clinica.config;

import com.clinica.clinica.servicio.CuentaInternaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cuenta-interna")
public class CrearCuentaInterna implements CommandLineRunner {
    private final CuentaInternaService cuentas;
    @Value("${CUENTA_ROL:}") private String rol;
    @Value("${CUENTA_USUARIO:}") private String usuario;
    @Value("${CUENTA_PASSWORD:}") private String clave;
    @Value("${CUENTA_DNI:}") private String dni;
    @Value("${CUENTA_NOMBRE:}") private String nombre;
    @Value("${CUENTA_APELLIDO:}") private String apellido;
    @Value("${CUENTA_CORREO:}") private String correo;

    public CrearCuentaInterna(CuentaInternaService cuentas) { this.cuentas = cuentas; }

    @Override
    public void run(String... args) {
        cuentas.crear(rol, usuario, clave, dni, nombre, apellido, correo);
        System.out.println("Cuenta interna creada correctamente con rol " + rol + ". La contraseña no se muestra.");
    }
}
