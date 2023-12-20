package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/pdf")
public class PdfEndpoint {

    private static String FILE_NAME;
    private static byte[] FILE_CONTENT;

    @GetMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> download(){
        log.info("Started downloading file: " + FILE_NAME + " | " + "size: " + FILE_CONTENT.length + " bytes");
        var byteArrayResource = new ByteArrayResource(FILE_CONTENT);
        return ResponseEntity.ok()
                .contentLength(byteArrayResource.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(byteArrayResource);
    }

    @GetMapping(value = "/prefilled", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadPrefilled() throws IOException {
        log.info("Started downloading file prefilled pdf file");
        var initialPdfFileContent = IOUtils.resourceToByteArray("/static/fillable_PDF_form_example.pdf");
        var prefilledPdfFileContent = prefillPdfFile(initialPdfFileContent);

        var byteArrayResource = new ByteArrayResource(prefilledPdfFileContent);
        return ResponseEntity.ok()
                .contentLength(byteArrayResource.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(byteArrayResource);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public void upload(@RequestParam("file") MultipartFile file) throws IOException {
        FILE_NAME = file.getOriginalFilename();
        FILE_CONTENT = file.getBytes();
        logPdfFormInfo(file.getBytes());
        log.info("Successfully uploaded file: " + file.getOriginalFilename() + " | " + "size: " + file.getBytes().length + " bytes");
    }

    private byte[] prefillPdfFile(byte[] pdfFileContent) throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfFileContent))) {
            var acroForm = document.getDocumentCatalog().getAcroForm();
            acroForm.getField("FirstName").setValue("Prefill");
            acroForm.getField("LastName").setValue("Prefillson");
            acroForm.getField("Email").setValue("prefill@example.com");
            ((PDRadioButton) acroForm.getField("Color")).setValue("Blue");
            ((PDCheckBox) acroForm.getField("Land")).check();
            ((PDCheckBox) acroForm.getField("Water")).check();
            ((PDComboBox) acroForm.getField("Options")).setValue("Large");
            acroForm.getField("BigTextField1").setValue("Just some random text");
            acroForm.getField("BigTextField2").setValue("More gibberish text");
            acroForm.getField("FinalField").setValue("The end in near");
            acroForm.getField("Date").setValue("2023-12-31");

            document.save(byteArrayOutputStream, CompressParameters.DEFAULT_COMPRESSION);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private void logPdfFormInfo(byte[] pdfFileContent) throws IOException {
        log.info("------------ PDF form info START ----------------");
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfFileContent))) {
            var acroForm = document.getDocumentCatalog().getAcroForm();
            for (var pdField : acroForm.getFields()) {
                log.info(pdField.toString());
            }
        }
        log.info("------------- PDF form info END -----------------");
    }
}
