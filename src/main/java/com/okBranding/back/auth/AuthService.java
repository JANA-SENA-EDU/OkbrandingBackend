package com.okBranding.back.auth;

import com.okBranding.back.dto.LoginRequestDTO;

public interface AuthService {

    /**
     * Registro de nuevo usuario (rol CLIENTE por defecto)
     */
    AuthResponse register(AuthRegisterRequest request);

    /**
     * Inicio de sesión, validación de credenciales y generación de JWT
     */
    AuthResponse login(LoginRequestDTO loginRequestDTO);
}
