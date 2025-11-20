package com.businessLogic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topics.Movie;
import com.topics.Movie.Genre;
import com.topics.MovieTicketRequest;
import com.topics.SeatRequest;
import com.topics.SeatResponse;
import com.topics.SeatResponse.Status;

@ExtendWith(MockitoExtension.class)
public class SendSeatRequestTest {
	@InjectMocks
	private BusinessLogic businessLogic;
	@Mock
	private RestClient seatingServiceClient;

	private ObjectMapper objectMapper = new ObjectMapper();

	@BeforeAll
	static void setUp() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	@Test
	@DisplayName("[BUSINESS_LOGIC] Valid SendTicketRequest")
	public void validRequest(TestInfo testInfo) {
		System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
		String JSON = """
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
					"email": "dummyemail@gmail.com",
					"creditCard": "6011000990139424",
					"cvc": "321"
				}
			}
			""";
		
		MovieTicketRequest request = null;	
		try{
			request = objectMapper.readValue(JSON, MovieTicketRequest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ZoneId cst;
		try {
			cst = ZoneId.of("America/Chicago");
		} catch (DateTimeException e) {
			cst = ZoneId.of("US/Central"); // fallback for Windows
		}
		ZoneId utc = ZoneId.of("UTC");

		// Create a ZonedDateTime in CST
		ZonedDateTime showtimeCST = ZonedDateTime.of(2025, 11, 10, 19, 30, 0, 0, cst);

		// Convert it to the same instant in UTC, then to LocalDateTime
		LocalDateTime showtime = showtimeCST.withZoneSameInstant(utc).toLocalDateTime();
		Date date = Date.from(showtime.atZone(ZoneId.systemDefault()).toInstant());

		Movie movie = new Movie();
		movie.setMovieName("Inception");
		movie.setGenre(Genre.SCIFI);
		movie.setShowtime(date);

        SeatResponse seatResponse = new SeatResponse();
        seatResponse.setMovieName("Inception");
        seatResponse.setSeatNumber("E6");
        seatResponse.setStatus(Status.HOLDING);
        seatResponse.setTopicName("SeatResponse");
        seatResponse.setShowtime(date);
        seatResponse.setTimestamp(date); // mock timestamp

		// REST CLIENT MOCK FOR THE MOVIE SERVICE
		RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
		RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
		RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

		when(seatingServiceClient.post()).thenReturn(uriSpec);
		when(uriSpec.uri(ArgumentMatchers.<String>any())).thenReturn(bodySpec); // this line is crucial, or else URI error
		when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
		when(bodySpec.body(any(SeatRequest.class))).thenReturn(bodySpec);
		when(bodySpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok(toJson(seatResponse)));

		SeatResponse rsp = businessLogic.sendSeatRequest(request);
		assertNotNull(rsp);
        Assertions.assertEquals("Inception", rsp.getMovieName());
        Assertions.assertEquals("E6", rsp.getSeatNumber());
        Assertions.assertEquals(date, rsp.getTimestamp());
        Assertions.assertEquals(Status.HOLDING, rsp.getStatus());
	}

    // Helper method to serialize an object to JSON string
    private String toJson(Object obj) {
        try {
            // Use Jackson ObjectMapper to convert the object to JSON
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(obj);  // Convert object to JSON string
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{\"error\":\"Error processing JSON\"}";
        }
    }
}
