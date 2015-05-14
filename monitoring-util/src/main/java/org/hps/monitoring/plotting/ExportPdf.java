package org.hps.monitoring.plotting;

import hep.aida.IPlotter;
import hep.aida.jfree.plotter.Plotter;
import hep.aida.jfree.plotter.PlotterRegion;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

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
     * Do not allow class instantiation.
     */
    private ExportPdf() {
    }

    /**
     * Save a set of tabs containing plots to a file.
     * 
     * @param plotTabs the top level tab component (plots are actually in a set of tabs without these tabs)
     * @param fileName the file name
     * @param runData the list of run data to save on the cover page
     * @throws IOException if there is a problem with the IO (e.g. writing to PDF file)
     */
    public static void write(JTabbedPane plotTabs, String fileName, List<String> runData) throws IOException {

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

        PlotterRegistry plotterRegistry = MonitoringPlotFactory.getPlotterRegistry();

        int savedTabIndex = plotTabs.getSelectedIndex();
        int savedSubTabIndex = ((JTabbedPane) plotTabs.getComponentAt(savedTabIndex)).getSelectedIndex();

        for (int tabIndex = 0; tabIndex < plotTabs.getTabCount(); tabIndex++) {
            plotTabs.setSelectedIndex(tabIndex);
            JTabbedPane subPane = (JTabbedPane) plotTabs.getSelectedComponent();
            for (int subTabIndex = 0; subTabIndex < subPane.getTabCount(); subTabIndex++) {
                subPane.setSelectedIndex(subTabIndex);
                String title = null;
                IPlotter plotter = plotterRegistry.find(tabIndex, subTabIndex);
                if (plotter != null) {
                    title = plotter.title();
                } else {
                    title = ((JLabel) subPane.getTabComponentAt(subTabIndex)).getText();
                }
                if (plotter != null) {
                    plotter.refresh();
                }
                Component component = subPane.getSelectedComponent();
                if (component != null) {
                    document.newPage();
                    Image image = getImage(component);
                    writePage(document, writer, title, image);
                }
            }
        }

        document.close();

        plotTabs.setSelectedIndex(savedTabIndex);
        ((JTabbedPane) plotTabs.getComponentAt(savedTabIndex)).setSelectedIndex(savedSubTabIndex);
    }

    /**
     * Write plotter graphics into a single PDF page.
     * 
     * @param document the output PDF document
     * @param writer the PDF writer
     * @param plotter the plotter with the graphics
     * @throws IOException if there is a problem writing to the PDF document
     */
    static void writePage(Document document, PdfWriter writer, String title, JPanel component) throws IOException {

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
            iTextImage = com.itextpdf.text.Image.getInstance(writer, getImage(component), 1f);
        } catch (BadElementException e) {
            throw new IOException(e);
        }
        iTextImage.scaleToFit(document.getPageSize());
        iTextImage.setAlignment(Element.ALIGN_CENTER);
        try {
            document.add(iTextImage);
        } catch (DocumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Write a buffered image into a single PDF page.
     * 
     * @param document the output PDF document
     * @param writer the PDF writer
     * @param image the buffered bitmap image
     * @throws IOException if there is a problem writing to the PDF document
     */
    static void writePage(Document document, PdfWriter writer, String title, Image image) throws IOException {

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
        iTextImage.scaleToFit(document.getPageSize());
        iTextImage.setAlignment(Element.ALIGN_CENTER);
        try {
            document.add(iTextImage);
        } catch (DocumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Write the graphics from a list of plotters to a PDF, with one plotter per page, including a summary page with the
     * run data.
     * 
     * @param plotters the list of plotters
     * @param fileName the output file name
     * @param runData a list of run data to include on the first page
     * @throws IOException if there is a problem opening or writing to the PDF document
     */
    public static void write(List<IPlotter> plotters, String fileName, List<String> runData) throws IOException {

        // Open the document and the writer.
        Document document = new Document(PageSize.LETTER.rotate(), 50, 50, 50, 50);
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
     * Write plotter graphics into a single PDF page.
     * 
     * @param document the output PDF document
     * @param writer the PDF writer
     * @param plotter the plotter with the graphics
     * @throws IOException if there is a problem writing to the PDF document
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

        // Create image from the panel.
        Image awtImage = ((Plotter) plotter).getImage();

        // Write image into the document.
        com.itextpdf.text.Image iTextImage = null;
        try {
            iTextImage = com.itextpdf.text.Image.getInstance(writer, awtImage, 1f);
        } catch (BadElementException e) {
            throw new IOException(e);
        }
        iTextImage.scaleToFit(document.getPageSize());
        iTextImage.setAlignment(Element.ALIGN_CENTER);
        try {
            document.add(iTextImage);
        } catch (DocumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Add a page with the run summary data.
     * 
     * @param runData The list of run summary information.
     */
    static void writeRunData(Document document, List<String> runData) throws DocumentException {
        for (String line : runData) {
            Paragraph p = new Paragraph(line, new Font(FontFamily.HELVETICA, 20));
            p.setAlignment(Element.ALIGN_LEFT);
            document.add(p);
        }
    }

    /**
     * This is a hack to try and get a plotter's chart components their scaling by poking them each with a mouse event.
     * 
     * @param plotter the AIDA plotter
     */
    static void poke(IPlotter plotter) {
        int nregions = plotter.numberOfRegions();
        for (int regionIndex = 0; regionIndex < nregions; regionIndex++) {
            JPanel panel = ((PlotterRegion) plotter.region(regionIndex)).getPanel();
            if (panel != null) {
                int b = InputEvent.getMaskForButton(MouseEvent.BUTTON1);
                Robot r = null;
                try {
                    r = new Robot();
                    Point location = panel.getLocationOnScreen();
                    r.mouseMove(location.x, location.y);
                    r.mousePress(b);
                    r.mouseRelease(b);
                } catch (AWTException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
