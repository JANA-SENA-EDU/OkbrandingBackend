package com.okBranding.back.repository;

import com.okBranding.back.models.EstadoCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoCotizacionRepository extends JpaRepository<EstadoCotizacion, Integer> {
    Optional<EstadoCotizacion> findByNombreEstadoCotizacion(String nombreEstadoCotizacion);
}
