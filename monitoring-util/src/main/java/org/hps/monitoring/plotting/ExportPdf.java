package org.hps.monitoring.plotting;

import hep.aida.IPlotter;
import hep.aida.jfree.plotter.Plotter;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.util.PDFMergerUtility;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
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
     * Export a JPanel to a single PDF.
     * @param panel The JPanel.
     * @param name The output file name.
     * @throws IOException If there is a problem opening the PDF document.
     */
    static void write(JPanel panel, String name) throws IOException {
        
        Document document = new Document(PageSize.A4.rotate(), 50, 50, 50, 50);        
        PdfWriter writer;
        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(name));
        } catch (DocumentException e) {
            throw new IOException(e);
        }
        document.open();        
        
        Image awtImage = getImageFromPanel(panel);
        
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
        
        document.close();
    }
    
    /**
     * Get an image from a Swing component.
     * @param component The Swing component.
     * @return The image from the component.
     */
    static java.awt.Image getImageFromPanel(Component component) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        component.paint(image.getGraphics());
        return image;
    }
        
    /**
     * Write a single PDF containing the graphics from the collection of plotters,
     * with one plotter per page.
     * 
     * @param plotters The collection of plotters.
     * @param fileName The name of the output file.
     * @throws IOException If there is a problem merging all the plotter PDFs together.
     */
    public static void write(Collection<IPlotter> plotters, String fileName) throws IOException {                
        
        PDFMergerUtility merge = new PDFMergerUtility();
        merge.setDestinationFileName(fileName);
        
        List<File> tempFiles = new ArrayList<File>();
        for (IPlotter plotter : plotters) {
            File tempFile = File.createTempFile("plot", ".pdf");
            tempFiles.add(tempFile);
            write(((Plotter)plotter).panel(), tempFile.getPath());
            merge.addSource(tempFile);
        }
        
        try {
            merge.mergeDocuments();
        } catch (COSVisitorException e) {
            throw new IOException("Error merging documents", e);
        }
        
        for (File tempFile : tempFiles) {
            tempFile.delete();
        }
    }
}
