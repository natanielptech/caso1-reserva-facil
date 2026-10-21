package br.com.reservafacil.pagamentos;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public synchronized Payment criar(CreatePaymentRequest request, String key) {
        Payment existente = repository.findByIdempotencyKey(key).orElse(null);
        if (existente != null) {
            return existente;
        }
        Payment payment = repository.saveAndFlush(new Payment(request.reservaId(), request.valor(),
                request.moeda(), request.metodo(), request.simularRecusa()
                ? PaymentStatus.RECUSADO : PaymentStatus.APROVADO, key, Instant.now()));
        return payment;
    }

    public Payment obter(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento não encontrado"));
    }

    public static boolean chaveValida(String key) {
        if (key == null) {
            return false;
        }
        try {
            UUID.fromString(key);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public record CreatePaymentRequest(Long reservaId, java.math.BigDecimal valor, String moeda,
                                       PaymentMethod metodo, boolean simularRecusa) {
        public boolean simularRecusa() {
            return simularRecusa;
        }
    }

    public record PaymentResponse(Long id, Long reservaId, java.math.BigDecimal valor, String moeda,
                                  PaymentMethod metodo, PaymentStatus status, Instant criadaEm) {
        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(payment.getId(), payment.getReservaId(), payment.getValor(),
                    payment.getMoeda(), payment.getMetodo(), payment.getStatus(), payment.getCriadaEm());
        }
    }
}
