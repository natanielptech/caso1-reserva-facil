package br.com.reservafacil.pagamentos;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junit5.Provider;
import au.com.dius.pact.provider.junit5.State;
import au.com.dius.pact.provider.junit5.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@Provider("pagamentos-service")
@PactFolder("../bff-mobile/target/pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentProviderPactTest {
    @LocalServerPort
    private int port;

    @Autowired
    private PaymentRepository repository;

    @BeforeEach
    void configurarAlvo(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @State("reserva pronta para pagamento")
    void prepararReserva() {
        repository.deleteAll();
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verificarContrato(PactVerificationContext context) {
        context.verifyInteraction();
    }
}
