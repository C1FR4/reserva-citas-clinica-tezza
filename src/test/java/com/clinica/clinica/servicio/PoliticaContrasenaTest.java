package com.clinica.clinica.servicio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PoliticaContrasenaTest {
    private final PoliticaContrasena politica = new PoliticaContrasena();

    @Test
    void exigeLasCincoCondiciones() {
        assertTrue(politica.esValida("Tezza2026!"));
        assertFalse(politica.esValida(null));
        assertFalse(politica.esValida("Tz26!"));
        assertFalse(politica.esValida("TEZZA2026!"));
        assertFalse(politica.esValida("tezza2026!"));
        assertFalse(politica.esValida("TezzaClave!"));
        assertFalse(politica.esValida("Tezza2026"));
        assertFalse(politica.esValida("Tezza 2026!"));
    }
}
