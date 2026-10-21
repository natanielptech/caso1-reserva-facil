package br.com.reservafacil.hoteis;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class HotelSeedData {
    @Bean
    CommandLineRunner seedHotels(HotelRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Hotel(1L, "Hotel Central", "São Paulo",
                        "https://example.com/hoteis/1.jpg", "Wi-Fi, piscina, café da manhã"));
                repository.save(new Hotel(2L, "Pousada Jardim", "Rio de Janeiro",
                        "https://example.com/hoteis/2.jpg", "Wi-Fi, estacionamento"));
            }
        };
    }
}
