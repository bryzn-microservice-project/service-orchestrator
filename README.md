################################################################
#                                                              #
#                   SERVICE-ORCHESTRATOR                       #
#                                                              #
################################################################


{
  "topicName": "MovieTicketRequest",
  "correlatorId": 12345,
  "ticketId": 67890,
  "movie": {
    "movieName": "Inception",
    "showtime": "2025-08-23T19:30:00Z"
  },
  "seatNumber": "A12",
  "price": 12.50,
  "payment": {
    "topicName": "payment.process",
    "correlatorId": 12345,
    "paymentAmount": 12.50,
    "email": "customer@example.com",
    "creditCard": "4111111111111111",
    "cvc": "123"
  }
}
