package com.okBranding.back.controller;

import com.okBranding.back.dto.LoginRequestDTO;
import com.okBranding.back.dto.UsuarioRequestDTO;
import com.okBranding.back.dto.UsuarioResponseDTO;
import com.okBranding.back.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/okBranding/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    UsuarioService usuarioService;

    @GetMapping("/listar")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        List<UsuarioResponseDTO> usuarios = usuarioService.listarUsuarios();
        return ResponseEntity.ok(usuarios);
    }

    @PutMapping("/actualizar/{idUsuario}")
    public ResponseEntity<Map<String, Object>> actualizarUsuario(
            @PathVariable Integer idUsuario,
            @RequestBody UsuarioRequestDTO usuarioRequestDTO) {

        Map<String, Object> respuesta = usuarioService.actualizarUsuario(idUsuario, usuarioRequestDTO);
        return (boolean) respuesta.get("exitoso")
                ? ResponseEntity.ok(respuesta)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    @DeleteMapping("/eliminar/{idUsuario}")
    public ResponseEntity<Map<String, Object>> eliminarUsuario(@PathVariable Integer idUsuario) {
        Map<String, Object> respuesta = usuarioService.eliminarUsuario(idUsuario);
        return (boolean) respuesta.get("exitoso")
                ? ResponseEntity.ok(respuesta)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }
}
