package com.convertlab.convertlab_backend.service_ai;

import java.util.List;

public interface EmbeddingProvider {

    /**
     * Generate embedding for single text
     */
    float[] embed(String text);

    /**
     * Generate embeddings for multiple texts (batch support)
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * Name of the provider model (e.g. text-embedding-3-small)
     */
    String modelName();

    /**
     * Embedding dimension (e.g. 1536)
     */
    int dimension();
}
