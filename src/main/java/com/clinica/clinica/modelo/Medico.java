
package com.clinica.clinica.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "medico")
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_MEDICO")
    private Integer idMedico;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "especialidad", nullable = false, length = 100)
    private String especialidad;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "dni", nullable = false, unique = true, length = 8)
    private String dni;

    @Column(name = "anios_experiencia", nullable = false)
    private Short aniosExperiencia = 0;

    @Column(name = "areas_especializacion", length = 255)
    private String areasEspecializacion;

    @Column(name = "correo", nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "estado", nullable = false, length = 10)
    private String estado = "ACTIVO";

    @OneToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    // construcor requerido JPA
    public Medico() {
    }

    public Integer getIdMedico() {
        return idMedico;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public Short getAniosExperiencia() { return aniosExperiencia; }
    public void setAniosExperiencia(Short aniosExperiencia) { this.aniosExperiencia = aniosExperiencia; }
    public String getAreasEspecializacion() { return areasEspecializacion; }
    public void setAreasEspecializacion(String areasEspecializacion) { this.areasEspecializacion = areasEspecializacion; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
