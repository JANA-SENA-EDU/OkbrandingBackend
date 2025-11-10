package com.okBranding.back.service.impl;

import com.okBranding.back.dto.LoginRequestDTO;
import com.okBranding.back.dto.LoginResponseDTO;
import com.okBranding.back.dto.UsuarioRequestDTO;
import com.okBranding.back.dto.UsuarioResponseDTO;
import com.okBranding.back.models.Credencial;
import com.okBranding.back.models.Rol;
import com.okBranding.back.models.Usuario;
import com.okBranding.back.repository.CredencialRepository;
import com.okBranding.back.repository.RolRepository;
import com.okBranding.back.repository.UsuarioRepository;
import com.okBranding.back.security.CustomUserDetailsService;
import com.okBranding.back.security.JwtService;
import com.okBranding.back.service.UsuarioService;
import com.okBranding.back.util.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CredencialRepository credencialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * Metodo para registrar un Usuario
     * @param usuarioRequestDTO
     * @return
     */
    @Override
    public Map<String, Object> registrarUsuario(UsuarioRequestDTO usuarioRequestDTO) {
        Map<String, Object> respuesta = new HashMap<>();
        try {
            // Validar si el nombre de usuario ya existe
            boolean existeNombreUsuario = credencialRepository.existsByNombreUsuario(usuarioRequestDTO.getUsername());
            if (existeNombreUsuario) {
                respuesta.put("exitoso", false);
                respuesta.put("message", "El nombre de usuario ya existe, intente nuevamente");
                respuesta.put("data", null);
                return respuesta;
            }

            if (usuarioRepository.existsBytelefono(usuarioRequestDTO.getTelefono())) {
                respuesta.put("exitoso", false);
                respuesta.put("message", "El teléfono ya está registrado");
                return respuesta;
            }
            if (usuarioRepository.existsByNombre(usuarioRequestDTO.getNombre())) {
                respuesta.put("exitoso", false);
                respuesta.put("message", "El nombre ya está registrado");
                return respuesta;
            }
            // Encriptar contraseña
            String passwordEncriptada = passwordEncoder.encode(usuarioRequestDTO.getPassword());

            // Guardar credenciales
            Credencial credencial = new Credencial();
            credencial.setNombreUsuario(usuarioRequestDTO.getUsername());
            credencial.setPassword(passwordEncriptada);

            // Validar que el correo de usuario no este repetido
            boolean existeCorreo = usuarioRepository.existsByCorreo(usuarioRequestDTO.getCorreo());
            if (existeCorreo) {
                respuesta.put("exitoso", false);
                respuesta.put("message", "El correo diligenciado ya esta registrado, intente nuevamente");
                respuesta.put("data", null);
                return respuesta;
            }

            if(!existeCorreo && !existeNombreUsuario){
                credencial = credencialRepository.save(credencial);
            }

            // Guardar usuario
            Usuario usuario = new Usuario();
            usuario.setCorreo(usuarioRequestDTO.getCorreo());
            usuario.setNombre(usuarioRequestDTO.getNombre());
            usuario.setTelefono(usuarioRequestDTO.getTelefono());
            usuario.setCredencial(credencial);
            usuario = usuarioRepository.save(usuario);

            // Obtener rol CLIENTE
            Rol rolCliente = rolRepository.findByNombre(Constantes.ROL_CLIENTE)
                    .orElseThrow(() -> new RuntimeException(String.format("El rol %s no existe", Constantes.ROL_CLIENTE)));

            // Preparar respuesta
            UsuarioResponseDTO responseDTO = new UsuarioResponseDTO();
            responseDTO.setIdUsuario(usuario.getIdUsuario());
            responseDTO.setCorreo(usuario.getCorreo());
            responseDTO.setNombre(usuario.getNombre());
            responseDTO.setTelefono(usuario.getTelefono());
            responseDTO.setNombreUsuario(credencial.getNombreUsuario());
            responseDTO.setActivo(usuario.getActivo());

            respuesta.put("exitoso", true);
            respuesta.put("message", "Cuenta creada con exito");
            respuesta.put("data", responseDTO);

        } catch (Exception e) {
            respuesta.put("exitoso", false);
            respuesta.put("message", "Error al crear el usuario: " + e.getMessage());
            respuesta.put("data", null);
        }
        return respuesta;
    }

    /**
     * Metodo para loguer un Usuario
     * @param loginRequestDTO
     * @return
     */
    @Override
    public Map<String, Object> login(LoginRequestDTO loginRequestDTO) {
        Map<String, Object> respuesta = new HashMap<>();
        try {
            // Buscar las credenciales por nombre de usuario
            Optional<Credencial> credencialOpt = credencialRepository.findByNombreUsuario(loginRequestDTO.getUsername());

            if (credencialOpt.isEmpty()) {
                respuesta.put("exitoso", false);
                respuesta.put("message", "Nombre de Usuario incorrecto o no registrado");
                respuesta.put("data", null);
                return respuesta;
            }

            Credencial credencial = credencialOpt.get();

            // Verificar contraseña
            if (!passwordEncoder.matches(loginRequestDTO.getPassword(), credencial.getPassword())) {
                respuesta.put("exitoso", false);
                respuesta.put("message", "Contraseña incorrecta");
                respuesta.put("data", null);
                return respuesta;
            }

            // Obtener usuario asociado
            Usuario usuario = usuarioRepository.findByCredencial(credencial)
                    .orElseThrow(() -> new RuntimeException("No se encontró el usuario asociado"));

            if (!usuario.getActivo()) {
                respuesta.put("exitoso", false);
                respuesta.put("message", "El usuario no está activo");
                respuesta.put("data", null);
                return respuesta;
            }

            //Crear un UserDetails
            UserDetails userDetails = userDetailsService.loadUserByUsername(credencial.getNombreUsuario());
            // Generar JWT
            String token = jwtService.generateToken(userDetails);


            // Preparar respuesta
            LoginResponseDTO responseDTO = new LoginResponseDTO();
            responseDTO.setToken(token);
            responseDTO.setUserName(credencial.getNombreUsuario());
            responseDTO.setNombre(usuario.getNombre());
            responseDTO.setCorreo(usuario.getCorreo());
            responseDTO.setTelefono(usuario.getTelefono());

            respuesta.put("exitoso", true);
            respuesta.put("message", "Inicio de sesión exitoso");
            respuesta.put("data", responseDTO);

        } catch (Exception e) {
            respuesta.put("exitoso", false);
            respuesta.put("message", "Error en el login: " + e.getMessage());
            respuesta.put("data", null);
        }
        return respuesta;
    }

    @Override
    public List<UsuarioResponseDTO> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll() ;
        return usuarios.stream().map(usuario -> new UsuarioResponseDTO(
                usuario.getIdUsuario(), usuario.getCorreo(), usuario.getNombre(),
                usuario.getTelefono(), usuario.getCredencial().getNombreUsuario(), usuario.getActivo()
        )).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> actualizarUsuario(Integer idUsuario, UsuarioRequestDTO usuarioRequestDTO) {
        Map<String, Object> respuesta = new HashMap<>();
        try {
            Usuario usuario = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Credencial credencial = credencialRepository.findById(usuario.getCredencial().getIdCredencial())
                    .orElseThrow(() -> new RuntimeException("Credencial no encontrada"));

            // Verificar si el nombre de usuario ya existe en otra cuenta
            if (!credencial.getNombreUsuario().equals(usuarioRequestDTO.getUsername()) &&
                    credencialRepository.existsByNombreUsuario(usuarioRequestDTO.getUsername())) {
                respuesta.put("exitoso", false);
                respuesta.put("message", "El nombre de usuario ya existe, intente nuevamente");
                return respuesta;
            }

            // Validaciones adicionales
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

            // Actualizar datos del usuario
            usuario.setCorreo(usuarioRequestDTO.getCorreo());
            usuario.setNombre(usuarioRequestDTO.getNombre());
            usuario.setTelefono(usuarioRequestDTO.getTelefono());
            usuario.setActivo(usuarioRequestDTO.getActivo());

            if (usuarioRequestDTO.getUsername() != null && !usuarioRequestDTO.getUsername().isEmpty()){
                // Actualizar credenciales (usuario y opcionalmente contraseña)
                credencial.setNombreUsuario(usuarioRequestDTO.getUsername());
            }

            // Si se proporciona una nueva contraseña, encriptarla antes de actualizarla
            if (usuarioRequestDTO.getPassword() != null && !usuarioRequestDTO.getPassword().isEmpty()) {
                String nuevaContraseñaEncriptada = passwordEncoder.encode(usuarioRequestDTO.getPassword());
                credencial.setPassword(nuevaContraseñaEncriptada);
            }

            // Guardar cambios
            credencialRepository.save(credencial);
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
