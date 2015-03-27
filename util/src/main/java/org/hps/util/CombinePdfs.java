package org.hps.util;

import java.io.File;
import org.apache.pdfbox.util.PDFMergerUtility;

/**
 * This will merge all the pdf files in the folder pdf into one file called
 * MergedFiles.pdf
 *
 * @author Norman A Graf
 *
 */
public class CombinePdfs
{

    public static void main(String[] args) throws Exception
    {
        PDFMergerUtility pdfMerger = new PDFMergerUtility();
        File[] filesInFolder = new File("pdf").listFiles();
        for (File file : filesInFolder) {
            pdfMerger.addSource(file);
        }
        pdfMerger.setDestinationFileName("MergedFiles.pdf");
        pdfMerger.mergeDocuments();
    }
}
