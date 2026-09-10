package com.bienestar.sistema_bienestar_universitario.service;

import com.bienestar.sistema_bienestar_universitario.model.Asistencia;
import com.bienestar.sistema_bienestar_universitario.model.Estudiante;
import com.bienestar.sistema_bienestar_universitario.model.Usuario;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReporteExportador {

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA =
            DateTimeFormatter.ofPattern("HH:mm");

    // Identificadores de columna por rol (orden de aparición)
    private static final List<String> COLS_ADMIN = List.of(
            "numero", "estudiante", "cedula", "correo", "sexo", "etnia",
            "facultad", "carrera", "area", "servicio", "estado", "primeraVez", "observaciones", "atendidoPor", "registro", "atencion", "cierre");

    private static final List<String> COLS_ENCARGADO = List.of(
            "numero", "estudiante", "cedula", "correo", "sexo", "etnia",
            "facultad", "carrera", "servicio", "estado", "primeraVez", "atendidoPor", "registro", "atencion", "cierre");

    private static final Map<String, String> ETIQUETAS = Map.ofEntries(
            Map.entry("numero", "N°"),
            Map.entry("estudiante", "Estudiante"),
            Map.entry("cedula", "Cédula"),
            Map.entry("correo", "Correo"),
            Map.entry("sexo", "Sexo"),
            Map.entry("etnia", "Etnia"),
            Map.entry("facultad", "Facultad"),
            Map.entry("carrera", "Carrera"),
            Map.entry("area", "Área"),
            Map.entry("servicio", "Servicio"),
            Map.entry("estado", "Estado"),
            Map.entry("primeraVez", "Primera vez"),
            Map.entry("observaciones", "Observaciones"),
            Map.entry("atendidoPor", "Atendido por"),
            Map.entry("registro", "Registro"),
            Map.entry("atencion", "Atención"),
            Map.entry("cierre", "Cierre"));

    // Columnas que siempre van aunque el usuario las desmarque
    private static final Set<String> FIJAS = Set.of("estudiante", "cedula");

    public static List<String> columnasEfectivas(boolean admin, List<String> seleccionadas) {
        List<String> base = admin ? COLS_ADMIN : COLS_ENCARGADO;
        Set<String> elegidas = (seleccionadas == null || seleccionadas.isEmpty())
                ? new HashSet<>(base) : new HashSet<>(seleccionadas);
        elegidas.addAll(FIJAS);
        List<String> resultado = new ArrayList<>();
        for (String id : base) {
            if (elegidas.contains(id)) {
                resultado.add(id);
            }
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public void exportarExcel(HttpServletResponse response, String titulo,
                              List<Asistencia> asistencias, boolean admin,
                              List<String> columnas) throws IOException {
        List<String> cols = columnasEfectivas(admin, columnas);

        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Reporte");

            CellStyle estiloCabecera = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            estiloCabecera.setFont(fuente);

            int r = 0;
            Row filaCabecera = hoja.createRow(r++);
            for (int c = 0; c < cols.size(); c++) {
                Cell celda = filaCabecera.createCell(c);
                celda.setCellValue(ETIQUETAS.getOrDefault(cols.get(c), cols.get(c)));
                celda.setCellStyle(estiloCabecera);
            }

            for (Asistencia a : asistencias) {
                Row fila = hoja.createRow(r++);
                for (int c = 0; c < cols.size(); c++) {
                    fila.createCell(c).setCellValue(valor(a, cols.get(c)));
                }
            }

            for (int i = 0; i < cols.size(); i++) {
                hoja.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=reporte.xlsx");
            libro.write(response.getOutputStream());
        }
    }

    // El PDF usa las mismas columnas seleccionadas que el Excel
    // Compatibilidad sin gráficas
    @Transactional(readOnly = true)
    public void exportarPdf(HttpServletResponse response, String titulo,
                            String elaboradoPor,
                            List<Asistencia> asistencias, boolean admin,
                            List<String> columnas) throws IOException {
        exportarPdf(response, titulo, elaboradoPor, asistencias, admin, columnas, null, null);
    }

    @Transactional(readOnly = true)
    public void exportarPdf(HttpServletResponse response, String titulo,
                            String elaboradoPor,
                            List<Asistencia> asistencias, boolean admin,
                            List<String> columnas,
                            List<String> graficasBase64,
                            List<String> graficasCategorias) throws IOException {

        List<String> cols = columnasEfectivas(admin, columnas);

        com.lowagie.text.Font tituloFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 12, new java.awt.Color(0x1B, 0x75, 0x05));
        com.lowagie.text.Font cabeceraFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 7, java.awt.Color.WHITE);
        com.lowagie.text.Font datoFont = FontFactory.getFont(FontFactory.HELVETICA, 7);
        com.lowagie.text.Font autorFont = FontFactory.getFont(
                FontFactory.HELVETICA, 8, new java.awt.Color(0x55, 0x55, 0x55));
        com.lowagie.text.Font seccionFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 11, new java.awt.Color(0x1B, 0x75, 0x05));

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reporte.pdf");

        Document documento = new Document(PageSize.A4.rotate(), 15, 15, 15, 15);
        PdfWriter.getInstance(documento, response.getOutputStream());
        documento.open();

        java.net.URL logoUrl = null;
        try {
            org.springframework.core.io.Resource logoResource =
                    new org.springframework.core.io.ClassPathResource("static/img/uteq-v2.png");
            if (logoResource.exists()) {
                logoUrl = logoResource.getURL();
            }
        } catch (java.io.IOException ignored) {
        }
        if (logoUrl != null) {
            try {
                Image logo = Image.getInstance(logoUrl);
                logo.scaleToFit(70, 70);
                documento.add(logo);
            } catch (Exception e) {
                documento.add(new Paragraph(" ",
                        FontFactory.getFont(FontFactory.HELVETICA, 6)));
            }
        }

        Paragraph tituloParrafo = new Paragraph(titulo, tituloFont);
        tituloParrafo.setSpacingBefore(6);
        tituloParrafo.setSpacingAfter(2);
        documento.add(tituloParrafo);

        Paragraph autorParrafo = new Paragraph("Elaborado por: " + elaboradoPor, autorFont);
        autorParrafo.setSpacingAfter(14);
        documento.add(autorParrafo);

        PdfPTable tabla = new PdfPTable(cols.size());
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.setSplitLate(false);
        tabla.setSplitRows(true);

        for (String id : cols) {
            PdfPCell celda = new PdfPCell(new Phrase(
                    ETIQUETAS.getOrDefault(id, id), cabeceraFont));
            celda.setBackgroundColor(new java.awt.Color(0x1B, 0x75, 0x05));
            celda.setPadding(3f);
            celda.setBorderWidth(0.5f);
            celda.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            celda.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
            tabla.addCell(celda);
        }
        for (Asistencia a : asistencias) {
            for (String id : cols) {
                PdfPCell celda = new PdfPCell(new Phrase(valor(a, id), datoFont));
                celda.setPadding(3f);
                celda.setBorderWidth(0.5f);
                celda.setNoWrap(false);
                tabla.addCell(celda);
            }
        }
        documento.add(tabla);

        // Gráficas opcionales en mismo PDF con salto de página
        if (graficasBase64 != null && !graficasBase64.isEmpty()) {
            for (int i = 0; i < graficasBase64.size(); i++) {
                String b64 = graficasBase64.get(i);
                String cat = graficasCategorias != null && i < graficasCategorias.size() ? graficasCategorias.get(i) : "Gráfica " + (i+1);
                if (b64 == null || b64.isBlank()) continue;
                try {
                    String clean = b64.contains(",") ? b64.substring(b64.indexOf(",")+1) : b64;
                    byte[] bytes = java.util.Base64.getDecoder().decode(clean);
                    Image img = Image.getInstance(bytes);
                    documento.newPage();
                    Paragraph sec = new Paragraph("Distribución por " + cat, seccionFont);
                    sec.setSpacingAfter(10);
                    documento.add(sec);
                    img.scaleToFit(750, 380);
                    img.setAlignment(Image.ALIGN_CENTER);
                    documento.add(img);
                } catch (Exception e) {
                    // ignorar gráfica corrupta
                }
            }
        }

        documento.close();
    }

    private String valor(Asistencia a, String id) {
        return switch (id) {
            case "numero" -> a.getTurnoTexto();
            case "estudiante" -> nombreEstudiante(a);
            case "cedula" -> cedula(a);
            case "correo" -> correo(a);
            case "sexo" -> sexo(a);
            case "etnia" -> etnia(a);
            case "facultad" -> facultad(a);
            case "carrera" -> carrera(a);
            case "area" -> area(a);
            case "servicio" -> servicio(a);
            case "estado" -> estadoTexto(a);
            case "primeraVez" -> primeraVez(a);
            case "observaciones" -> observaciones(a);
            case "atendidoPor" -> atendidoPor(a);
            case "registro" -> fechaHora(a.getFechaHoraRegistro());
            case "atencion" -> hora(a.getFechaHoraAtencion());
            case "cierre" -> hora(a.getFechaHoraCierre());
            default -> "-";
        };
    }

    private String nombreEstudiante(Asistencia a) {
        Estudiante e = a.getEstudiante();
        if (e == null) return "-";
        return (e.getNombres() != null ? e.getNombres() : "") + " " +
               (e.getApellidos() != null ? e.getApellidos() : "");
    }

    private String cedula(Asistencia a) {
        Estudiante e = a.getEstudiante();
        return e != null && e.getCedula() != null ? e.getCedula() : "-";
    }

    private String correo(Asistencia a) {
        Estudiante e = a.getEstudiante();
        return e != null && e.getCorreoInstitucional() != null
                ? e.getCorreoInstitucional() : "-";
    }

    private String sexo(Asistencia a) {
        Estudiante e = a.getEstudiante();
        return e != null && e.getSexo() != null && e.getSexo().getNombre() != null
                ? e.getSexo().getNombre() : "-";
    }

    private String etnia(Asistencia a) {
        Estudiante e = a.getEstudiante();
        return e != null && e.getEtnia() != null && e.getEtnia().getNombre() != null
                ? e.getEtnia().getNombre() : "-";
    }

    private String facultad(Asistencia a) {
        Estudiante e = a.getEstudiante();
        if (e != null && e.getCarrera() != null && e.getCarrera().getFacultad() != null && e.getCarrera().getFacultad().getNombre() != null) return e.getCarrera().getFacultad().getNombre();
        return "-";
    }
    private String carrera(Asistencia a) {
        Estudiante e = a.getEstudiante();
        if (e != null && e.getCarrera() != null && e.getCarrera().getNombre() != null) return e.getCarrera().getNombre();
        return "-";
    }
    private String observaciones(Asistencia a) {
        return a.getObservaciones() != null && !a.getObservaciones().isBlank() ? a.getObservaciones() : "-";
    }
    private String area(Asistencia a) {
        if (a.getServicio() != null && a.getServicio().getArea() != null
                && a.getServicio().getArea().getNombre() != null) {
            return a.getServicio().getArea().getNombre();
        }
        return "-";
    }

    private String servicio(Asistencia a) {
        return a.getServicio() != null && a.getServicio().getNombre() != null
                ? a.getServicio().getNombre() : "-";
    }

    private String estadoTexto(Asistencia a) {
        if (a.getEstado() == null) return "-";
        return switch (a.getEstado()) {
            case ATENDIDO -> "Atendido";
            case EN_ATENCION -> "En atención";
            case ESPERANDO -> "En espera";
            case NO_SE_PRESENTO -> "No se presentó";
            case CANCELADO -> "Cancelado";
            case PENDIENTE_VERIFICACION -> "Pendiente de verificación";
        };
    }

    private String primeraVez(Asistencia a) {
        return Boolean.TRUE.equals(a.getEsPrimeraVez()) ? "Sí" : "No";
    }

    private String atendidoPor(Asistencia a) {
        Usuario u = a.getUsuarioAtendio();
        if (u == null) return "-";
        return (u.getNombres() != null ? u.getNombres() : "") + " " +
               (u.getApellidos() != null ? u.getApellidos() : "");
    }

    private String fechaHora(LocalDateTime fecha) {
        return fecha != null ? fecha.format(FECHA_HORA) : "-";
    }

    private String hora(LocalDateTime fecha) {
        return fecha != null ? fecha.format(HORA) : "-";
    }
}
