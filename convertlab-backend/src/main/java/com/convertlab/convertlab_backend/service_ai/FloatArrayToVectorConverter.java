package com.convertlab.convertlab_backend.service_ai;

import com.pgvector.PGvector;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;
import java.util.Arrays;

@Converter
public class FloatArrayToVectorConverter implements AttributeConverter<float[], Object> {

    @Override
    public Object convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        try {
            PGobject out = new PGobject();
            out.setType("vector");
            out.setValue(Arrays.toString(attribute));
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert float[] to PGvector", e);
        }
    }

    @Override
    public float[] convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }

        // Case 1: Driver returns the specialized PGvector object
        if (dbData instanceof PGvector pgv) {
            return pgv.toArray();
        }

        // Case 2: Driver returns a String (e.g., "[0.1, 0.2, 0.3]")
        if (dbData instanceof String str) {
            if (str.isEmpty() || str.equals("[]")) {
                return new float[0];
            }

            // Remove brackets and split by comma
            String[] parts = str.substring(1, str.length() - 1).split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            return result;
        }

        throw new IllegalArgumentException("Unsupported type for vector conversion: " + dbData.getClass());
    }
}
