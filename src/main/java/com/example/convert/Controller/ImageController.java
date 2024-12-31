package com.example.convert.Controller;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Value("${tesseract.datapath}")
    private String tessdataPath;
    @Value("${tesseract.lir}")
    private String lirPath;
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                return ResponseEntity.badRequest().body("File name is invalid");
            }

            File tempFile = File.createTempFile("uploaded", originalFilename);
            tempFile.deleteOnExit();
            file.transferTo(tempFile);

            String extractedText = extractTextFromImage(tempFile);
            File wordFile = createWordFile(extractedText);

            InputStreamResource resource = new InputStreamResource(new FileInputStream(wordFile));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=result.docx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error processing file", e);
            return ResponseEntity.status(500).body("Error processing file");
        }
    }

    private String extractTextFromImage(File imageFile) throws TesseractException {
        System.setProperty("jna.library.path",lirPath);
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("vie");
        return tesseract.doOCR(imageFile);
    }

    private File createWordFile(String content) throws IOException {
        File outputFile = File.createTempFile("result", ".docx");
        outputFile.deleteOnExit();
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph paragraph = doc.createParagraph();
            paragraph.createRun().setText(content);
            try (OutputStream out = new FileOutputStream(outputFile)) {
                doc.write(out);
            }
        }
        return outputFile;
    }
}
