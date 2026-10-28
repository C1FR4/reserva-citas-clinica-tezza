package com.clinica.clinica.servicio;

public record CitaNotificable(int idCita, String tipo) {
    public static final String FICHA = "FICHA";
    public static final String CAMBIO = "CAMBIO";
    public static final String CANCELACION = "CANCELACION";
}
