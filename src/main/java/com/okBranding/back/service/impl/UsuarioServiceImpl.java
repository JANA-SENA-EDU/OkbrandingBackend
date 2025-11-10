package com.okBranding.back.service.impl;

import com.okBranding.back.dto.UsuarioRequestDTO;
import com.okBranding.back.dto.UsuarioResponseDTO;
import com.okBranding.back.models.Usuario;
import com.okBranding.back.repository.UsuarioRepository;
import com.okBranding.back.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public List<UsuarioResponseDTO> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        return usuarios.stream()
                .map(usuario -> new UsuarioResponseDTO(
                        usuario.getIdUsuario(),
                        usuario.getCorreo(),
                        usuario.getNombre(),
                        usuario.getTelefono(),
                        usuario.getCredencial().getNombreUsuario(),
                        usuario.getActivo()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> actualizarUsuario(Integer idUsuario, UsuarioRequestDTO usuarioRequestDTO) {
        Map<String, Object> respuesta = new HashMap<>();
        try {
            Usuario usuario = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Validaciones de duplicados
            if (usuarioRepository.existsByCorreoAndIdUsuarioNot(usuarioRequestDTO.getCorreo(), idUsuario)) {
                respuesta.put("exitoso", false);
                respuesta.put("message", "El correo ya está registrado");
                return respuesta;
            }
            if (usuarioRepository.existsByTelefonoAndIdUsuarioNot(usuarioRequestDTO.getTelefono(), idUsuario)) {
                respuesta.put("exitoso", false);
                respuesta.put("message", "El teléfono ya está registrado");
                return respuesta;
            }
            if (usuarioRepository.existsByNombreAndIdUsuarioNot(usuarioRequestDTO.getNombre(), idUsuario)) {
                respuesta.put("exitoso", false);
                respuesta.put("message", "El nombre ya está registrado");
                return respuesta;
            }

            // Actualizar datos
            usuario.setCorreo(usuarioRequestDTO.getCorreo());
            usuario.setNombre(usuarioRequestDTO.getNombre());
            usuario.setTelefono(usuarioRequestDTO.getTelefono());
            usuario.setActivo(usuarioRequestDTO.getActivo());

            // Solo se actualizan datos de usuario (no credenciales)
            usuarioRepository.save(usuario);

            respuesta.put("exitoso", true);
            respuesta.put("message", "Usuario actualizado correctamente");
        } catch (Exception e) {
            respuesta.put("exitoso", false);
            respuesta.put("message", "Error al actualizar usuario: " + e.getMessage());
        }
        return respuesta;
    }

    @Override
    public Map<String, Object> eliminarUsuario(Integer idUsuario) {
        Map<String, Object> respuesta = new HashMap<>();
        try {
            Usuario usuario = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            usuario.setActivo(false);
            usuarioRepository.save(usuario);

            respuesta.put("exitoso", true);
            respuesta.put("message", "Usuario desactivado correctamente");
        } catch (Exception e) {
            respuesta.put("exitoso", false);
            respuesta.put("message", "Error al eliminar usuario: " + e.getMessage());
        }
        return respuesta;
    }
}