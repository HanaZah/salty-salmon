package com.finadvise.crm.documents;

public enum DocumentFormat {
    PDF,
    JPG,
    PNG,
    WORD,
    EXCEL,
    UNKNOWN;

    public static DocumentFormat fromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return UNKNOWN;
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            case "pdf" -> PDF;
            case "jpg", "jpeg" -> JPG;
            case "png" -> PNG;
            case "doc", "docx" -> WORD;
            case "xls", "xlsx", "csv" -> EXCEL;
            default -> UNKNOWN;
        };
    }
}


