package br.com.reservafacil.bff;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
class PaymentConsumerPactTest {
    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-12d3-a456-426614174000";

    @Pact(consumer = "bff-mobile", provider = "pagamentos-service")
    V4Pact pagamentoAprovado(PactBuilder builder) {
        PactDslJsonBody request = new PactDslJsonBody()
                .integerType("reservaId", 42)
                .decimalType("valor", 500.0)
                .stringValue("moeda", "BRL")
                .stringValue("metodo", "CARTAO")
                .booleanType("simularRecusa", false);
        PactDslJsonBody response = new PactDslJsonBody()
                .integerType("id", 23)
                .integerType("reservaId", 42)
                .decimalType("valor", 500.0)
                .stringValue("moeda", "BRL")
                .stringValue("metodo", "CARTAO")
                .stringValue("status", "APROVADO")
                .stringMatcher("criadaEm",
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z",
                        "2026-10-21T17:00:00Z");

        return builder.expectsToReceiveHttpInteraction("uma cobrança de reserva com chave de idempotência",
                interaction -> {
                    interaction.state("reserva pronta para pagamento");
                    interaction.withRequest(pactRequest -> pactRequest.method("POST")
                            .path("/v1/pagamentos")
                            .headers(Map.of("Content-Type", "application/json",
                                    "Idempotency-Key", IDEMPOTENCY_KEY))
                            .body(request));
                    interaction.willRespondWith(pactResponse -> pactResponse
                            .status(201)
                            .headers(Map.of("Content-Type", "application/json"))
                            .body(response));
                    return interaction;
                }).toPact();
    }

    @Test
    @PactTestFor(providerName = "pagamentos-service", port = "1234")
    void enviaCobrancaConformeContrato(MockServer mockServer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", IDEMPOTENCY_KEY);
        String body = """
                {"reservaId":42,"valor":500.00,"moeda":"BRL","metodo":"CARTAO","simularRecusa":false}
                """;

        var response = new RestTemplate().postForEntity(mockServer.getUrl() + "/v1/pagamentos",
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).containsEntry("status", "APROVADO");
    }
}
