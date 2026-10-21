package br.com.reservafacil.bff;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class BookingService {
    private static final String DEFAULT_PHOTO = "https://example.com/images/hotel-placeholder.jpg";
    private final RestTemplate http;
    private final String hotelsUrl;
    private final String reservationsUrl;
    private final String paymentsUrl;

    public BookingService(RestTemplate http,
                          @Value("${services.hoteis-url:http://localhost:8081}") String hotelsUrl,
                          @Value("${services.reservas-url:http://localhost:8082}") String reservationsUrl,
                          @Value("${services.pagamentos-url:http://localhost:8083}") String paymentsUrl) {
        this.http = http;
        this.hotelsUrl = hotelsUrl;
        this.reservationsUrl = reservationsUrl;
        this.paymentsUrl = paymentsUrl;
    }

    public BookingResponse confirmar(BookingRequest request, String userId, String idempotencyKey) {
        HotelResponse hotel;
        try {
            hotel = http.getForObject(hotelsUrl + "/v1/hoteis/{id}", HotelResponse.class, request.hotelId());
        } catch (ResourceAccessException exception) {
            hotel = new HotelResponse(request.hotelId(), "Hotel", null, DEFAULT_PHOTO, new String[0]);
        }

        String availabilityUrl = reservationsUrl
                + "/v1/quartos/disponibilidade?hotel={hotel}&entrada={entrada}&saida={saida}";
        http.getForObject(availabilityUrl, RoomResponse[].class,
                request.hotelId(), request.entrada(), request.saida());

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.set("X-User-Id", userId);
        userHeaders.set("Idempotency-Key", idempotencyKey);
        userHeaders.setContentType(MediaType.APPLICATION_JSON);
        ReservationResponse reservation = http.postForObject(reservationsUrl + "/v1/reservas",
                new HttpEntity<>(new ReservationRequest(request.hotelId(), request.quartoId(),
                        request.entrada(), request.saida()), userHeaders), ReservationResponse.class);
        if (reservation == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Serviço de reservas retornou resposta vazia");
        }

        if (reservation.status().equals("CANCELADA") || reservation.status().equals("EXPIRADA")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta tentativa foi encerrada; use uma nova Idempotency-Key");
        }
        BigDecimal amount = reservation.valorDiaria()
                .multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(request.entrada(), request.saida())));
        HttpHeaders paymentHeaders = new HttpHeaders();
        paymentHeaders.setContentType(MediaType.APPLICATION_JSON);
        paymentHeaders.set("Idempotency-Key", idempotencyKey);
        PaymentRequest paymentRequest = new PaymentRequest(reservation.id(), amount, "BRL",
                request.metodo(), request.simularRecusa());
        ResponseEntity<PaymentResponse> payment;
        try {
            payment = http.exchange(paymentsUrl + "/v1/pagamentos", HttpMethod.POST,
                    new HttpEntity<>(paymentRequest, paymentHeaders), PaymentResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                try {
                    http.exchange(reservationsUrl + "/v1/reservas/{id}", HttpMethod.DELETE,
                            new HttpEntity<>(userHeaders), Void.class, reservation.id());
                } catch (RuntimeException compensationFailure) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Pagamento recusado e não foi possível cancelar a pré-reserva", compensationFailure);
                }
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                        "Pagamento recusado; a pré-reserva foi cancelada");
            }
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Resultado do pagamento desconhecido; repita a solicitação com a mesma Idempotency-Key", exception);
        }
        if (payment.getBody() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Serviço de pagamentos retornou resposta vazia");
        }

        try {
            ReservationResponse confirmed = http.postForObject(
                    reservationsUrl + "/v1/reservas/{id}/confirmacao",
                    new HttpEntity<>(userHeaders), ReservationResponse.class, reservation.id());
            return new BookingResponse(hotel, confirmed, payment.getBody());
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Pagamento aprovado, mas não foi possível confirmar a reserva", exception);
        }
    }

    public record BookingRequest(Long hotelId, Long quartoId, LocalDate entrada, LocalDate saida,
                                 PaymentMethod metodo, boolean simularRecusa) {
    }

    public record HotelResponse(Long id, String nome, String cidade, String fotoUrl, String[] comodidades) {
    }

    public record RoomResponse(Long id, Long hotelId, String nome, BigDecimal valorDiaria) {
    }

    public record ReservationRequest(Long hotelId, Long quartoId, LocalDate entrada, LocalDate saida) {
    }

    public record ReservationResponse(Long id, Long hotelId, Long quartoId, BigDecimal valorDiaria,
                                      String hospedeId, LocalDate entrada,
                                      LocalDate saida, String status, String criadaEm, String expiraEm) {
    }

    public record PaymentRequest(Long reservaId, BigDecimal valor, String moeda, PaymentMethod metodo,
                                 boolean simularRecusa) {
    }

    public record PaymentResponse(Long id, Long reservaId, BigDecimal valor, String moeda, PaymentMethod metodo,
                                  String status, String criadaEm) {
    }

    public record BookingResponse(HotelResponse hotel, ReservationResponse reserva, PaymentResponse pagamento) {
    }

    public enum PaymentMethod {
        CARTAO, PIX
    }
}
