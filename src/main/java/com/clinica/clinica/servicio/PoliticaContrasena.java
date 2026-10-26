package com.clinica.clinica.servicio;

import org.springframework.stereotype.Component;

/** Reglas comunes para las contraseñas de las cuentas de la clínica. */
@Component
public class PoliticaContrasena {
    public boolean esValida(String contrasena) {
        if (contrasena == null || contrasena.length() < 8 ||
                contrasena.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        boolean minuscula = false;
        boolean mayuscula = false;
        boolean digito = false;
        boolean simbolo = false;
        for (char caracter : contrasena.toCharArray()) {
            minuscula |= Character.isLowerCase(caracter);
            mayuscula |= Character.isUpperCase(caracter);
            digito |= Character.isDigit(caracter);
            simbolo |= !Character.isLetterOrDigit(caracter);
        }
        return minuscula && mayuscula && digito && simbolo;
    }
}
