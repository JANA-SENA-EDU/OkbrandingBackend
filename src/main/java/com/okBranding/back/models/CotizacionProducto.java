package com.okBranding.back.models;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cotizacion_producto")
public class CotizacionProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cotizacion_productos")
    private Integer idCotizacionProducto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "colores_seleccionados", length = 255)
    private String coloresSeleccionados;

    @Column(name = "comentario_cliente", length = 500)
    private String comentarioCliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cotizacion", nullable = false)
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    public Integer getIdCotizacionProducto() {
        return idCotizacionProducto;
    }

    public void setIdCotizacionProducto(Integer idCotizacionProducto) {
        this.idCotizacionProducto = idCotizacionProducto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getColoresSeleccionados() {
        return coloresSeleccionados;
    }

    public void setColoresSeleccionados(String coloresSeleccionados) {
        this.coloresSeleccionados = coloresSeleccionados;
    }

    public String getComentarioCliente() {
        return comentarioCliente;
    }

    public void setComentarioCliente(String comentarioCliente) {
        this.comentarioCliente = comentarioCliente;
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(Cotizacion cotizacion) {
        this.cotizacion = cotizacion;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }
}
