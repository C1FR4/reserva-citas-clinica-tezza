
package com.clinica.clinica.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clinica.clinica.modelo.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

}
