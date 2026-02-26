package com.convertlab.convertlab_backend.security_util;

import com.convertlab.convertlab_backend.exception.PdfPasswordException;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Log4j2
public class PdfPasswordUtils {

    public static byte[] addPassword(File pdfFile, String password) throws IOException {
        log.info("Adding password protection to file: {}", pdfFile.getName());

        // Check if already encrypted before attempting to load
        try (PDDocument check = Loader.loadPDF(pdfFile)) {
            if (check.isEncrypted()) {
                throw new PdfPasswordException(
                        "This PDF is already password protected. Please remove the existing password first.",
                        "PDF_ALREADY_PROTECTED"
                );
            }
        } catch (IOException e) {
            // If we can't open without a password, it's encrypted
            throw new PdfPasswordException(
                    "This PDF is already password protected. Please remove the existing password first.",
                    "PDF_ALREADY_PROTECTED"
            );
        }

        try (PDDocument document = Loader.loadPDF(pdfFile);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            AccessPermission permissions = new AccessPermission();

            StandardProtectionPolicy policy =
                    new StandardProtectionPolicy(password, password, permissions);
            policy.setEncryptionKeyLength(256);

            document.protect(policy);
            document.save(baos);

            log.info("Password added successfully to: {}", pdfFile.getName());
            return baos.toByteArray();
        }
    }

    public static byte[] removePassword(File pdfFile, String password) throws IOException {
        log.info("Removing password protection from file: {}", pdfFile.getName());

        try (PDDocument document = Loader.loadPDF(pdfFile, password);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            if (!document.isEncrypted()) {
                throw new PdfPasswordException(
                        "This PDF is not password protected.",
                        "PDF_NOT_PROTECTED"
                );
            }

            document.setAllSecurityToBeRemoved(true);
            document.save(baos);

            log.info("Password removed successfully from: {}", pdfFile.getName());
            return baos.toByteArray();
        } catch (IOException e) {
            // PDFBox throws IOException with "Cannot decrypt PDF, the password is incorrect"
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("password")) {
                throw new IllegalArgumentException("Incorrect password provided for the PDF", e);
            }
            throw e;
        }
    }
}