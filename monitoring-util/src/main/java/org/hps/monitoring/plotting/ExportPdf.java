package org.hps.monitoring.plotting;

import hep.aida.IPlotter;
import hep.aida.jfree.plotter.Plotter;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

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
 */
public final class ExportPdf {

    /**
     * Setup logging.
     */
    private static Logger LOGGER = Logger.getLogger(ExportPdf.class.getPackage().getName());

    /**
     * Do not allow class instantiation.
     */
    private ExportPdf() {
    }

    /**
     * Save a set of tabs containing plots to a file.
     *
     * @param plotTabs the top level tab component (plots are actually in a set
     * of tabs without these tabs)
     * @param fileName the file name
     * @param runData the list of run data to save on the cover page
     * @throws IOException if there is a problem with the IO (e.g. writing to
     * PDF file)
     */
    public static void write(List<IPlotter> plotters, String fileName, List<String> runData)
            throws IOException {

        LOGGER.info("writing plots to " + fileName + " ...");

        // Open the document and the writer.
        Document document = new Document(PageSize.LETTER.rotate(), 50, 50, 50, 50);
        PdfWriter writer = null;
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
        
        ArrayList<IPlotter> sortedPlotters = new ArrayList<IPlotter>(plotters);
        Collections.sort(sortedPlotters, new Comparator<Object>() {
           public int compare(Object object1, Object object2) {
               return ((IPlotter)object1).title().compareTo(((IPlotter)object2).title());
           }            
        });

        // Write the graphics from each plotter on a new page.
        for (IPlotter plotter : sortedPlotters) {
            plotter.refresh();
            document.newPage();
            writePage(document, writer, plotter);
        }

        document.close();

        LOGGER.info("done writing plots to " + fileName);
    }

    /**
     * Write a plotter's graphics into a single PDF page.
     *
     * @param document the output PDF document
     * @param writer the PDF writer
     * @param image the buffered bitmap image
     * @throws IOException if there is a problem writing to the PDF document
     */
    static void writePage(Document document, PdfWriter writer, IPlotter plotter) throws IOException {

        Image image = ((Plotter) plotter).getImage();
        String title = plotter.title();

        // Add header label.
        Paragraph p = new Paragraph(title, new Font(FontFamily.HELVETICA, 24));
        p.setAlignment(Element.ALIGN_CENTER);
        try {
            document.add(p);
        } catch (DocumentException e) {
            throw new IOException(e);
        }

        // Write image into the document.
        com.itextpdf.text.Image iTextImage = null;
        try {
            iTextImage = com.itextpdf.text.Image.getInstance(writer, image, 1f);
        } catch (BadElementException e) {
            throw new IOException(e);
        }
        iTextImage.scaleAbsolute(document.getPageSize().getWidth(), (float) 0.75 * document.getPageSize().getHeight());
        iTextImage.setAlignment(Element.ALIGN_CENTER);
        try {
            document.add(iTextImage);
        } catch (DocumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Get a buffered image from a Swing component.
     *
     * @param component the Swing component
     * @return the image from painting the component onto a buffered image
     */
    public static BufferedImage getImage(Component component) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        component.paint(image.getGraphics());
        return image;
    }

    /**
     * Add a page with the run summary data.
     *
     * @param runData the list of run summary information
     */
    static void writeRunData(Document document, List<String> runData) throws DocumentException {
        for (String line : runData) {
            Paragraph p = new Paragraph(line, new Font(FontFamily.HELVETICA, 20));
            p.setAlignment(Element.ALIGN_LEFT);
            document.add(p);
        }
    }
}
