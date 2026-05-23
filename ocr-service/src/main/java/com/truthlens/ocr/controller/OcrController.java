package com.truthlens.ocr.controller;

import com.truthlens.ocr.model.OcrExtractionResponse;
import com.truthlens.ocr.service.VisionExtractionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class OcrController {

    private final VisionExtractionService visionExtractionService;

    public OcrController(VisionExtractionService visionExtractionService) {
        this.visionExtractionService = visionExtractionService;
    }

    @PostMapping("/api/internal/v1/ocr:extract")
    public OcrExtractionResponse extract(@RequestParam("image") MultipartFile image) {
        return visionExtractionService.extractText(image);
    }
}