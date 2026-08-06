package com.visium.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.visium.backend.entity.RecetaOptica;
import com.visium.backend.entity.RecetaOpticaDetalle;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Service
public class RecetaPdfService {

    public byte[] generarPdf(RecetaOptica receta) {
        // Configuramos márgenes un poco más amplios para que se vea como un recetario real
        Document document = new Document(com.lowagie.text.PageSize.LETTER, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // ==========================================
            // CONFIGURACIÓN DE ESTILOS (Colores y Fuentes)
            // ==========================================
            Color colorFondoAzul = new Color(225, 238, 255); // Azul claro similar a la imagen
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font fontNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // ==========================================
            // 1. ENCABEZADO (FECHA Y TÍTULO CON EMPRESA)
            // ==========================================
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1f, 1.5f}); // Proporción de columnas

            // Celda Fecha
            String fecha = receta.getFechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            PdfPCell cellFecha = new PdfPCell(new Phrase("Fecha: " + fecha, fontNegrita));
            cellFecha.setBackgroundColor(colorFondoAzul);
            cellFecha.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cellFecha.setPadding(8f);
            headerTable.addCell(cellFecha);

            // Celda Título
            String nombreEmpresa = receta.getConsulta().getCita().getSucursal().getEmpresa().getRazonSocial();
            PdfPCell cellTitulo = new PdfPCell(new Phrase("RECETA ÓPTICA - " + nombreEmpresa.toUpperCase(), fontTitulo));
            cellTitulo.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellTitulo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellTitulo.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            headerTable.addCell(cellTitulo);

            document.add(headerTable);
            document.add(new Paragraph("\n"));

            // ==========================================
            // 2. DATOS DEL PACIENTE
            // ==========================================
            PdfPTable pacienteTable = new PdfPTable(2);
            pacienteTable.setWidthPercentage(100);
            pacienteTable.setWidths(new float[]{1f, 1f});

            // Nombre
            String nombrePaciente = receta.getConsulta().getCita().getPaciente().getNombre() + " " +
                    receta.getConsulta().getCita().getPaciente().getApellido();
            PdfPCell cellNombre = new PdfPCell(new Phrase("Paciente: " + nombrePaciente, fontNegrita));
            cellNombre.setColspan(2); // Ocupa todo el ancho
            cellNombre.setBackgroundColor(colorFondoAzul);
            cellNombre.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cellNombre.setPadding(8f);
            pacienteTable.addCell(cellNombre);

            // Fila de espacio en blanco para separar
            PdfPCell espacio = new PdfPCell(new Phrase(" "));
            espacio.setColspan(2);
            espacio.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            espacio.setFixedHeight(5f);
            pacienteTable.addCell(espacio);

            // Edad[cite: 2]
            String edadStr = "";
            LocalDate fn = receta.getConsulta().getCita().getPaciente().getFechaNacimiento();
            if (fn != null) {
                edadStr = String.valueOf(Period.between(fn, LocalDate.now()).getYears());
            }
            PdfPCell cellEdad = new PdfPCell(new Phrase("Edad: " + edadStr, fontNegrita));
            cellEdad.setBackgroundColor(colorFondoAzul);
            cellEdad.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cellEdad.setPadding(8f);
            pacienteTable.addCell(cellEdad);

            // Rut[cite: 2]
            String rut = receta.getConsulta().getCita().getPaciente().getNumeroDocumento();
            PdfPCell cellRut = new PdfPCell(new Phrase("Rut: " + (rut != null ? rut : ""), fontNegrita));
            cellRut.setBackgroundColor(colorFondoAzul);
            cellRut.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cellRut.setPadding(8f);
            pacienteTable.addCell(cellRut);

            document.add(pacienteTable);
            document.add(new Paragraph("\n"));

            // ==========================================
            // 3. DIAGNÓSTICO
            // ==========================================
            String diag = receta.getConsulta().getDiagnostico();
            document.add(new Paragraph("Diagnóstico: " + (diag != null ? diag : ""), fontNegrita));
            document.add(new Paragraph("\n"));

            // ==========================================
            // 4. TABLA: LENTES PARA LEJOS
            // ==========================================
            PdfPTable tableLejos = new PdfPTable(4);
            tableLejos.setWidthPercentage(100);
            tableLejos.setWidths(new float[]{1.5f, 1f, 1f, 1f});

            // Encabezados Lejos
            String[] headersLejos = {"LENTES PARA LEJOS", "ESFERA", "CILINDRO", "EJE"};
            for (String h : headersLejos) {
                PdfPCell c = new PdfPCell(new Phrase(h, fontNegrita));
                c.setBackgroundColor(colorFondoAzul);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setPadding(6f);
                tableLejos.addCell(c);
            }

            // Buscar OD y OI en los detalles[cite: 2]
            RecetaOpticaDetalle od = null;
            RecetaOpticaDetalle oi = null;
            for (RecetaOpticaDetalle det : receta.getDetalles()) {
                if ("OD".equals(det.getOjo().name())) od = det;
                if ("OI".equals(det.getOjo().name())) oi = det;
            }

            agregarFilaOjo(tableLejos, "OD", od, fontNormal);
            agregarFilaOjo(tableLejos, "OI", oi, fontNormal);

            document.add(tableLejos);
            document.add(new Paragraph("\n"));

            // ==========================================
            // 5. DISTANCIA PUPILAR (DP) - Superior
            // ==========================================
            PdfPTable tableDp = new PdfPTable(1);
            tableDp.setWidthPercentage(100);
            String dpVal = receta.getDistanciaPupilar() != null ? receta.getDistanciaPupilar().toString() : "      ";
            PdfPCell cDp = new PdfPCell(new Phrase("DP:   " + dpVal + "   MM", fontNegrita));
            cDp.setPadding(6f);
            tableDp.addCell(cDp);

            document.add(tableDp);
            document.add(new Paragraph("\n"));

            // ==========================================
            // 6. TABLA: LENTES PARA CERCA (Con Adición)
            // ==========================================
            PdfPTable tableCerca = new PdfPTable(5);
            tableCerca.setWidthPercentage(100);
            tableCerca.setWidths(new float[]{0.6f, 1.2f, 1f, 1f, 1f});

            // Fila 1: Encabezados Cerca
            PdfPCell headerCerca = new PdfPCell(new Phrase("LENTES PARA CERCA", fontNegrita));
            headerCerca.setColspan(2);
            headerCerca.setBackgroundColor(colorFondoAzul);
            headerCerca.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCerca.setPadding(6f);
            tableCerca.addCell(headerCerca);

            String[] headersCercaDer = {"ESFERA", "CILINDRO", "EJE"};
            for (String h : headersCercaDer) {
                PdfPCell c = new PdfPCell(new Phrase(h, fontNegrita));
                c.setBackgroundColor(colorFondoAzul);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setPadding(6f);
                tableCerca.addCell(c);
            }

            // Fila 2: OD + Celda Combinada de ADD + Valores Vacíos
            PdfPCell cellOD = crearCeldaCentro("OD", fontNegrita);
            cellOD.setBackgroundColor(colorFondoAzul);
            tableCerca.addCell(cellOD);

            String addVal = receta.getAdicion() != null ? receta.getAdicion().toString() : "      ";
            PdfPCell cellAdd = new PdfPCell(new Phrase("ADD:\n\n" + addVal + " ESF", fontNegrita));
            cellAdd.setRowspan(2); // ¡Aquí ocurre la magia! Esta celda abarca 2 filas hacia abajo
            cellAdd.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellAdd.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tableCerca.addCell(cellAdd);

            tableCerca.addCell(crearCeldaCentro("", fontNormal));
            tableCerca.addCell(crearCeldaCentro("", fontNormal));
            tableCerca.addCell(crearCeldaCentro("", fontNormal));

            // Fila 3: OI + Valores Vacíos (La celda ADD ya ocupa su espacio)
            PdfPCell cellOI = crearCeldaCentro("OI", fontNegrita);
            cellOI.setBackgroundColor(colorFondoAzul);
            tableCerca.addCell(cellOI);

            tableCerca.addCell(crearCeldaCentro("", fontNormal));
            tableCerca.addCell(crearCeldaCentro("", fontNormal));
            tableCerca.addCell(crearCeldaCentro("", fontNormal));

            document.add(tableCerca);
            document.add(new Paragraph("\n"));

            // Volvemos a colocar la fila de DP para abajo (como en la imagen)
            document.add(tableDp);
            document.add(new Paragraph("\n"));

            // ==========================================
            // 7. OBSERVACIONES Y FIRMA
            // ==========================================
            document.add(new Paragraph("Observaciones: " + (receta.getObservaciones() != null ? receta.getObservaciones() : ""), fontNormal));

            // Espacio amplio para la firma
            document.add(new Paragraph("\n\n\n\n\n"));

            Paragraph firma = new Paragraph("___________________________________\nFIRMA", fontNegrita);
            firma.setAlignment(Element.ALIGN_RIGHT);
            firma.setIndentationRight(30f); // Separamos un poco del borde derecho
            document.add(firma);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de la receta: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    // ==========================================
    // MÉTODOS AUXILIARES PARA LIMPIAR EL CÓDIGO
    // ==========================================
    private void agregarFilaOjo(PdfPTable table, String ojo, RecetaOpticaDetalle det, Font font) {
        PdfPCell cOjo = new PdfPCell(new Phrase(ojo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        cOjo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cOjo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cOjo.setBackgroundColor(new Color(225, 238, 255)); // Azul claro
        cOjo.setPadding(6f);
        table.addCell(cOjo);

        if (det != null) {
            table.addCell(crearCeldaCentro(det.getEsfera() != null ? det.getEsfera().toString() : "", font));
            table.addCell(crearCeldaCentro(det.getCilindro() != null ? det.getCilindro().toString() : "", font));
            table.addCell(crearCeldaCentro(det.getEje() != null ? det.getEje().toString() : "", font));
        } else {
            table.addCell(crearCeldaCentro("", font));
            table.addCell(crearCeldaCentro("", font));
            table.addCell(crearCeldaCentro("", font));
        }
    }

    private PdfPCell crearCeldaCentro(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        return cell;
    }
}