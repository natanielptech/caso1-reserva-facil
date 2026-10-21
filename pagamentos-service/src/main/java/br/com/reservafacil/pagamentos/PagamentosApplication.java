package br.com.reservafacil.pagamentos;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Pagamentos API", version = "v1",
        description = "Contrato definido em /openapi.yaml. Campos opcionais são compatíveis; mudanças incompatíveis requerem nova versão URI."))
public class PagamentosApplication {
    public static void main(String[] args) {
        SpringApplication.run(PagamentosApplication.class, args);
    }
}
