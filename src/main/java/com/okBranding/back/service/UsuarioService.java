package com.okBranding.back.service;

import com.okBranding.back.dto.UsuarioRequestDTO;
import com.okBranding.back.dto.UsuarioResponseDTO;

import java.util.List;
import java.util.Map;

public interface UsuarioService {

    /**
     * Listar todos los usuarios
     */
    List<UsuarioResponseDTO> listarUsuarios();

    /**
     * Actualizar un usuario
     */
    Map<String, Object> actualizarUsuario(Integer idUsuario, UsuarioRequestDTO usuarioRequestDTO);

    /**
     * Desactivar (eliminar lógico) un usuario
     */
    Map<String, Object> eliminarUsuario(Integer idUsuario);
}
