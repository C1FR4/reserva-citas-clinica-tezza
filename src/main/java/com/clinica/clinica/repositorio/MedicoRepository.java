
package com.clinica.clinica.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clinica.clinica.modelo.Medico;
import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico, Integer> {
    Optional<Medico> findByUsuario_IdUsuario(Integer idUsuario);
    Optional<Medico> findByDni(String dni);
}
