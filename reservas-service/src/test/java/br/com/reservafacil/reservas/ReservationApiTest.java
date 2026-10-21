package br.com.reservafacil.reservas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationApiTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ReservationRepository repository;

    @BeforeEach
    void limparReservas() {
        repository.deleteAll();
    }

    @Test
    void protegeReservaPorTitularEReusaChaveDeIdempotencia() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "hospede-1");
        headers.set("Idempotency-Key", "123e4567-e89b-12d3-a456-426614174002");
        LocalDate entrada = LocalDate.now().plusDays(2);
        String body = """
                {"hotelId":1,"quartoId":101,"entrada":"%s","saida":"%s"}
                """.formatted(entrada, entrada.plusDays(2));
        String url = "http://localhost:" + port + "/v1/reservas";

        var first = http.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        var retry = http.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        Long id = ((Number) first.getBody().get("id")).longValue();
        HttpHeaders otherUser = new HttpHeaders();
        otherUser.set("X-User-Id", "hospede-2");
        var forbidden = http.exchange(url + "/" + id, HttpMethod.GET,
                new HttpEntity<>(otherUser), String.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(retry.getBody()).containsEntry("id", first.getBody().get("id"));
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(repository.count()).isEqualTo(1);
    }
}
