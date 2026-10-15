package com.recovr.backend.service;

import com.recovr.backend.entity.FotoEmpleado;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.FotoEmpleadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Fotos de perfil de las terapeutas. La sube la propia terapeuta (con su consentimiento) o el ADMIN.
 * Seguridad: solo JPEG o PNG, verificados por su firma (no por el nombre ni el Content-Type que declare el cliente),
 * máximo 3 MB; la imagen se vuelve a codificar como JPEG de hasta 1000 px, lo que elimina metadatos (EXIF/GPS) y
 * cualquier contenido incrustado.
 */
@Service
public class FotoService {

    static final int MAX_BYTES = 3 * 1024 * 1024;
    static final int LADO_MAX = 1000;

    private final FotoEmpleadoRepository fotoRepository;
    private final EmpleadoRepository empleadoRepository;

    public FotoService(FotoEmpleadoRepository fotoRepository, EmpleadoRepository empleadoRepository) {
        this.fotoRepository = fotoRepository;
        this.empleadoRepository = empleadoRepository;
    }

    @Transactional
    public void guardar(Long empleadoId, byte[] original) {
        if (!empleadoRepository.existsById(empleadoId)) {
            throw new RecursoNoEncontradoException("Empleado", empleadoId);
        }
        if (original == null || original.length == 0) {
            throw new IllegalArgumentException("Selecciona una imagen");
        }
        if (original.length > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen supera los 3 MB permitidos");
        }
        if (!esJpeg(original) && !esPng(original)) {
            throw new IllegalArgumentException("Formato no permitido: usa una imagen JPG o PNG");
        }
        FotoEmpleado foto = fotoRepository.findById(empleadoId).orElseGet(FotoEmpleado::new);
        foto.setEmpleadoId(empleadoId);
        foto.setTipo("image/jpeg");
        foto.setDatos(recodificar(original));
        foto.setActualizadaEn(LocalDateTime.now());
        fotoRepository.save(foto);
    }

    @Transactional(readOnly = true)
    public FotoEmpleado obtener(Long empleadoId) {
        return fotoRepository.findById(empleadoId).orElseThrow(() -> new RecursoNoEncontradoException("Foto del empleado", empleadoId));
    }

    @Transactional
    public void eliminar(Long empleadoId) {
        fotoRepository.deleteById(empleadoId);
    }

    static boolean esJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    static boolean esPng(byte[] b) {
        return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    private static byte[] recodificar(byte[] original) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(original));
            if (src == null) {
                throw new IllegalArgumentException("El archivo no es una imagen válida");
            }
            double escala = Math.min(1.0, (double) LADO_MAX / Math.max(src.getWidth(), src.getHeight()));
            int w = Math.max(1, (int) Math.round(src.getWidth() * escala));
            int h = Math.max(1, (int) Math.round(src.getHeight() * escala));
            BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, w, h, java.awt.Color.WHITE, null);
            g.dispose();

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.85f);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream os = new MemoryCacheImageOutputStream(salida)) {
                writer.setOutput(os);
                writer.write(null, new IIOImage(rgb, null, null), param);
            } finally {
                writer.dispose();
            }
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo procesar la imagen");
        }
    }
}
