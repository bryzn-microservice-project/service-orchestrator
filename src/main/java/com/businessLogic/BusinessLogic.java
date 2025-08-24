package com.businessLogic;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.topics.MovieTicketRequest;
import com.topics.MovieTicketResponse;
import com.topics.PaymentRequest;
import com.topics.SeatRequest;

/*
 * Handles the business logic for processing various topics and utilizes 
 * REST clients to communicate with other microservices.
 */
@Service
public class BusinessLogic {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessLogic.class);

    // REST Clients to communicate with other microservices
    private RestClient apiGatewayClient = RestClient.create();
    private RestClient paymentServiceClient = RestClient.create();
    private RestClient movieServiceClient = RestClient.create();
    private RestClient seatServiceClient = RestClient.create();

    // UNIQUE Client for ticketID generation
    private RestClient ticketMangerClient = RestClient.create();

    private HashMap<String, RestClient> restRouter = new HashMap<>();
    private HashMap<RestClient, String> restEndpoints = new HashMap<>();

    public BusinessLogic() {
        mapTopicsToClient();
    }

    /*
     * Method to map topics to their respective microservices and endpoints
     * # api-gateway:8081
     * # movie-service:8082
     * # notification-service:8083
     * # payment-service:8084
     * # seating-service:8085
     * # user-management-service:8086
     * # gui-service:8087
     * # ticketing-manager:8088
     * # service-orchestrator:8089
     */
    public void mapTopicsToClient() {
        restRouter.put("MovieTicketResponse", apiGatewayClient);
        restEndpoints.put(apiGatewayClient, "http://api-gateway:8081/api/v1/processTopic");

        restRouter.put("MovieTicketRequest", movieServiceClient);
        restEndpoints.put(movieServiceClient, "http://movie-service:8082/api/v1/processTopic");

        restRouter.put("PaymentRequest", paymentServiceClient);
        restEndpoints.put(paymentServiceClient, "http://payment-service:8084/api/v1/processTopic");

        restRouter.put("SeatRequest", seatServiceClient);
        restEndpoints.put(seatServiceClient, "http://seat-service:8085/api/v1/processTopic");

        LOG.info("Sucessfully mapped the topics to their respective microservices...");
    }

    /*
     * Request handlers for the various topics, which communicate through REST
     * clients
     */

    public ResponseEntity<String> orchestrate(MovieTicketRequest movieRequest) {
        LOG.info("Orchestrating the {MovieTicketRequest}...");

        // FIRST TRANSACTION - MOVIE TICKET REQUEST
        // SECOND TRANSACTION - SEAT REQUEST
        // THIRD TRANSACTION - PAYMENT REQUEST
        // FOURTH TRANSACTION - MOVIE TICKET RESPONSE

        ResponseEntity<String> movieResponse = createMovieRequest(movieRequest);
        if (movieResponse.getStatusCode() == HttpStatus.OK) {
            LOG.info("{MovieTicketRequest} processed successfully. Now creating {SeatRequest}...");
        } else {
            LOG.error("Failed to process {MovieTicketRequest}... Ending the transaction.");
            return handleFailedResponses(1);
        }

        ResponseEntity<String> seatResponse = createSeatRequest(movieRequest);
        if (seatResponse.getStatusCode() == HttpStatus.OK) {
            LOG.info("{SeatRequest} processed successfully. Now creating {PaymentRequest}...");
        } else {
            LOG.error("Failed to process {SeatRequest}... Ending the transaction.");
            return handleFailedResponses(2);
        }

        ResponseEntity<String> paymentResponse = createPaymentRequest(movieRequest);
        if (paymentResponse.getStatusCode() == HttpStatus.OK) {
            LOG.info("{PaymentRequest} processed successfully. Transaction complete!");
        } else {
            LOG.error("Failed to process {PaymentRequest}... Ending the transaction.");
            return handleFailedResponses(3);
        }

        ResponseEntity<String> apiGatewayResponse = createMovieTicketResponse(movieRequest);
        if (apiGatewayResponse.getStatusCode() == HttpStatus.OK) {
            LOG.info("{MovieTicketResponse} sent back to API Gateway successfully. End of Orchestration.");
        } else {
            LOG.error("Failed to send {MovieTicketResponse} back to API Gateway... End of Orchestration.");
            return handleFailedResponses(4);
        }

        return new ResponseEntity<>("Orchestration completed successfully!", HttpStatus.OK);
    }

    public ResponseEntity<String> createMovieRequest(MovieTicketRequest movieRequest) {
        // MOCK RETURN
        // return mockResponse();

        LOG.info("Received a MovieTicketRequest. Nothing to do here... Forwarding to the Movie Service");
        ResponseEntity<String> movieServiceResponse = restRouter.get("MovieTicketRequest")
                .post()
                .uri(restEndpoints.get(restRouter.get("MovieTicketRequest")))
                .contentType(MediaType.APPLICATION_JSON)
                .body(movieRequest)
                .retrieve()
                .toEntity(String.class);
        LOG.info("MovieRequest processed with status: " + movieServiceResponse.getStatusCode());
        return movieServiceResponse;
    }

    private ResponseEntity<String> createSeatRequest(MovieTicketRequest movieRequest) {
        // MOCK RETURN
        // return mockResponse();
        
        LOG.info("Creating a SeatRequest based on the MovieTicketRequest...");
        SeatRequest seatRequest = new SeatRequest();
        seatRequest.setTopicName("SeatRequest");
        seatRequest.setCorrelatorId(movieRequest.getCorrelatorId());
        seatRequest.setMovieName(movieRequest.getMovie().getMovieName());
        seatRequest.setShowtime(movieRequest.getMovie().getShowtime());

        LOG.info("Sending a SeatRequest to the [Seating Service]");

        ResponseEntity<String> seatServiceResponse = restRouter.get("SeatRequest")
                .post()
                .uri(restEndpoints.get(restRouter.get("SeatRequest")))
                .contentType(MediaType.APPLICATION_JSON)
                .body(movieRequest)
                .retrieve()
                .toEntity(String.class);
        LOG.info("SeatRequest processed with status: " + seatServiceResponse.getStatusCode());
        return seatServiceResponse;
    }

    private ResponseEntity<String> createPaymentRequest(MovieTicketRequest movieRequest) {
        // MOCK RETURN
        // return mockResponse();

        LOG.info("Creating a PaymentRequest based on the MovieTicketRequest...");
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setTopicName("PaymentRequest");
        paymentRequest.setCorrelatorId(movieRequest.getCorrelatorId());
        paymentRequest.setEmail(movieRequest.getPayment().getEmail());
        paymentRequest.setPaymentAmount(movieRequest.getPayment().getPaymentAmount());
        paymentRequest.setCreditCard(movieRequest.getPayment().getCreditCard());
        paymentRequest.setCvc(movieRequest.getPayment().getCvc());

        LOG.info("Sending a PaymentRequest to the [Payment Service]");

        ResponseEntity<String> paymentServiceResponse = restRouter.get("PaymentRequest")
                .post()
                .uri(restEndpoints.get(restRouter.get("PaymentRequest")))
                .contentType(MediaType.APPLICATION_JSON)
                .body(paymentRequest)
                .retrieve()
                .toEntity(String.class);
        LOG.info("PaymentRequest processed with status: " + paymentServiceResponse.getStatusCode());
        return paymentServiceResponse;
    }

    private ResponseEntity<String> createMovieTicketResponse(MovieTicketRequest movieRequest) {
        // MOCK RETURN
        // return new ResponseEntity<>("BRY123", HttpStatus.OK);
        
        LOG.info("Creating a MovieTicketResponse to send back to the API Gateway...");
        MovieTicketResponse movieResponse = new MovieTicketResponse();
        movieResponse.setTopicName("MovieTicketResponse");
        movieResponse.setCorrelatorId(movieRequest.getCorrelatorId());
        movieResponse.setMovie(movieRequest.getMovie());
        movieResponse.setSeatNumber(movieRequest.getSeatNumber());
        movieResponse.setTicketId(ticketRequest());

        LOG.info("Sending a MovieTicketResponse to the [API Gateway Service]");

        ResponseEntity<String> apiGatewayResponse = restRouter.get("MovieTicketResponse")
                .post()
                .uri(restEndpoints.get(restRouter.get("MovieTicketResponse")))
                .contentType(MediaType.APPLICATION_JSON)
                .body(movieResponse)
                .retrieve()
                .toEntity(String.class);
        LOG.info("MovieTicketResponse processed with status: " + apiGatewayResponse.getStatusCode());
        return apiGatewayResponse;
    }

    private int ticketRequest() {
        mockTicket();
        ResponseEntity<String> ticketManagerResponse = ticketMangerClient
                .post()
                .uri(restEndpoints.get(restRouter.get("MovieTicketResponse")))
                .contentType(MediaType.APPLICATION_JSON)
                .body(null)
                .retrieve()
                .toEntity(String.class);

        return Integer.parseInt(ticketManagerResponse.getBody());
    }

    private ResponseEntity<String> handleFailedResponses(int stage) {
        String failedService = "";
        switch (stage) {
            case 1:
                failedService = "Movie Service";
                break;
            case 2:
                failedService = "Seating Service";
                break;
            case 3:
                failedService = "Payment Service";
                break;
            case 4:
                failedService = "API Gateway Service";
            default:
                LOG.error("Invalid stage for handling.");
                return new ResponseEntity<>("Invalid stage", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Orchestration failed at the " + failedService, HttpStatus.SERVICE_UNAVAILABLE);
    }

    // MOCK METHODS - To be removed when actual implementation is done
    private ResponseEntity<String> mockResponse() {
        return new ResponseEntity<>("Mock response from BusinessLogic", HttpStatus.OK);
    }

    private ResponseEntity<String> mockTicket() {
        return new ResponseEntity<>("BRY123", HttpStatus.OK);
    }
}
