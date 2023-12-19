package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + FILE_NAME);

        var byteArrayResource = new ByteArrayResource(FILE_CONTENT);
        byteArrayResource.contentLength();

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(byteArrayResource.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(byteArrayResource);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String upload(@RequestParam("file") MultipartFile file) throws IOException {
        FILE_NAME = file.getOriginalFilename();
        FILE_CONTENT = file.getBytes();
        var infoString = "Successfully uploaded file: " + file.getOriginalFilename() + " | " + "size: " + file.getBytes().length + " bytes";
        log.info(infoString);
        return infoString;
    }

    @PostMapping("/reset")
    public void resetSavedFile(@RequestParam("file") MultipartFile file) throws IOException {
        FILE_NAME = file.getOriginalFilename();
        FILE_CONTENT = file.getBytes();
        log.info("Successfully rest saved file to default pdf");
    }
}
