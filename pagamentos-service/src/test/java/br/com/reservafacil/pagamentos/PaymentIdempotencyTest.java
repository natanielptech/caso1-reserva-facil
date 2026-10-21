package br.com.reservafacil.pagamentos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentIdempotencyTest {
    @LocalServerPort
    private int port;

    @Autowired
    private PaymentRepository repository;

    @Autowired
    private TestRestTemplate http;

    @Test
    void mesmaChaveRetornaResultadoExistenteSemNovaCobranca() {
        repository.deleteAll();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "123e4567-e89b-12d3-a456-426614174001");
        String body = """
                {"reservaId":77,"valor":250.00,"moeda":"BRL","metodo":"PIX","simularRecusa":false}
                """;

        var first = http.postForEntity("http://localhost:" + port + "/v1/pagamentos",
                new HttpEntity<>(body, headers), Map.class);
        var second = http.postForEntity("http://localhost:" + port + "/v1/pagamentos",
                new HttpEntity<>(body, headers), Map.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getBody()).containsEntry("id", first.getBody().get("id"));
        assertThat(repository.count()).isEqualTo(1);
    }
}
