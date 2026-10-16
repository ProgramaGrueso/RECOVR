package com.recovr.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

/** Una imagen liviana en bytes puede declarar dimensiones enormes y agotar la memoria al decodificarse. */
class FotoServiceTest {

    private static byte[] png(int ancho, int alto) throws IOException {
        // 1 bit por píxel: 6000 × 6000 comprime a unos pocos KB pero decodificada a RGB ocuparía 144 MB
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_BYTE_BINARY);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    @DisplayName("Rechaza por sus dimensiones una imagen liviana en bytes pero gigante en píxeles, sin decodificarla")
    void rechazaPixelFlood() throws IOException {
        byte[] bomba = png(6000, 6000);
        assertThat(bomba.length).as("el archivo es pequeño: pasa el tope de 3 MB").isLessThan(FotoService.MAX_BYTES);
        assertThatThrownBy(() -> FotoService.validarDimensiones(bomba))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demasiado grande");
    }

    @Test
    @DisplayName("Acepta una foto normal")
    void aceptaFotoNormal() throws IOException {
        byte[] normal = png(800, 1000);
        assertThatCode(() -> FotoService.validarDimensiones(normal)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rechaza un archivo que no es una imagen")
    void rechazaNoImagen() {
        assertThatThrownBy(() -> FotoService.validarDimensiones("no soy una imagen".getBytes()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
