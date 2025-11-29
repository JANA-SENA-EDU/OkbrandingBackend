package com.okBranding.back.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class CotizacionDTO {
    private Integer idCotizacion;
    private Date fechaSolicitud;
    private String estadoCotizacion;
    private BigDecimal montoTotal;
    private Boolean permiteAjusteCliente;
    private UsuarioCotizacionDTO usuario;
    private List<CotizacionProductoDTO> productos;
    private List<ComentarioCotizacionDTO> comentarios;
    private List<HistorialCotizacionDTO> historial;

    public Integer getIdCotizacion() {
        return idCotizacion;
    }

    public void setIdCotizacion(Integer idCotizacion) {
        this.idCotizacion = idCotizacion;
    }

    public Date getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(Date fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getEstadoCotizacion() {
        return estadoCotizacion;
    }

    public void setEstadoCotizacion(String estadoCotizacion) {
        this.estadoCotizacion = estadoCotizacion;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public Boolean getPermiteAjusteCliente() {
        return permiteAjusteCliente;
    }

    public void setPermiteAjusteCliente(Boolean permiteAjusteCliente) {
        this.permiteAjusteCliente = permiteAjusteCliente;
    }

    public UsuarioCotizacionDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioCotizacionDTO usuario) {
        this.usuario = usuario;
    }

    public List<CotizacionProductoDTO> getProductos() {
        return productos;
    }

    public void setProductos(List<CotizacionProductoDTO> productos) {
        this.productos = productos;
    }

    public List<ComentarioCotizacionDTO> getComentarios() {
        return comentarios;
    }

    public void setComentarios(List<ComentarioCotizacionDTO> comentarios) {
        this.comentarios = comentarios;
    }

    public List<HistorialCotizacionDTO> getHistorial() {
        return historial;
    }

    public void setHistorial(List<HistorialCotizacionDTO> historial) {
        this.historial = historial;
    }
}
