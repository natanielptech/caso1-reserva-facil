package br.com.reservafacil.bff;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/app/v1/confirmacoes-reserva")
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BookingService.BookingResponse> confirmar(
            @Valid @RequestBody BookingService.BookingRequest request,
            @RequestHeader("X-User-Id") @NotBlank String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey == null || !validUuid(idempotencyKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key UUID é obrigatório para permitir novas tentativas seguras");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.confirmar(request, userId, idempotencyKey));
    }

    private boolean validUuid(String value) {
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
