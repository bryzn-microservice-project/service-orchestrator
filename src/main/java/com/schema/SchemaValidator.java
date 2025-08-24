package com.schema;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/*
 * Valiation Service for JSON schemas from the internal icd
 */
@Service
public class SchemaValidator {
    private static final String SCHEMA_BASE_PATH = "json-schema/";

    private final ResourceLoader resourceLoader;

    public SchemaValidator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public boolean validateJson(InputStream schemaStream, JSONObject jsonNode) {
        boolean valid = false;
        try {
            JSONObject rawSchema = new JSONObject(new JSONTokener(schemaStream));

            Schema schema = SchemaLoader.builder()
                    .schemaJson(rawSchema)
                    .resolutionScope("classpath:/" + SCHEMA_BASE_PATH)
                    .build()
                    .load()
                    .build();

            schema.validate(jsonNode);
            valid = true;
        } catch (ValidationException e) {
            System.out.println("Validation failed");
            List<String> errors = collectErrors(e);
            errors.forEach(err -> System.out.println(" - " + err));
            valid = false;
        }
        return valid;
    }

    public InputStream getSchemaStream(String jsonPath) {
        String location = "classpath:" + jsonPath;
        Resource resource = resourceLoader.getResource(location);
        System.out.println("Loading schema at: " + location + " | Exists? " + resource.exists());
        try {
            return resource.exists() ? resource.getInputStream() : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> collectErrors(ValidationException e) {
        List<String> errors = new ArrayList<>();
        if (e.getCausingExceptions().isEmpty()) {
            // leaf node = actual error
            errors.add(e.getMessage());
        } else {
            // recurse into children
            for (ValidationException cause : e.getCausingExceptions()) {
                errors.addAll(collectErrors(cause));
            }
        }
        return errors;
    }

}
