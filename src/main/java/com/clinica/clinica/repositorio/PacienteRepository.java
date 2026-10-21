package com.clinica.clinica.repositorio;

import com.clinica.clinica.modelo.Paciente;
import com.clinica.clinica.modelo.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Integer> {
    Optional<Paciente> findByUsuario(Usuario usuario);
    Optional<Paciente> findByUsuario_IdUsuario(Integer idUsuario);
}
