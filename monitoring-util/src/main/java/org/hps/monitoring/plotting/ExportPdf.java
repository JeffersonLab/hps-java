package org.hps.monitoring.plotting;

import hep.aida.IPlotter;
import hep.aida.jfree.plotter.Plotter;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * This is a class for exporting plot graphics to PDF.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class ExportPdf {

    private ExportPdf() {        
    }
    
    /**
     * Write the graphics from a list of plotters to a PDF, with one plotter per page,
     * including a summary page with the run data.     
     * @param plotters The list of plotters.
     * @param fileName The output file name.
     * @param runData A list of run data to include on the first page.
     * @throws IOException If there is a problem opening or writing to the PDF document.
     */
    public static void write(List<IPlotter> plotters, String fileName, List<String> runData) throws IOException {
        
        // Open the document and the writer.
        Document document = new Document(PageSize.A4.rotate(), 50, 50, 50, 50);
        PdfWriter writer;
        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
        } catch (DocumentException e) {
            throw new IOException(e);
        }
        document.open();
        
        // Create 1st page with run summary data.
        try {
            writeRunData(document, runData);
        } catch (DocumentException e) {
            throw new IOException(e);
        }

        // Write out the plots to the PDF, one page per plotter.
        for (int i = 0; i < plotters.size(); i++) {            
            document.newPage();            
            IPlotter plotter = plotters.get(i);            
            writePage(document, writer, plotter);
        }
        
        document.close();
    }
    
    /**
     * Get an image from a Swing component.
     * @param component The Swing component.
     * @return The image from the component.
     */
    public static BufferedImage getImage(Component component) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        component.paint(image.getGraphics());
        return image;
    }
                   
    /**
     * Write plotter graphics into a single PDF page.
     * @param document The output PDF document.
     * @param writer The PDF writer.
     * @param plotter The plotter with the graphics.
     * @throws IOException If there is a problem writing to the PDF document.
     */
    static void writePage(Document document, PdfWriter writer, IPlotter plotter) throws IOException {
                
        // Add header label.
        Paragraph p = new Paragraph(plotter.title(), new Font(FontFamily.HELVETICA, 24));
        p.setAlignment(Element.ALIGN_CENTER);
        try {
            document.add(p);
        } catch (DocumentException e) {
            throw new IOException(e);
        }
        
        // Create image from panel.
        Image awtImage = getImage(((Plotter)plotter).panel());
        
        // Write image into the document.
        com.itextpdf.text.Image iTextImage = null;
        try {
            iTextImage = com.itextpdf.text.Image.getInstance(writer, awtImage, 1f);
        } catch (BadElementException e) {
            throw new IOException(e);
        }                 
        iTextImage.setAbsolutePosition(50, 50);
        iTextImage.scalePercent(60);
        try {
            document.add(iTextImage);
        } catch (DocumentException e) {
            throw new IOException(e);
        }
    }    
    
    /**
     * Add a page with the run summary data.
     * @param runData The list of run summary information.
     */
    static void writeRunData(Document document, List<String> runData) throws DocumentException {
        for (String line : runData) {
            Paragraph p = new Paragraph(line, new Font(FontFamily.HELVETICA, 20));
            p.setAlignment(Element.ALIGN_LEFT);
            document.add(p);
        }
    }
}
