package com.convertlab.convertlab_backend.service_ai;

public interface DocumentTextProcessor {

    /**
     * Processes raw extracted document text
     * and returns normalized text ready for indexing.
     */
    String process(String rawText);
}
