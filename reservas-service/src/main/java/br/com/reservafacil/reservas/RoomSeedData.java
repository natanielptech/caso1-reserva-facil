package br.com.reservafacil.reservas;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
class RoomSeedData {
    @Bean
    CommandLineRunner seedRooms(RoomRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Room(101L, 1L, "Standard", new BigDecimal("250.00")));
                repository.save(new Room(102L, 1L, "Deluxe", new BigDecimal("390.00")));
                repository.save(new Room(201L, 2L, "Casal", new BigDecimal("320.00")));
            }
        };
    }
}
