package com.okBranding.back.service.impl;

import com.okBranding.back.dto.CotizacionDTO;
import com.okBranding.back.dto.CotizacionProductoDTO;
import com.okBranding.back.dto.UsuarioCotizacionDTO;
import com.okBranding.back.dto.ComentarioCotizacionDTO;
import com.okBranding.back.dto.HistorialCotizacionDTO;
import com.okBranding.back.models.Cotizacion;
import com.okBranding.back.models.CotizacionProducto;
import com.okBranding.back.models.ComentarioCotizacion;
import com.okBranding.back.models.EstadoCotizacion;
import com.okBranding.back.models.HistorialCotizacion;
import com.okBranding.back.models.Producto;
import com.okBranding.back.models.Usuario;
import com.okBranding.back.repository.CotizacionRepository;
import com.okBranding.back.repository.EstadoCotizacionRepository;
import com.okBranding.back.repository.ProductoRepository;
import com.okBranding.back.repository.UsuarioRepository;
import com.okBranding.back.service.CotizacionService;
import com.okBranding.back.util.Constantes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CotizacionServiceImpl implements CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoCotizacionRepository estadoCotizacionRepository;

    public CotizacionServiceImpl(
            CotizacionRepository cotizacionRepository,
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            EstadoCotizacionRepository estadoCotizacionRepository) {
        this.cotizacionRepository = cotizacionRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.estadoCotizacionRepository = estadoCotizacionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CotizacionDTO> listarCotizaciones() {
        return cotizacionRepository.findAll().stream()
                .map(this::mapearCotizacion)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CotizacionDTO obtenerCotizacion(Integer id) {
        Cotizacion cotizacion = obtenerCotizacionOrThrow(id);
        return mapearCotizacion(cotizacion);
    }

    @Override
    @Transactional
    public CotizacionDTO crearCotizacion(CotizacionDTO cotizacionDTO) {
        Usuario cliente = obtenerUsuarioDesdeDto(cotizacionDTO, true);
        validarRol(cliente, Constantes.ROL_CLIENTE, "Solo un cliente puede crear cotizaciones.");

        if (cotizacionDTO.getProductos() == null || cotizacionDTO.getProductos().isEmpty()) {
            throw new IllegalArgumentException("Debe enviar al menos un producto para cotizar.");
        }

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setUsuario(cliente);
        cotizacion.setFechaSolicitud(new Date());
        cotizacion.setEstadoCotizacion(obtenerEstadoOrThrow(Constantes.ESTADO_COTIZACION_SOLICITADA));
        cotizacion.setMontoTotal(BigDecimal.ZERO);

        for (CotizacionProductoDTO productoDTO : cotizacionDTO.getProductos()) {
            if (productoDTO == null || productoDTO.getIdProducto() == null) {
                throw new IllegalArgumentException("Se recibio una linea de producto sin identificador.");
            }
            Producto producto = productoRepository.findById(productoDTO.getIdProducto())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con id " + productoDTO.getIdProducto()));
            Integer cantidad = productoDTO.getCantidad();
            if (cantidad == null || cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad es obligatoria y debe ser mayor que cero para el producto " + producto.getNombre());
            }
            CotizacionProducto detalle = new CotizacionProducto();
            detalle.setProducto(producto);
            detalle.setCotizacion(cotizacion);
            detalle.setCantidad(cantidad);
            List<String> coloresSeleccionados = limpiarColores(productoDTO.getColoresSeleccionados());
            if (!coloresSeleccionados.isEmpty()) {
                detalle.setColoresSeleccionados(serializarColores(coloresSeleccionados));
            }
            String comentarioCliente = productoDTO.getComentarioCliente();
            if (comentarioCliente != null && !comentarioCliente.isBlank()) {
                detalle.setComentarioCliente(comentarioCliente.trim());
            }
            cotizacion.getProductos().add(detalle);
        }

        registrarComentariosEntrada(cotizacion, cliente, cotizacionDTO.getComentarios());
        registrarHistorial(cotizacion, null, Constantes.ESTADO_COTIZACION_SOLICITADA, cliente,
                "El cliente registro la cotizacion.");

        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        return mapearCotizacion(guardada);
    }

    @Override
    @Transactional
    public CotizacionDTO responderCotizacion(Integer id, CotizacionDTO respuesta) {
        Cotizacion cotizacion = obtenerCotizacionOrThrow(id);
        String estadoActual = cotizacion.getEstadoCotizacion().getNombreEstadoCotizacion();
        if (!Constantes.ESTADO_COTIZACION_SOLICITADA.equalsIgnoreCase(estadoActual)
                && !Constantes.ESTADO_COTIZACION_AJUSTADA_CLIENTE.equalsIgnoreCase(estadoActual)) {
            throw new IllegalStateException("Solo se pueden responder cotizaciones en estado SOLICITADA o AJUSTADA_CLIENTE.");
        }

        Usuario administrador = obtenerUsuarioDesdeDto(respuesta, true);
        validarRol(administrador, Constantes.ROL_ADMINISTRADOR, "Solo un administrador puede responder cotizaciones.");

        Map<Integer, CotizacionProductoDTO> productosRespuesta = construirMapaProductos(respuesta.getProductos());
        if (productosRespuesta.isEmpty()) {
            throw new IllegalArgumentException("Debe enviar los precios para los productos incluidos en la cotizacion.");
        }

        BigDecimal total = BigDecimal.ZERO;
        Set<Integer> productosProcesados = new HashSet<>();
        for (CotizacionProducto detalle : cotizacion.getProductos()) {
            CotizacionProductoDTO detalleRespuesta = productosRespuesta.get(detalle.getProducto().getIdProducto());
            if (detalleRespuesta == null) {
                throw new IllegalArgumentException("Falta la informacion de precios para el producto " + detalle.getProducto().getNombre());
            }
            BigDecimal precioUnitario = detalleRespuesta.getPrecioUnitario();
            if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El precio unitario debe ser mayor que cero para el producto " + detalle.getProducto().getNombre());
            }
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(precioUnitario.multiply(BigDecimal.valueOf(detalle.getCantidad())));
            total = total.add(detalle.getSubtotal());
            productosProcesados.add(detalle.getProducto().getIdProducto());
        }

        if (productosProcesados.size() != productosRespuesta.size()) {
            throw new IllegalArgumentException("No se pueden agregar productos nuevos en la respuesta de la cotizacion.");
        }

        cotizacion.setMontoTotal(total);
        cotizacion.setEstadoCotizacion(obtenerEstadoOrThrow(Constantes.ESTADO_COTIZACION_RESPONDIDA));

        registrarComentariosEntrada(cotizacion, administrador, respuesta.getComentarios());
        registrarHistorial(cotizacion, estadoActual, Constantes.ESTADO_COTIZACION_RESPONDIDA, administrador,
                "El administrador respondio la cotizacion con los valores.");

        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        return mapearCotizacion(guardada);
    }

    @Override
    @Transactional
    public CotizacionDTO ajustarCotizacionCliente(Integer id, CotizacionDTO ajustes) {
        Cotizacion cotizacion = obtenerCotizacionOrThrow(id);
        String estadoActual = cotizacion.getEstadoCotizacion().getNombreEstadoCotizacion();
        if (!Constantes.ESTADO_COTIZACION_RESPONDIDA.equalsIgnoreCase(estadoActual)) {
            throw new IllegalStateException("Solo se pueden realizar ajustes cuando la cotizacion ha sido respondida por el administrador.");
        }

        Usuario actor = obtenerUsuarioDesdeDto(ajustes, false);
        if (actor == null) {
            actor = cotizacion.getUsuario();
        }
        if (!Objects.equals(actor.getIdUsuario(), cotizacion.getUsuario().getIdUsuario())) {
            throw new IllegalStateException("Solo el cliente que solicito la cotizacion puede ajustar los productos.");
        }

        Map<Integer, CotizacionProductoDTO> ajustesRecibidos = construirMapaProductos(ajustes != null ? ajustes.getProductos() : null);
        if (ajustesRecibidos.isEmpty()) {
            throw new IllegalArgumentException("Debe enviar la cantidad final de al menos un producto para aplicar los ajustes.");
        }

        BigDecimal total = BigDecimal.ZERO;
        Set<Integer> ajustesProcesados = new HashSet<>();
        Iterator<CotizacionProducto> iterator = cotizacion.getProductos().iterator();
        while (iterator.hasNext()) {
            CotizacionProducto detalle = iterator.next();
            CotizacionProductoDTO ajusteProducto = ajustesRecibidos.get(detalle.getProducto().getIdProducto());
            if (ajusteProducto == null) {
                BigDecimal precioUnitario = validarPrecioAsignado(detalle);
                detalle.setSubtotal(precioUnitario.multiply(BigDecimal.valueOf(detalle.getCantidad())));
                total = total.add(detalle.getSubtotal());
                continue;
            }
            ajustesProcesados.add(detalle.getProducto().getIdProducto());
            Integer nuevaCantidad = ajusteProducto.getCantidad();
            if (nuevaCantidad == null || nuevaCantidad < 0) {
                throw new IllegalArgumentException("La cantidad indicada para el producto " + detalle.getProducto().getNombre() + " no es valida.");
            }

            int cantidadActual = detalle.getCantidad();
            if (nuevaCantidad == 0) {
                detalle.setCotizacion(null);
                iterator.remove();
                continue;
            }

            if (nuevaCantidad < cantidadActual) {
                throw new IllegalArgumentException("Solo puedes eliminar el producto o aumentar su cantidad para " + detalle.getProducto().getNombre() + ".");
            }

            if (ajusteProducto.getColoresSeleccionados() != null) {
                List<String> coloresAjustados = limpiarColores(ajusteProducto.getColoresSeleccionados());
                detalle.setColoresSeleccionados(coloresAjustados.isEmpty() ? null : serializarColores(coloresAjustados));
            }
            if (ajusteProducto.getComentarioCliente() != null) {
                String comentario = ajusteProducto.getComentarioCliente().trim();
                detalle.setComentarioCliente(comentario.isBlank() ? null : comentario);
            }

            detalle.setCantidad(nuevaCantidad);
            BigDecimal precioUnitario = validarPrecioAsignado(detalle);
            detalle.setSubtotal(precioUnitario.multiply(BigDecimal.valueOf(nuevaCantidad)));
            total = total.add(detalle.getSubtotal());
        }

        if (ajustesProcesados.size() != ajustesRecibidos.size()) {
            throw new IllegalArgumentException("No se pueden agregar productos nuevos al ajuste del cliente.");
        }

        if (cotizacion.getProductos().isEmpty()) {
            throw new IllegalArgumentException("La cotizacion debe conservar al menos un producto para continuar.");
        }

        cotizacion.setMontoTotal(total);
        cotizacion.setEstadoCotizacion(obtenerEstadoOrThrow(Constantes.ESTADO_COTIZACION_AJUSTADA_CLIENTE));

        registrarComentariosEntrada(cotizacion, actor, ajustes != null ? ajustes.getComentarios() : null);
        registrarHistorial(cotizacion, estadoActual, Constantes.ESTADO_COTIZACION_AJUSTADA_CLIENTE, actor,
                "El cliente ajusto las cantidades de la cotizacion.");

        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        return mapearCotizacion(guardada);
    }

    @Override
    @Transactional
    public CotizacionDTO aceptarCotizacionCliente(Integer id, CotizacionDTO datos) {
        Cotizacion cotizacion = obtenerCotizacionOrThrow(id);
        String estadoActual = cotizacion.getEstadoCotizacion().getNombreEstadoCotizacion();
        if (!Constantes.ESTADO_COTIZACION_RESPONDIDA.equalsIgnoreCase(estadoActual)) {
            throw new IllegalStateException("Solo se pueden aceptar cotizaciones cuando el administrador ya respondio con los precios.");
        }
        Usuario actor = obtenerUsuarioDesdeDto(datos, false);
        if (actor == null) {
            actor = cotizacion.getUsuario();
        }
        if (!Objects.equals(actor.getIdUsuario(), cotizacion.getUsuario().getIdUsuario())) {
            throw new IllegalStateException("Solo el cliente que solicito la cotizacion puede aceptarla.");
        }

        cotizacion.setEstadoCotizacion(obtenerEstadoOrThrow(Constantes.ESTADO_COTIZACION_CERRADA));
        registrarComentariosEntrada(cotizacion, actor, datos != null ? datos.getComentarios() : null);
        registrarHistorial(cotizacion, estadoActual, Constantes.ESTADO_COTIZACION_CERRADA, actor,
                "El cliente acepto la cotizacion y autorizo la gestion del pedido.");

        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        return mapearCotizacion(guardada);
    }

    @Override
    @Transactional
    public CotizacionDTO cancelarCotizacionCliente(Integer id, CotizacionDTO datos) {
        Cotizacion cotizacion = obtenerCotizacionOrThrow(id);
        String estadoActual = cotizacion.getEstadoCotizacion().getNombreEstadoCotizacion();
        if (Constantes.ESTADO_COTIZACION_CERRADA.equalsIgnoreCase(estadoActual)
                || Constantes.ESTADO_COTIZACION_CANCELADA.equalsIgnoreCase(estadoActual)) {
            throw new IllegalStateException("La cotizacion ya no se puede cancelar.");
        }
        Usuario actor = obtenerUsuarioDesdeDto(datos, false);
        if (actor == null) {
            actor = cotizacion.getUsuario();
        }
        if (!Objects.equals(actor.getIdUsuario(), cotizacion.getUsuario().getIdUsuario())) {
            throw new IllegalStateException("Solo el cliente que solicito la cotizacion puede cancelarla.");
        }

        cotizacion.setEstadoCotizacion(obtenerEstadoOrThrow(Constantes.ESTADO_COTIZACION_CANCELADA));
        registrarComentariosEntrada(cotizacion, actor, datos != null ? datos.getComentarios() : null);
        registrarHistorial(
                cotizacion,
                estadoActual,
                Constantes.ESTADO_COTIZACION_CANCELADA,
                actor,
                "El cliente cancelo la cotizacion."
        );

        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        return mapearCotizacion(guardada);
    }

    private Cotizacion obtenerCotizacionOrThrow(Integer id) {
        return cotizacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cotizacion no encontrada con id " + id));
    }

    private EstadoCotizacion obtenerEstadoOrThrow(String nombreEstado) {
        return estadoCotizacionRepository.findByNombreEstadoCotizacion(nombreEstado)
                .orElseThrow(() -> new IllegalArgumentException("El estado de cotizacion " + nombreEstado + " no existe en la configuracion."));
    }

    private Usuario obtenerUsuarioDesdeDto(CotizacionDTO dto, boolean obligatorio) {
        if (dto == null || dto.getUsuario() == null || dto.getUsuario().getIdUsuario() == null) {
            if (obligatorio) {
                throw new IllegalArgumentException("Debe indicar el usuario que realiza la accion.");
            }
            return null;
        }
        Integer idUsuario = dto.getUsuario().getIdUsuario();
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id " + idUsuario));
    }

    private Map<Integer, CotizacionProductoDTO> construirMapaProductos(List<CotizacionProductoDTO> productos) {
        Map<Integer, CotizacionProductoDTO> mapa = new HashMap<>();
        if (productos == null) {
            return mapa;
        }
        for (CotizacionProductoDTO productoDTO : productos) {
            if (productoDTO == null || productoDTO.getIdProducto() == null) {
                continue;
            }
            mapa.put(productoDTO.getIdProducto(), productoDTO);
        }
        return mapa;
    }

    private CotizacionDTO mapearCotizacion(Cotizacion cotizacion) {
        CotizacionDTO dto = new CotizacionDTO();
        dto.setIdCotizacion(cotizacion.getIdCotizacion());
        dto.setFechaSolicitud(cotizacion.getFechaSolicitud());
        dto.setEstadoCotizacion(cotizacion.getEstadoCotizacion().getNombreEstadoCotizacion());
        dto.setMontoTotal(cotizacion.getMontoTotal());
        dto.setPermiteAjusteCliente(Constantes.ESTADO_COTIZACION_RESPONDIDA
                .equalsIgnoreCase(cotizacion.getEstadoCotizacion().getNombreEstadoCotizacion()));
        dto.setUsuario(mapearUsuario(cotizacion.getUsuario()));

        dto.setProductos(cotizacion.getProductos().stream()
                .map(this::mapearProducto)
                .collect(Collectors.toList()));

        dto.setComentarios(cotizacion.getComentarios().stream()
                .map(this::mapearComentario)
                .collect(Collectors.toList()));

        dto.setHistorial(cotizacion.getHistorial().stream()
                .map(this::mapearHistorial)
                .collect(Collectors.toList()));

        return dto;
    }

    private UsuarioCotizacionDTO mapearUsuario(Usuario usuario) {
        UsuarioCotizacionDTO dto = new UsuarioCotizacionDTO();
        dto.setIdUsuario(usuario.getIdUsuario());
        dto.setNombre(usuario.getNombre());
        dto.setCorreo(usuario.getCorreo());
        return dto;
    }

    private CotizacionProductoDTO mapearProducto(CotizacionProducto detalle) {
        CotizacionProductoDTO dto = new CotizacionProductoDTO();
        dto.setIdProducto(detalle.getProducto().getIdProducto());
        dto.setNombreProducto(detalle.getProducto().getNombre());
        dto.setCantidad(detalle.getCantidad());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setSubtotal(detalle.getSubtotal());
        dto.setColoresSeleccionados(deserializarColores(detalle.getColoresSeleccionados()));
        dto.setComentarioCliente(detalle.getComentarioCliente());
        return dto;
    }

    private ComentarioCotizacionDTO mapearComentario(ComentarioCotizacion comentario) {
        ComentarioCotizacionDTO dto = new ComentarioCotizacionDTO();
        dto.setIdComentario(comentario.getIdComentario());
        dto.setIdUsuario(comentario.getUsuario().getIdUsuario());
        dto.setNombreUsuario(comentario.getUsuario().getNombre());
        dto.setComentario(comentario.getComentario());
        dto.setFechaComentario(comentario.getFechaComentario());
        dto.setEsAdministrador(comentario.getEsAdministrador());
        return dto;
    }

    private HistorialCotizacionDTO mapearHistorial(HistorialCotizacion historial) {
        HistorialCotizacionDTO dto = new HistorialCotizacionDTO();
        dto.setIdHistorial(historial.getIdHistorial());
        dto.setEstadoAnterior(historial.getEstadoAnterior());
        dto.setEstadoNuevo(historial.getEstadoNuevo());
        dto.setUsuarioAccion(historial.getUsuarioAccion());
        dto.setFechaCambio(historial.getFechaCambio());
        dto.setDescripcionCambio(historial.getDescripcionCambio());
        return dto;
    }

    private void registrarComentariosEntrada(Cotizacion cotizacion, Usuario actor, List<ComentarioCotizacionDTO> comentarios) {
        if (comentarios == null || comentarios.isEmpty()) {
            return;
        }
        for (ComentarioCotizacionDTO comentarioDTO : comentarios) {
            if (comentarioDTO == null || comentarioDTO.getIdComentario() != null) {
                continue;
            }
            String texto = comentarioDTO.getComentario();
            if (texto == null || texto.isBlank()) {
                continue;
            }
            ComentarioCotizacion comentario = new ComentarioCotizacion();
            comentario.setCotizacion(cotizacion);
            comentario.setUsuario(actor != null ? actor : cotizacion.getUsuario());
            comentario.setComentario(texto.trim());
            comentario.setFechaComentario(new Date());
            comentario.setEsAdministrador(esAdministrador(actor));
            cotizacion.getComentarios().add(comentario);
        }
    }

    private void registrarHistorial(Cotizacion cotizacion, String estadoAnterior, String estadoNuevo, Usuario actor, String descripcion) {
        HistorialCotizacion historial = new HistorialCotizacion();
        historial.setCotizacion(cotizacion);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setUsuarioAccion(actor != null ? actor.getNombre() : "Sistema");
        historial.setFechaCambio(new Date());
        historial.setDescripcionCambio(descripcion);
        cotizacion.getHistorial().add(historial);
    }

    private void validarRol(Usuario usuario, String rolEsperado, String mensajeError) {
        if (usuario == null || usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            throw new IllegalArgumentException(mensajeError);
        }
        if (!rolEsperado.equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new IllegalArgumentException(mensajeError);
        }
    }

    private boolean esAdministrador(Usuario usuario) {
        return usuario != null
                && usuario.getRol() != null
                && Constantes.ROL_ADMINISTRADOR.equalsIgnoreCase(usuario.getRol().getNombre());
    }

    private BigDecimal validarPrecioAsignado(CotizacionProducto detalle) {
        if (detalle.getPrecioUnitario() == null) {
            throw new IllegalStateException("El producto " + detalle.getProducto().getNombre() + " aun no tiene precio asignado.");
        }
        return detalle.getPrecioUnitario();
    }

    private List<String> limpiarColores(List<String> colores) {
        if (colores == null) {
            return Collections.emptyList();
        }
        return colores.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private String serializarColores(List<String> colores) {
        return String.join(",", colores);
    }

    private List<String> deserializarColores(String data) {
        if (data == null || data.isBlank()) {
            return Collections.emptyList();
        }
        return java.util.Arrays.stream(data.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}
