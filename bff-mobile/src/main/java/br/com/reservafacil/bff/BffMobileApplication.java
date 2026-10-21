package br.com.reservafacil.bff;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "ReservaFácil Mobile BFF", version = "v1",
        description = "BFF móvel para orquestração de reservas. Campos opcionais são compatíveis; mudanças incompatíveis requerem nova versão URI."))
public class BffMobileApplication {
    public static void main(String[] args) {
        SpringApplication.run(BffMobileApplication.class, args);
    }
}
