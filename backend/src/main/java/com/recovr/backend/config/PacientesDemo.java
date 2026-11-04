package com.recovr.backend.config;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Pacientes de demostración: nombres y apellidos frecuentes en el Perú combinados al azar (con semilla fija), así que
 * no corresponden a ninguna persona real. No llevan DNI; el correo usa el dominio reservado {@code example.com}
 * (RFC 2606, nunca entrega correo) y el teléfono es un número de ficción de la serie 900 000 xxx.
 */
final class PacientesDemo {

    static final String DOMINIO = "@example.com";

    private static final String[] NOMBRES = {
            "Carlos", "Luis", "Jorge", "Miguel", "José", "Diego", "Renato", "Mateo", "Rodrigo", "Álvaro",
            "Hugo", "Sebastián", "Ricardo", "Fernando", "Andrés", "Gonzalo", "Julio", "César", "Víctor", "Raúl",
            "Martín", "Gustavo", "Óscar", "Javier", "Bruno", "Gabriel", "Joaquín", "Franco", "Iván", "Marco",
            "Lucía", "Andrea", "Sofía", "Valeria", "Camila", "Daniela", "Fiorella", "Milagros", "Rosa", "Karina",
            "Patricia", "Ximena", "Gabriela", "Claudia", "Mariela", "Jimena"};
    private static final String[] APELLIDOS = {
            "Quispe", "Mendoza", "Flores", "Rojas", "Torres", "Huamán", "Castillo", "Vargas", "Chávez", "Ramírez",
            "Salazar", "Paredes", "Cárdenas", "Medina", "Gutiérrez", "Rivera", "Mamani", "Condori", "Ccori", "Ticona",
            "Apaza", "Choque", "Villanueva", "Espinoza", "Aguilar", "Valdivia", "Zegarra", "Benavides", "Palomino", "Cueva",
            "Llanos", "Arévalo", "Saavedra", "Ríos", "Gamarra", "Tello", "Ñahui", "Yupanqui", "Inga", "Sánchez"};

    record Paciente(String nombre, String correo, String telefono) {}

    private PacientesDemo() {}

    /** Los mismos {@code cantidad} pacientes en cada ejecución (misma semilla, mismo orden). */
    static List<Paciente> generar(int cantidad) {
        Random rnd = new Random(GeneradorAgendaDemo.SEMILLA + 1);
        List<Paciente> pacientes = new ArrayList<>(cantidad);
        for (int i = 1; i <= cantidad; i++) {
            String nombre = NOMBRES[rnd.nextInt(NOMBRES.length)];
            String apellido1 = APELLIDOS[rnd.nextInt(APELLIDOS.length)];
            String apellido2 = APELLIDOS[rnd.nextInt(APELLIDOS.length)];
            String correo = slug(nombre) + "." + slug(apellido1) + String.format(".%03d", i) + DOMINIO;
            String telefono = String.format("900000%03d", i);
            pacientes.add(new Paciente(nombre + " " + apellido1 + " " + apellido2, correo, telefono));
        }
        return pacientes;
    }

    /** "Ñahui" → "nahui", "Álvaro" → "alvaro". */
    static String slug(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
