package pe.instruccionfacil.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import pe.instruccionfacil.entity.Pago;

public final class ReciboPdf {
    private static final Charset WIN_ANSI = Charset.forName("windows-1252");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ReciboPdf() {}

    public static byte[] crear(Pago pago, String alumno) {
        String titulo = "InstrucciónFácil";
        String medio = switch (pago.medio()) {
            case YAPE -> "Yape";
            case PLIN -> "Plin";
            case TRANSFERENCIA -> "Transferencia";
            case EFECTIVO -> "Efectivo";
            case MERCADO_PAGO -> "Mercado Pago";
        };
        String alumnoLinea = alumno.length() <= 72 ? alumno : alumno.substring(0, 72);
        String alumnoContinuacion = alumno.length() <= 72 ? "" : alumno.substring(72);
        var lineas = List.of(
            "Recibo de pago #" + pago.id(),
            "Cita #" + pago.citaId(),
            "Alumno: " + alumnoLinea,
            alumnoContinuacion,
            "Medio de pago: " + medio,
            "Referencia: " + pago.operacion(),
            "Importe pagado: S/ " + pago.monto().toPlainString(),
            "Fecha de aprobación: " + pago.revisado().format(FECHA) + " (Lima)",
            "Constancia interna de pago. Sin validez tributaria.");
        var contenido = new StringBuilder("1 .84 .08 rg 48 730 499 8 re f\n0 0 0 rg BT /F1 24 Tf 48 690 Td ");
        contenido.append(texto(titulo)).append(" Tj /F1 12 Tf");
        for (String linea : lineas) {
            if (!linea.isEmpty()) contenido.append("\n0 -25 Td ").append(texto(linea)).append(" Tj");
        }
        contenido.append(" ET\n");

        var objetos = List.of(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
            "<< /Length " + contenido.length() + " >>\nstream\n" + contenido + "endstream");

        var pdf = new ByteArrayOutputStream();
        escribir(pdf, "%PDF-1.4\n");
        int[] posiciones = new int[objetos.size()];
        for (int i = 0; i < objetos.size(); i++) {
            posiciones[i] = pdf.size();
            escribir(pdf, (i + 1) + " 0 obj\n" + objetos.get(i) + "\nendobj\n");
        }
        int xref = pdf.size();
        escribir(pdf, "xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n");
        for (int posicion : posiciones) {
            escribir(pdf, String.format(Locale.ROOT, "%010d 00000 n \n", posicion));
        }
        escribir(pdf, "trailer\n<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return pdf.toByteArray();
    }

    private static String texto(String valor) {
        return "<" + HexFormat.of().formatHex(valor.getBytes(WIN_ANSI)) + ">";
    }

    private static void escribir(ByteArrayOutputStream pdf, String valor) {
        pdf.writeBytes(valor.getBytes(StandardCharsets.US_ASCII));
    }
}
