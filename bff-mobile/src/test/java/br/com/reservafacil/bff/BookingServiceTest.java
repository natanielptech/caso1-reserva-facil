package br.com.reservafacil.bff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BookingServiceTest {
    private static final String KEY = "123e4567-e89b-12d3-a456-426614174003";
    private static final String HOTELS = "http://hotels";
    private static final String RESERVATIONS = "http://reservations";
    private static final String PAYMENTS = "http://payments";
    private RestTemplate http;
    private MockRestServiceServer server;
    private BookingService service;

    @BeforeEach
    void setUp() {
        http = new RestTemplate();
        server = MockRestServiceServer.createServer(http);
        service = new BookingService(http, HOTELS, RESERVATIONS, PAYMENTS);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void confirmaReservaComPrecoDaDiariaEChaveIdempotente() {
        expectCommonRequests(201, false);

        var result = service.confirmar(request(false), "hospede-123", KEY);

        assertThat(result.reserva().status()).isEqualTo("CONFIRMADA");
        assertThat(result.pagamento().valor()).isEqualByComparingTo("500.00");
        assertThat(result.hotel().fotoUrl()).isEqualTo("https://example.com/hoteis/1.jpg");
    }

    @Test
    void cancelaPreReservaQuandoPagamentoERecusado() {
        expectCommonRequests(402, true);
        server.expect(requestTo(RESERVATIONS + "/v1/reservas/88"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("X-User-Id", "hospede-123"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatThrownBy(() -> service.confirmar(request(true), "hospede-123", KEY))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.PAYMENT_REQUIRED);
    }

    private void expectCommonRequests(int paymentStatus, boolean decline) {
        server.expect(requestTo(HOTELS + "/v1/hoteis/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":1,"nome":"Hotel Central","cidade":"São Paulo",
                         "fotoUrl":"https://example.com/hoteis/1.jpg","comodidades":["Wi-Fi"]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(RESERVATIONS + "/v1/reservas/idempotencia/" + KEY))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", "hospede-123"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(RESERVATIONS
                        + "/v1/quartos/disponibilidade?hotel=1&entrada=2027-01-10&saida=2027-01-12"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":101,"hotelId":1,"nome":"Standard","valorDiaria":250.00}]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(RESERVATIONS + "/v1/reservas"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-Id", "hospede-123"))
                .andExpect(header("Idempotency-Key", KEY))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"id":88,"hotelId":1,"quartoId":101,"valorDiaria":250.00,"hospedeId":"hospede-123",
                         "entrada":"2027-01-10","saida":"2027-01-12","status":"PRE_RESERVADA",
                         "criadaEm":"2026-10-21T17:00:00Z","expiraEm":"2026-10-21T17:15:00Z"}
                        """));
        server.expect(requestTo(PAYMENTS + "/v1/pagamentos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", KEY))
                .andRespond(withStatus(HttpStatus.valueOf(paymentStatus)).contentType(MediaType.APPLICATION_JSON)
                        .body(decline ? """
                                {"id":99,"reservaId":88,"valor":500.00,"moeda":"BRL","metodo":"CARTAO",
                                 "status":"RECUSADO","criadaEm":"2026-10-21T17:00:00Z"}
                                """ : """
                                {"id":99,"reservaId":88,"valor":500.00,"moeda":"BRL","metodo":"CARTAO",
                                 "status":"APROVADO","criadaEm":"2026-10-21T17:00:00Z"}
                                """));
        if (!decline) {
            server.expect(requestTo(RESERVATIONS + "/v1/reservas/88/confirmacao"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {"id":88,"hotelId":1,"quartoId":101,"valorDiaria":250.00,
                             "hospedeId":"hospede-123","entrada":"2027-01-10","saida":"2027-01-12",
                             "status":"CONFIRMADA","criadaEm":"2026-10-21T17:00:00Z",
                             "expiraEm":"2026-10-21T17:15:00Z"}
                            """, MediaType.APPLICATION_JSON));
        }
    }

    private BookingService.BookingRequest request(boolean decline) {
        return new BookingService.BookingRequest(1L, 101L, LocalDate.parse("2027-01-10"),
                LocalDate.parse("2027-01-12"), BookingService.PaymentMethod.CARTAO, decline);
    }
}
