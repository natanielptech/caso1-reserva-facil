package br.com.reservafacil.reservas;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1")
public class ReservationController {
    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @GetMapping("/quartos/disponibilidade")
    public List<ReservationService.RoomResponse> disponibilidade(@RequestParam Long hotel,
                                                                 @RequestParam LocalDate entrada,
                                                                 @RequestParam LocalDate saida) {
        return service.disponibilidade(hotel, entrada, saida);
    }

    @PostMapping("/reservas")
    public ResponseEntity<ReservationService.ReservationResponse> criar(
            @Valid @RequestBody ReservationService.CreateReservationRequest request,
            @RequestHeader("X-User-Id") String hospedeId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Reservation reservation = service.criar(request, hospedeId, idempotencyKey);
        return ResponseEntity.created(URI.create("/v1/reservas/" + reservation.getId()))
                .body(ReservationService.ReservationResponse.from(reservation));
    }

    @GetMapping("/reservas/{id}")
    public ReservationService.ReservationResponse obter(@PathVariable Long id,
                                                         @RequestHeader("X-User-Id") String hospedeId) {
        return ReservationService.ReservationResponse.from(service.obterDoTitular(id, hospedeId));
    }

    @PostMapping("/reservas/{id}/confirmacao")
    public ReservationService.ReservationResponse confirmar(@PathVariable Long id,
                                                             @RequestHeader("X-User-Id") String hospedeId) {
        return ReservationService.ReservationResponse.from(service.confirmar(id, hospedeId));
    }

    @DeleteMapping("/reservas/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id,
                                         @RequestHeader("X-User-Id") String hospedeId) {
        service.cancelar(id, hospedeId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }
}
