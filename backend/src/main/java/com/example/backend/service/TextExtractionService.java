package com.example.backend.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class TextExtractionService {

    private final Tika tika = new Tika();

    @Value("${app.resume.max-chars:8000}")
    private int maxChars;

    public String extract(MultipartFile file) throws IOException {
        try {
            String text = tika.parseToString(file.getInputStream());
            // Truncate to avoid excessive token usage
            return text.length() > maxChars ? text.substring(0, maxChars) : text;
        } catch (TikaException e) {
            throw new IOException("Could not parse file: " + file.getOriginalFilename(), e);
        }
    }
}
