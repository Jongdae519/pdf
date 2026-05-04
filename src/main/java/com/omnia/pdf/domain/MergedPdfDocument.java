package com.omnia.pdf.domain;

public record MergedPdfDocument(
        String fileName,
        byte[] content
) {
    public MergedPdfDocument {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("PDF content cannot be empty");
        }
    }
}