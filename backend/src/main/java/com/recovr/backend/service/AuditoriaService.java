package com.recovr.backend.service;

import com.recovr.backend.entity.Auditoria;
import com.recovr.backend.repository.AuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Guarda y consulta la auditoría. Un fallo al auditar nunca debe romper la operación auditada. */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String usuario, String rol, String accion, String recurso, Integer estado, String ip, String detalle) {
        try {
            Auditoria a = new Auditoria();
            a.setFecha(LocalDateTime.now());
            a.setUsuario(recortar(usuario, 120));
            a.setRol(recortar(rol, 20));
            a.setAccion(recortar(accion, 40));
            a.setRecurso(recortar(recurso, 255));
            a.setEstado(estado);
            a.setIp(recortar(ip, 60));
            a.setDetalle(recortar(detalle, 255));
            auditoriaRepository.save(a);
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar la auditoría: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Auditoria> ultimos(int limite) {
        return auditoriaRepository.findAllByOrderByFechaDesc(PageRequest.of(0, Math.max(1, Math.min(limite, 500))));
    }

    private static String recortar(String s, int max) {
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }
}
