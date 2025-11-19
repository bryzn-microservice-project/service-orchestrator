package com.businessLogic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topics.AccountInfoRequest;
import com.topics.AccountInfoResponse;


@ExtendWith(MockitoExtension.class)
public class HandleFailedResponsesTest {
	@InjectMocks
	private BusinessLogic businessLogic;
	@Mock
	private RestClient sessionManagerClient;

	@Test
	@DisplayName("[BUSINESS_LOGIC] Valid Responses")
	public void validResponses(TestInfo testInfo) {
		System.out.println("\n-----------Running: " + testInfo.getDisplayName() + "-----------");
		ResponseEntity<String> rsp1 = businessLogic.handleFailedResponses(1);
		Assertions.assertEquals(HttpStatusCode.valueOf(409), rsp1.getStatusCode());

		ResponseEntity<String> rsp2 = businessLogic.handleFailedResponses(2);
		Assertions.assertEquals(HttpStatusCode.valueOf(500), rsp2.getStatusCode());

		ResponseEntity<String> rsp3 = businessLogic.handleFailedResponses(3);
		Assertions.assertEquals(HttpStatusCode.valueOf(502), rsp3.getStatusCode());

		ResponseEntity<String> rsp4 = businessLogic.handleFailedResponses(4);
		Assertions.assertEquals(HttpStatusCode.valueOf(502), rsp4.getStatusCode());

		ResponseEntity<String> rsp5 = businessLogic.handleFailedResponses(5);
		Assertions.assertEquals(HttpStatusCode.valueOf(502), rsp5.getStatusCode());

		ResponseEntity<String> rsp6 = businessLogic.handleFailedResponses(2342342);
		Assertions.assertEquals(HttpStatusCode.valueOf(400), rsp6.getStatusCode());
	}
}
