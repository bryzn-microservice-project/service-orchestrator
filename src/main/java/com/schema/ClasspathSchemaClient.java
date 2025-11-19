package com.schema;

import org.everit.json.schema.loader.SchemaClient;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

// This Schema Client is needed to resolve references
public class ClasspathSchemaClient implements SchemaClient {
    @Override
    public InputStream get(String url) {
        try {
            // Strip any leading protocol if needed
            String path = URI.create(url).getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            // Load from classpath
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(path);
            if (resource == null) {
                throw new RuntimeException("Schema not found in classpath: " + path);
            }
            return resource.openStream();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load schema from classpath: " + url, e);
        }
    }
}