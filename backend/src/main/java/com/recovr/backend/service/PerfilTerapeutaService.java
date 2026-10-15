package com.recovr.backend.service;

import com.recovr.backend.dto.PerfilTerapeutaDtos.*;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.FotoEmpleadoRepository;
import com.recovr.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Perfil propio de la terapeuta y listado público (solo activas) para el sitio web. */
@Service
public class PerfilTerapeutaService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final FotoEmpleadoRepository fotoRepository;
    private final FotoService fotoService;

    public PerfilTerapeutaService(UsuarioRepository usuarioRepository, EmpleadoRepository empleadoRepository,
                                  FotoEmpleadoRepository fotoRepository, FotoService fotoService) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.fotoRepository = fotoRepository;
        this.fotoService = fotoService;
    }

    @Transactional(readOnly = true)
    public MiPerfilResponse miPerfil(String correo) {
        return aRespuesta(propia(correo));
    }

    @Transactional
    public MiPerfilResponse actualizar(String correo, ActualizarMiPerfilRequest request) {
        Empleado e = propia(correo);
        e.setTelefono(request.telefono());
        e.setBio(request.bio());
        return aRespuesta(empleadoRepository.save(e));
    }

    @Transactional
    public MiPerfilResponse subirFoto(String correo, byte[] datos) {
        Empleado e = propia(correo);
        fotoService.guardar(e.getId(), datos);
        return aRespuesta(e);
    }

    @Transactional
    public MiPerfilResponse quitarFoto(String correo) {
        Empleado e = propia(correo);
        fotoService.eliminar(e.getId());
        return aRespuesta(e);
    }

    @Transactional(readOnly = true)
    public List<TerapeutaPublica> publicas() {
        Map<Long, Long> versiones = new HashMap<>();
        for (Object[] fila : fotoRepository.fechasDeFotos()) {
            versiones.put((Long) fila[0], ((java.time.LocalDateTime) fila[1]).atZone(ZoneId.systemDefault()).toEpochSecond());
        }
        return empleadoRepository.findAll().stream().filter(Empleado::isActivo)
                .sorted(java.util.Comparator.comparing(Empleado::getId))
                .map(e -> new TerapeutaPublica(e.getId(), e.getNombre(), e.getEspecialidad(), e.getBio(),
                        versiones.containsKey(e.getId()) ? "/api/publico/terapeutas/" + e.getId() + "/foto?v=" + versiones.get(e.getId()) : null))
                .toList();
    }

    private Empleado propia(String correo) {
        var usuario = usuarioRepository.findByCorreo(correo).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return empleadoRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("Tu usuario no está vinculado a ninguna terapeuta"));
    }

    private MiPerfilResponse aRespuesta(Empleado e) {
        return new MiPerfilResponse(e.getId(), e.getNombre(), e.getEspecialidad(), e.getTelefono(), e.getBio(), fotoRepository.existsById(e.getId()));
    }
}
