package br.com.reservafacil.hoteis;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Hotéis API", version = "v1",
        description = "Mudanças compatíveis adicionam campos opcionais; alterações incompatíveis exigem nova versão URI."))
public class HoteisApplication {
    public static void main(String[] args) {
        SpringApplication.run(HoteisApplication.class, args);
    }
}
