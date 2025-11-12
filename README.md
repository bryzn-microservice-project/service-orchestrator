################################################################
#                                                              #
#                   SERVICE-ORCHESTRATOR                       #
#                                                              #
################################################################


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
