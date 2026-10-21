package br.com.reservafacil.hoteis;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/v1/hoteis")
public class HotelController {
    private final HotelRepository repository;

    public HotelController(HotelRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    public HotelResponse buscar(@PathVariable Long id) {
        Hotel hotel = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel não encontrado"));
        return HotelResponse.from(hotel);
    }

    @GetMapping
    public List<HotelResponse> listar() {
        return repository.findAll().stream().map(HotelResponse::from).toList();
    }

    public record HotelResponse(Long id, String nome, String cidade, String fotoUrl, List<String> comodidades) {
        static HotelResponse from(Hotel hotel) {
            return new HotelResponse(hotel.getId(), hotel.getNome(), hotel.getCidade(), hotel.getFotoUrl(),
                    List.of(hotel.getComodidades().split(",")));
        }
    }
}
