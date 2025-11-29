package com.okBranding.back.service;

import com.okBranding.back.dto.CotizacionDTO;
import java.util.List;

public interface CotizacionService {
    List<CotizacionDTO> listarCotizaciones();
    CotizacionDTO obtenerCotizacion(Integer id);
    CotizacionDTO crearCotizacion(CotizacionDTO cotizacionDTO);
    CotizacionDTO responderCotizacion(Integer id, CotizacionDTO respuesta);
    CotizacionDTO ajustarCotizacionCliente(Integer id, CotizacionDTO ajustes);
    CotizacionDTO aceptarCotizacionCliente(Integer id, CotizacionDTO datos);
    CotizacionDTO cancelarCotizacionCliente(Integer id, CotizacionDTO datos);
}
