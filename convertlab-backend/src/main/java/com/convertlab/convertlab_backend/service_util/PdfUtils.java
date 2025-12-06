package com.convertlab.convertlab_backend.service_util;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.convertlab.convertlab_backend.exception.InvalidPageInputException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;

public class PdfUtils {

    final static String FILE_NAME_SEPARATOR = "_";

    public static int getPageCount(File pdfFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            return doc.getNumberOfPages();
        }
    }

    public static byte[] extractPages(File pdfFile, List<Integer> pagesToKeep) throws IOException {
        try (PDDocument original = Loader.loadPDF(pdfFile);
             PDDocument output = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPageTree allPages = original.getDocumentCatalog().getPages();

            for (Integer pageNum : pagesToKeep) {
                output.addPage(allPages.get(pageNum - 1)); // 0-based index
            }

            output.save(baos);
            return baos.toByteArray();
        }
    }

    public static String getOriginalUserFileName(File pdfFile) {
        if (null == pdfFile) {
            return null;
        }
        String[] fileNameParts = pdfFile.getName().split(FILE_NAME_SEPARATOR);
        if (fileNameParts.length == 1) {
            return fileNameParts[0];
        }
        if (fileNameParts.length > 1) {
            return fileNameParts[1];
        }
        return pdfFile.getName();
    }

    public static void validateInputRangePattern(String input) {
        if (input == null || input.isBlank()) {
            throw new InvalidPageInputException("Page selection cannot be empty.");
        }

        // Allowed chars: digits, comma, dash
        if (!input.matches("[0-9,\\- ]+")) {
            throw new InvalidPageInputException("Page pattern contains invalid characters.");
        }

        String[] parts = input.split(",");

        for (String part : parts) {
            part = part.trim();

            if (part.isEmpty()) {
                throw new InvalidPageInputException("Invalid page format: empty section between commas.");
            }

            if (part.contains("-")) {
                // Validate correct range format: X-Y
                if (!part.matches("\\d+\\s*-\\s*\\d+")) {
                    throw new InvalidPageInputException("Invalid range format: '" + part + "'. Expected format: 4-7");
                }
            } else {
                // Must be a number
                if (!part.matches("\\d+")) {
                    throw new InvalidPageInputException("Invalid page value: '" + part + "'");
                }
            }
        }
    }


    /**
     * Parse a string containing page ranges and single page numbers into a list of valid page numbers.
     * The input string is expected to be a comma-separated list of page ranges (e.g. "1-2, 4-5")
     * or single page numbers (e.g. "3, 6"). The page ranges can be in any order, and the numbers
     * do not need to be in increasing order.
     * The function will return all page numbers from 1 to totalPages (inclusive) that are present in the input string.
     * If the input string contains invalid page numbers or invalid range formats, an InvalidPageInputException
     * will be thrown.
     * @param input The input string that contains the page ranges or single page numbers to be parsed.
     * @param totalPages The total number of pages in the PDF.
     * @return A list of valid page numbers that are present in the input string.
     * @throws InvalidPageInputException If the input string contains invalid page numbers or invalid range formats.
     */
    public static List<Integer> parsePageRanges(String input, int totalPages) {
        if (totalPages <= 0) {
            throw new InvalidPageInputException("PDF has zero pages or invalid page count.");
        }
        if (input == null || input.isBlank()) {
            throw new InvalidPageInputException("Page selection cannot be empty.");
        }
        Set<Integer> pages = new TreeSet<>();
        String[] parts = input.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length != 2) {
                    throw new InvalidPageInputException("Invalid range format: '" + part + "'. Expected format: 4-7");
                }
                int start, end;
                try {
                    start = Integer.parseInt(range[0].trim());
                    end = Integer.parseInt(range[1].trim());
                } catch (NumberFormatException e) {
                    throw new InvalidPageInputException("Range contains non-numeric values: '" + part + "'");
                }
                // Normalizing reversed input (7-4)
                if (start > end) {
                    int temp = start;
                    start = end;
                    end = temp;
                }
                if (start < 1 || end > totalPages) {
                    throw new InvalidPageInputException(
                            "Range '" + part + "' is outside valid page numbers (1 to " + totalPages + ").");
                }
                for (int i = start; i <= end; i++) {
                    pages.add(i);
                }

            } else {
                // Handle single numbers
                try {
                    int page = Integer.parseInt(part);

                    if (page < 1 || page > totalPages) {
                        throw new InvalidPageInputException(
                                "Page '" + page + "' is outside valid range 1 to " + totalPages + ".");
                    }
                    pages.add(page);
                } catch (NumberFormatException e) {
                    throw new InvalidPageInputException("Invalid page value: '" + part + "'. Only numbers allowed.");
                }
            }
        }
        return new ArrayList<>(pages);
    }

    /**
     * Returns a list of page numbers that are not present in the given remove input.
     * The remove input is expected to be a string of comma-separated page ranges (e.g. "1-2, 4-5").
     * The page ranges can be in any order, and the numbers do not need to be in increasing order.
     * The function will return all page numbers from 1 to totalPages (inclusive) that are not present in the remove input.
     * @param removeInput The input string that contains the page ranges to be removed.
     * @param totalPages The total number of pages.
     * @return A list of page numbers that are not present in the given remove input.
     */
    public static List<Integer> removePages(String removeInput, int totalPages) {
        List<Integer> removeList = parsePageRanges(removeInput, totalPages);

        Set<Integer> remaining = new TreeSet<>();
        for (int i = 1; i <= totalPages; i++) {
            if (!removeList.contains(i)) {
                remaining.add(i);
            }
        }
        return new ArrayList<>(remaining);
    }

    public static List<Integer> getPageRanges(String input, int totalPages, boolean keep) {
        if (keep) {
            return parsePageRanges(input, totalPages);
        } else {
            return removePages(input, totalPages);
        }
    }
}

