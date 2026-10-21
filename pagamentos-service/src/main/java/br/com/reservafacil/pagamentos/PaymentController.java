package br.com.reservafacil.pagamentos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/pagamentos")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PaymentService.PaymentResponse> criar(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        if (!PaymentService.chaveValida(key)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key deve ser um UUID válido");
        }
        PaymentService.CreatePaymentRequest command = new PaymentService.CreatePaymentRequest(
                request.reservaId(), request.valor(), request.moeda(), request.metodo(), request.simularRecusa());
        Payment payment = service.criar(command, key);
        HttpStatus status = payment.getStatus() == PaymentStatus.RECUSADO
                ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(PaymentService.PaymentResponse.from(payment));
    }

    @GetMapping("/{id}")
    public PaymentService.PaymentResponse obter(@PathVariable Long id) {
        return PaymentService.PaymentResponse.from(service.obter(id));
    }

    public record PaymentRequest(@NotNull Long reservaId, @NotNull @Positive BigDecimal valor,
                                @NotNull String moeda, @NotNull PaymentMethod metodo, boolean simularRecusa) {
    }
}
