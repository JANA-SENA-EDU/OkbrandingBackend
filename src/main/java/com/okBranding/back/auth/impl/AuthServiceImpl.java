package com.okBranding.back.auth.impl;

import com.okBranding.back.auth.AuthRegisterRequest;
import com.okBranding.back.auth.AuthResponse;
import com.okBranding.back.auth.AuthService;
import com.okBranding.back.dto.LoginRequestDTO;
import com.okBranding.back.models.Credencial;
import com.okBranding.back.models.Rol;
import com.okBranding.back.models.Usuario;
import com.okBranding.back.repository.CredencialRepository;
import com.okBranding.back.repository.RolRepository;
import com.okBranding.back.repository.UsuarioRepository;
import com.okBranding.back.security.CustomUserDetailsService;
import com.okBranding.back.security.JwtService;
import com.okBranding.back.util.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CredencialRepository credencialRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * Registro de usuario (rol CLIENTE por defecto)
     */
    @Override
    public AuthResponse register(AuthRegisterRequest request) {
        // Validar duplicados
        if (credencialRepository.existsByNombreUsuario(request.getUsername())) {
            throw new RuntimeException("El nombre de usuario ya existe");
        }
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new RuntimeException("El correo ya está registrado");
        }

        // Crear y guardar credenciales
        Credencial credencial = new Credencial();
        credencial.setNombreUsuario(request.getUsername());
        credencial.setPassword(passwordEncoder.encode(request.getPassword()));
        credencial = credencialRepository.save(credencial);

        // Obtener rol CLIENTE
        Rol rolCliente = rolRepository.findByNombre(Constantes.ROL_CLIENTE)
                .orElseThrow(() -> new RuntimeException("El rol CLIENTE no existe"));

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setCorreo(request.getCorreo());
        usuario.setNombre(request.getNombre());
        usuario.setTelefono(request.getTelefono());
        usuario.setCredencial(credencial);
        usuario.setRol(rolCliente);
        usuarioRepository.save(usuario);

        // Generar token
        UserDetails userDetails = userDetailsService.loadUserByUsername(credencial.getNombreUsuario());
        String token = jwtService.generateToken(userDetails);

        // Devolver respuesta
        return new AuthResponse(token,
                credencial.getNombreUsuario(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRol().getNombre());
    }

    /**
     * Inicio de sesión y generación del JWT
     */
    @Override
    public AuthResponse login(LoginRequestDTO loginRequestDTO) {
        Credencial credencial = credencialRepository.findByNombreUsuario(loginRequestDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("Nombre de usuario incorrecto o no registrado"));

        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), credencial.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        Usuario usuario = usuarioRepository.findByCredencial(credencial)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario asociado"));

        if (!usuario.getActivo()) {
            throw new RuntimeException("El usuario no está activo");
        }

        // Generar token
        UserDetails userDetails = userDetailsService.loadUserByUsername(credencial.getNombreUsuario());
        String token = jwtService.generateToken(userDetails);

        // Devolver respuesta
        return new AuthResponse(token,
                credencial.getNombreUsuario(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRol().getNombre());
    }
}
