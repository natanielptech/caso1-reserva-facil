package br.com.reservafacil.pagamentos;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final TransactionTemplate transactions;

    public PaymentService(PaymentRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public synchronized Payment criar(CreatePaymentRequest request, String key) {
        try {
            return transactions.execute(status -> {
                Payment existente = repository.findByIdempotencyKey(key).orElse(null);
                if (existente != null) {
                    return existente;
                }
                Payment payment = new Payment(request.reservaId(), request.valor(), request.moeda(),
                        request.metodo(), request.simularRecusa()
                        ? PaymentStatus.RECUSADO : PaymentStatus.APROVADO, key, Instant.now());
                return repository.saveAndFlush(payment);
            });
        } catch (DataIntegrityViolationException exception) {
            return repository.findByIdempotencyKey(key).orElseThrow(() -> exception);
        }
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
            return UUID.fromString(key).toString().equalsIgnoreCase(key);
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
