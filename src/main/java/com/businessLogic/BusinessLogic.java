package com.businessLogic;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topics.CreateTicketRequest;
import com.topics.CreateTicketResponse;
import com.topics.MovieTicketRequest;
import com.topics.MovieTicketResponse;
import com.topics.PaymentRequest;
import com.topics.SeatRequest;
import jakarta.annotation.PostConstruct;

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

    private HashMap<String, RestClient> restRouter = new HashMap<>();
    private HashMap<RestClient, String> restEndpoints = new HashMap<>();

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
     * # session-manager:8090
     */
    @Value("${api.gateway}")
    private String apigateway;
    @Value("${api.gateway.port}")
    private String apigatewayPort;
    private String agw;

    @Value("${payment.service}")
    private String paymentService;
    @Value("${payment.service.port}")
    private String paymentServicePort;
    private String ps;

    @Value("${movie.service}")
    private String movieService;
    @Value("${movie.service.port}")
    private String movieServicePort;
    private String ms;

    @Value("${seating.service}")
    private String seatingService;
    @Value("${seating.service.port}")
    private String seatingServicePort;
    private String ss;

    @PostConstruct
    public void init() {
        agw = "http://" + apigateway + ":" + apigatewayPort + "/api/v1/processTopic";
        LOG.info("Business Logic initialized API Gateway at: " + agw);
        restEndpoints.put(apiGatewayClient, agw);
        restRouter.put("MovieTicketResponse", apiGatewayClient);

        ps = "http://" + paymentService + ":" + paymentServicePort + "/api/v1/processTopic";
        LOG.info("Business Logic initialized Payment Service at: " + ps);
        restEndpoints.put(paymentServiceClient, ps);
        restRouter.put("PaymentRequest", paymentServiceClient);

        ms = "http://" + movieService + ":" + movieServicePort + "/api/v1/processTopic";
        LOG.info("Business Logic initialized Movie Service at: " + ms);
        restEndpoints.put(movieServiceClient, ms);
        restRouter.put("CreateTicketRequest", movieServiceClient);

        ss = "http://" + seatingService + ":" + seatingServicePort + "/api/v1/";
        LOG.info("Business Logic initialized Seating Service at: " + ss);
        restEndpoints.put(seatServiceClient, ss);
        restRouter.put("SeatRequest", seatServiceClient);
    }

    /*
     * Request handlers for the various topics, which communicate through REST
     * clients
     */

    public ResponseEntity<String> orchestrate(MovieTicketRequest movieRequest) {
        LOG.info("Orchestrating the {MovieTicketRequest}...");

        // FIRST TRANSACTION - SEAT REQUEST
        // SECOND TRANSACTION - PAYMENT REQUEST
        //     * SEND CONFIRMATION TO SEATING SERVICE TO UPDATE SEAT STATUS TO BOOKED
        // THIRD TRANSACTION - CREATE TICKET REQUEST
        // FOURTH TRANSACTION - MOVIE TICKET RESPONSE

        ResponseEntity<String> seatResponse = createSeatRequest(movieRequest);
        if (seatResponse.getStatusCode() == HttpStatus.OK) {
            LOG.info("{SeatRequest} processed successfully. Now creating {PaymentRequest}...");
        } else {
            LOG.error("Failed to process {SeatRequest}... Ending the transaction.");
            return handleFailedResponses(1);
        }

        ResponseEntity<String> paymentResponse = createPaymentRequest(movieRequest);
        if (paymentResponse.getStatusCode() == HttpStatus.OK) {
            LOG.info("{PaymentRequest} processed successfully. Now creating {CreateTicketRequest}...");

            // sending confirmation to the seating service to update the seat status to BOOKED
            ResponseEntity<String> confirmationResponse = createConfirmationResponse(movieRequest.getCorrelatorId());
            if(confirmationResponse.getStatusCode() == HttpStatus.OK)
                LOG.info("Seat status updated to BOOKED successfully.");
            else
            {
                LOG.error("Failed to update seat status to BOOKED.");
                return handleFailedResponses(2);
            }
        } else {
            LOG.error("Failed to process {PaymentRequest}... Ending the transaction.");
            return handleFailedResponses(3);
        }

        CreateTicketResponse ticketResponse = createTicketRequest(movieRequest);
        if (ticketResponse.getTicketId() != null) {
            LOG.info("{CreateTicketResponse} processed successfully. Now creating {MovieTicketResponse}...");
        } else {
            LOG.error("Failed to process {CreateTicketResponse}... Ending the transaction.");
            return handleFailedResponses(4);
        }

        ResponseEntity<String> apiGatewayResponse = createMovieTicketResponse(movieRequest, ticketResponse.getTicketId());
        if (apiGatewayResponse.getStatusCode() == HttpStatus.OK) {
            LOG.info("{MovieTicketResponse} sent back to API Gateway successfully. End of Orchestration.");
        } else {
            LOG.error("Failed to send {MovieTicketResponse} back to API Gateway... End of Orchestration.");
            return handleFailedResponses(5);
        }

        return new ResponseEntity<>("Orchestration completed successfully!", HttpStatus.OK);
    }

    public CreateTicketResponse createTicketRequest(MovieTicketRequest movieRequest) {
        LOG.info("Received a CreateTicketRequest. Nothing to do here... Forwarding to the Movie Service");

        CreateTicketRequest request = new CreateTicketRequest();
        request.setTopicName("CreateTicketRequest");
        request.setMovie(movieRequest.getMovie());
        request.setSeatNumber(movieRequest.getSeatNumber());
        request.setCorrelatorId(movieRequest.getCorrelatorId());

        ResponseEntity<String> movieServiceResponse = restRouter.get("CreateTicketRequest")
                .post()
                .uri(restEndpoints.get(restRouter.get("CreateTicketRequest")))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);
        LOG.info("MovieRequest processed with status: " + movieServiceResponse.getStatusCode());

        ObjectMapper mapper = new ObjectMapper();
        CreateTicketResponse response = new CreateTicketResponse();
        try {
            if (movieServiceResponse.getBody() != null) {
                response = mapper.readValue(movieServiceResponse.getBody(), CreateTicketResponse.class);
            } else {
                LOG.error("Movie Service returned null/empty body for CreateTicketRequest");
            }
        } catch (JsonProcessingException e) {
            LOG.error("Failed to parse CreateTicketResponse from Movie Service", e);
        }
        return response;
    }

    private ResponseEntity<String> createSeatRequest(MovieTicketRequest movieRequest) {    
        LOG.info("Creating a SeatRequest based on the MovieTicketRequest...");
        SeatRequest seatRequest = new SeatRequest();
        seatRequest.setTopicName("SeatRequest");
        seatRequest.setCorrelatorId(movieRequest.getCorrelatorId());
        seatRequest.setMovieName(movieRequest.getMovie().getMovieName());
        seatRequest.setShowtime(movieRequest.getMovie().getShowtime());
        seatRequest.setSeatNumber(movieRequest.getSeatNumber());

        LOG.info("Sending a SeatRequest to the [Seating Service]");

        ResponseEntity<String> seatServiceResponse = restRouter.get("SeatRequest")
                .post()
                .uri(restEndpoints.get(restRouter.get("SeatRequest")) + "processTopic")
                .contentType(MediaType.APPLICATION_JSON)
                .body(seatRequest)
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

    private ResponseEntity<String> createMovieTicketResponse(MovieTicketRequest movieRequest, int ticket) {
        // MOCK RETURN
        // return new ResponseEntity<>("BRY123", HttpStatus.OK);
        
        LOG.info("Creating a MovieTicketResponse to send back to the API Gateway...");
        MovieTicketResponse movieResponse = new MovieTicketResponse();
        movieResponse.setTopicName("MovieTicketResponse");
        movieResponse.setCorrelatorId(movieRequest.getCorrelatorId());
        movieResponse.setMovie(movieRequest.getMovie());
        movieResponse.setSeatNumber(movieRequest.getSeatNumber());
        movieResponse.setTicketId(ticket);

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

    private ResponseEntity<String> createConfirmationResponse(int correlatorId)
    {
        return seatServiceClient
                .post()
                .uri(restEndpoints.get(restRouter.get("SeatRequest")) + "confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(correlatorId)
                .retrieve()
                .toEntity(String.class);
    }

    private ResponseEntity<String> handleFailedResponses(int stage) {
        String failedService = "";
        switch (stage) {
            case 1:
                failedService = "Seating Service";
                break;
            case 2:
                failedService = "Seating Service (BOOKING)";
                break;
            case 3:
                failedService = "Payment Service";
                break;
            case 4:
                failedService = "Movie Service";
                break;
            case 5:
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
}
