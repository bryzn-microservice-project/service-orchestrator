package com.schemaValidator;

import java.io.InputStream;
import java.net.URL;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import com.SchemaService;
import com.schema.SchemaValidator;  

@SpringBootTest(classes = SchemaValidator.class)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
class SchemaValidatorTest {
    @Autowired
    private ResourceLoader resourceLoader;

    private SchemaValidator schemaValidator;

    @BeforeEach
    void setup() {
        schemaValidator = new SchemaValidator(resourceLoader);
    }

    @Test
    @DisplayName("[SCHEMA] Valid MovieTicketRequest")
    void testMovieTicketRequest(TestInfo testInfo) throws Exception {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
        // JSON that satisfies the schema
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "MovieTicketRequest",
                "correlatorId": 1001,
                "movie": {
                    "movieName": "Inception",
                    "showtime": "2025-11-10T19:30:00-06:00",
                    "genre": "SCIFI"
                },
                "seatNumber": "E6",
                "price": 12.50,
                "payment": {
                    "topicName": "PaymentRequest",
                    "correlatorId": 1001,
                    "paymentAmount": 12.50,
                    "email": "bryzntest@gmail.com",
                    "creditCard": "6011000990139424",
                    "cvc": "321"
                }
            }
            """);

        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "MovieTicketRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertTrue(validate(topicName, validJson));
    }

    @Test
    @DisplayName("[SCHEMA] Invalid MovieTicketRequest (no payment)")
    void testBadMovieTicketRequest1(TestInfo testInfo) throws Exception {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
        // JSON that satisfies the schema
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "MovieTicketRequest",
                "correlatorId": 1001,
                "movie": {
                    "movieName": "Inception",
                    "showtime": "2025-11-10T19:30:00-06:00",
                    "genre": "SCIFI"
                },
                "seatNumber": "E6",
                "price": 12.50,
            }
        """);

        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "MovieTicketRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertFalse(validate(topicName, validJson));
    }

    @Test
    @DisplayName("[SCHEMA] Invalid MovieTicketRequest (bad seat)")
    void testBadMovieTicketRequest2(TestInfo testInfo) throws Exception {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
        // JSON that satisfies the schema
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "MovieTicketRequest",
                "correlatorId": 1001,
                "movie": {
                    "movieName": "Inception",
                    "showtime": "2025-11-10T19:30:00-06:00",
                    "genre": "SCIFI"
                },
                "seatNumber": "EE6",
                "price": 12.50,
                "payment": {
                    "topicName": "PaymentRequest",
                    "correlatorId": 1001,
                    "paymentAmount": 12.50,
                    "email": "bryzntest@gmail.com",
                    "creditCard": "6011000990139424",
                    "cvc": "321"
                }
            }
        """);

        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "MovieTicketRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertFalse(validate(topicName, validJson));
    }

    @Test
    @DisplayName("[SCHEMA] Invalid MovieTicketRequest (bad showtime)")
    void testBadMovieTicketRequest3(TestInfo testInfo) throws Exception {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
        // JSON that satisfies the schema
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "MovieTicketRequest",
                "correlatorId": 1001,
                "movie": {
                    "movieName": "Inception",
                    "showtime": "2025-11-10T19:30:00-06:00",
                    "genre": "SCIFI"
                },
                "seatNumber": "EE6",
                "price": 12.50,
                "payment": {
                    "topicName": "PaymentRequest",
                    "correlatorId": 1001,
                    "paymentAmount": 12.50,
                    "email": "bryzntest@gmail.com",
                    "creditCard": "6011000990139424",
                    "cvc": "321"
                }
            }
        """);

        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "MovieTicketRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertFalse(validate(topicName, validJson));
    }

    @Test
    @DisplayName("[SCHEMA] Invalid MovieTicketRequest (bad genre)")
    void testBadMovieTicketRequest4(TestInfo testInfo) throws Exception {
        System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
        // JSON that satisfies the schema
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "MovieTicketRequest",
                "correlatorId": 1001,
                "movie": {
                    "movieName": "Inception",
                    "showtime": "October 28, 2025",
                    "genre": "FANTASTIC"
                },
                "seatNumber": "EE6",
                "price": 12.50,
                "payment": {
                    "topicName": "PaymentRequest",
                    "correlatorId": 1001,
                    "paymentAmount": 12.50,
                    "email": "bryzntest@gmail.com",
                    "creditCard": "6011000990139424",
                    "cvc": "321"
                }
            }
        """);

        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "MovieTicketRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertFalse(validate(topicName, validJson));
    }

    private boolean validate(String topicName, JSONObject validJson) {
        // Load schema stream from SchemaService
        InputStream schemaStream = schemaValidator.getSchemaStream(SchemaService.getPathFor(topicName));

        if (schemaStream == null) {
            throw new RuntimeException("Schema not found: " + topicName);
        }

        // Pass the stream directly
        return schemaValidator.validateJson(schemaStream, validJson);
    }
}
