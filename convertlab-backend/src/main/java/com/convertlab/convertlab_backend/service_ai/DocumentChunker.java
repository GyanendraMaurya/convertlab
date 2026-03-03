package com.convertlab.convertlab_backend.service_ai;

import java.util.List;

public interface DocumentChunker {

    List<String> chunk(String cleanedText);
}
