package com.convertlab.convertlab_backend.service_resume;

public interface HtmlToPdfConverter {
    byte[] convert(String html, HtmlToPdfOptions options);
}
