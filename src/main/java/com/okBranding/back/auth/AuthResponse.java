package com.okBranding.back.auth;

public class AuthResponse {
    private Integer idUsuario;
    private String token;
    private String userName;
    private String nombre;
    private String correo;
    private String telefono;
    private String rol;

    public AuthResponse() {
    }

    public AuthResponse(Integer idUsuario, String token, String userName, String nombre, String correo, String telefono, String rol) {
        this.idUsuario = idUsuario;
        this.token = token;
        this.userName = userName;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.rol = rol;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
