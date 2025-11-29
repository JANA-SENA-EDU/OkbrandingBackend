-- Ajustes para soportar comentarios, historial y montos de cotizaciones

-- 1. Actualizar tabla de cotizacion
ALTER TABLE cotizacion
    MODIFY COLUMN fecha_solicitud DATETIME NOT NULL,
    ADD COLUMN monto_total DECIMAL(14, 2) NOT NULL DEFAULT 0 AFTER id_usuario;

-- 2. Actualizar tabla detalle de productos de la cotizacion
ALTER TABLE cotizacion_producto
    ADD COLUMN precio_unitario DECIMAL(14, 2) NULL AFTER cantidad,
    ADD COLUMN subtotal DECIMAL(14, 2) NULL AFTER precio_unitario;
    

-- 3. Tabla de comentarios de la cotizacion
CREATE TABLE IF NOT EXISTS comentario_cotizacion (
    id_comentario INT AUTO_INCREMENT PRIMARY KEY,
    id_cotizacion INT NOT NULL,
    id_usuario INT NOT NULL,
    comentario VARCHAR(500) NOT NULL,
    fecha_comentario DATETIME NOT NULL,
    es_administrador BIT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_comentario_cotizacion_cotizacion FOREIGN KEY (id_cotizacion)
        REFERENCES cotizacion (id_cotizacion) ON DELETE CASCADE,
    CONSTRAINT fk_comentario_cotizacion_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

-- 4. Tabla de historial de movimientos de la cotizacion
CREATE TABLE IF NOT EXISTS historial_cotizacion (
    id_historial INT AUTO_INCREMENT PRIMARY KEY,
    id_cotizacion INT NOT NULL,
    estado_anterior VARCHAR(50),
    estado_nuevo VARCHAR(50) NOT NULL,
    usuario_accion VARCHAR(150),
    fecha_cambio DATETIME NOT NULL,
    descripcion_cambio VARCHAR(500),
    CONSTRAINT fk_historial_cotizacion FOREIGN KEY (id_cotizacion)
        REFERENCES cotizacion (id_cotizacion) ON DELETE CASCADE
);
