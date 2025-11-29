package com.okBranding.back.controller;

import com.okBranding.back.dto.CotizacionDTO;
import com.okBranding.back.service.CotizacionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/okBranding/cotizaciones")
@CrossOrigin(origins = "*")
public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @GetMapping
    public List<CotizacionDTO> listarCotizaciones() {
        return cotizacionService.listarCotizaciones();
    }

    @GetMapping("/{id}")
    public CotizacionDTO obtenerCotizacion(@PathVariable Integer id) {
        return cotizacionService.obtenerCotizacion(id);
    }

    @PostMapping
    public CotizacionDTO crearCotizacion(@RequestBody CotizacionDTO cotizacionDTO) {
        return cotizacionService.crearCotizacion(cotizacionDTO);
    }

    @PutMapping("/{id}/respuesta")
    public CotizacionDTO responderCotizacion(@PathVariable Integer id, @RequestBody CotizacionDTO respuesta) {
        return cotizacionService.responderCotizacion(id, respuesta);
    }

    @PutMapping("/{id}/ajustes-cliente")
    public CotizacionDTO ajustarCotizacionCliente(@PathVariable Integer id, @RequestBody CotizacionDTO ajustes) {
        return cotizacionService.ajustarCotizacionCliente(id, ajustes);
    }

    @PutMapping("/{id}/aceptacion-cliente")
    public CotizacionDTO aceptarCotizacionCliente(@PathVariable Integer id, @RequestBody CotizacionDTO datos) {
        return cotizacionService.aceptarCotizacionCliente(id, datos);
    }

    @PutMapping("/{id}/cancelacion-cliente")
    public CotizacionDTO cancelarCotizacionCliente(@PathVariable Integer id, @RequestBody CotizacionDTO datos) {
        return cotizacionService.cancelarCotizacionCliente(id, datos);
    }
}
