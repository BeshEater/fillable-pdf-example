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
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.flavours.PDFAFlavour;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/pdf")
public class PdfEndpoint {

    private static String FILE_NAME;
    private static byte[] FILE_CONTENT;

    static {
        // Initialize veraPDF validator
        VeraGreenfieldFoundryProvider.initialise();
    }

    @GetMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> download(){
        log.info("Started downloading file: {} | size: {} bytes", FILE_NAME, FILE_CONTENT.length);
        validatePdf(FILE_CONTENT);
        var byteArrayResource = new ByteArrayResource(FILE_CONTENT);
        return ResponseEntity.ok()
                .contentLength(byteArrayResource.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(byteArrayResource);
    }

    @GetMapping(value = "/flattened", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadFlattened() {
        log.info("Started downloading flattened pdf file");
        var flattenedPdfFile = flattenPdfFile(FILE_CONTENT);

        var byteArrayResource = new ByteArrayResource(flattenedPdfFile);
        return ResponseEntity.ok()
                .contentLength(byteArrayResource.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(byteArrayResource);
    }

    @GetMapping(value = "/default-prefilled", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadDefaultPrefilled() throws IOException {
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
        log.info("Successfully uploaded file: {} | size: {} bytes", file.getOriginalFilename(), file.getBytes().length);
    }

    private byte[] flattenPdfFile(byte[] pdfFileContent) {
        var byteArrayOutputStream = new ByteArrayOutputStream();

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfFileContent))) {
            var acroForm = document.getDocumentCatalog().getAcroForm();
            acroForm.flatten();

            document.save(byteArrayOutputStream, CompressParameters.DEFAULT_COMPRESSION);
        } catch (IOException ex) {
            log.error("Error while flattening PDF file", ex);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private byte[] prefillPdfFile(byte[] pdfFileContent) {
        var byteArrayOutputStream = new ByteArrayOutputStream();

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfFileContent))) {
            var acroForm = document.getDocumentCatalog().getAcroForm();
            acroForm.getField("FirstName").setValue("Prefill");
            acroForm.getField("LastName").setValue("Prefillson");
            acroForm.getField("Email").setValue("prefill@example.com");
            ((PDRadioButton) acroForm.getField("Color")).setValue("Blue");
            ((PDCheckBox) acroForm.getField("Land")).check();
            ((PDCheckBox) acroForm.getField("Water")).check();
            ((PDComboBox) acroForm.getField("Options")).setValue("Crisp");
            acroForm.getField("BigTextField1").setValue("Just some random text");
            acroForm.getField("BigTextField2").setValue("More gibberish text");
            acroForm.getField("FinalField").setValue("The end");
            acroForm.getField("Date").setValue("2023-12-31");

            document.save(byteArrayOutputStream, CompressParameters.DEFAULT_COMPRESSION);
        } catch (IOException ex) {
            log.error("Error while prefilling PDF file", ex);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private void logPdfFormInfo(byte[] pdfFileContent) {
        log.info("------------ PDF form info START ----------------");
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfFileContent))) {
            var acroForm = document.getDocumentCatalog().getAcroForm();
            for (var pdField : acroForm.getFields()) {
                log.info(pdField.toString());
            }
        } catch (IOException ex) {
            log.error("Error while reading PDF form info", ex);
        }
        log.info("------------- PDF form info END -----------------");
    }

    private void validatePdf(byte[] pdfFileContent) {
        var flavour = PDFAFlavour.PDFA_2_B;

        try (var pdfFoundry = Foundries.defaultInstance();
             var parser = pdfFoundry.createParser(new ByteArrayInputStream(pdfFileContent));
             var validator = pdfFoundry.createValidator(flavour, false)
        ) {
            var result = validator.validate(parser);
            log.info("Pdf validation results: {}", result);
        } catch (Exception ex) {
            log.error("Cannot validate pdf", ex);
        }
    }
}