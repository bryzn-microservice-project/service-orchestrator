package com.schema;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class SchemaValidator {

    private final ResourceLoader resourceLoader;

    public SchemaValidator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public boolean validateJson(InputStream schemaStream, JSONObject jsonNode) {
        try {
            JSONObject rawSchema = new JSONObject(new JSONTokener(schemaStream));

            // Set the base URL to the folder containing the schemas
            URL baseUrl = getClass().getClassLoader().getResource("json-schema");
            System.out.println("Base URL: " + baseUrl);
            if (baseUrl == null) {
                throw new RuntimeException("Could not locate json-schema folder in classpath");
            }

            Schema schema = SchemaLoader.builder()
                .schemaJson(rawSchema)
                .resolutionScope("classpath:/json-schema/") // base URI for resolving $ref
                .schemaClient(new ClasspathSchemaClient()) 
                .build()
                .load()
                .build();

            schema.validate(jsonNode);
            return true;
        } catch (ValidationException e) {
            System.out.println("Validation failed");
            List<String> errors = collectErrors(e);
            errors.forEach(err -> System.out.println(" - " + err));
            return false;
        }
    }

    private List<String> collectErrors(ValidationException e) {
        List<String> errors = new ArrayList<>();
        if (e.getCausingExceptions().isEmpty()) {
            errors.add(e.getMessage());
        } else {
            for (ValidationException cause : e.getCausingExceptions()) {
                errors.addAll(collectErrors(cause));
            }
        }
        return errors;
    }

    public InputStream getSchemaStream(String schemaPath) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + schemaPath);
            if (!resource.exists()) {
                System.out.println("Schema not found: " + schemaPath);
                return null;
            }
            return resource.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
