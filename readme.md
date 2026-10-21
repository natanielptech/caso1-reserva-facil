# ReservaFácil — API REST, Contratos e Integração entre Serviços

> Este README é a especificação funcional e arquitetural para implementação de uma aplicação baseada no estudo de caso da Aula 1 (Unidade 4). Ele **não contém código** — a implementação deve ser feita por outra IA/desenvolvedor a partir destas instruções.

## Stack obrigatória

- **Linguagem: Java** (17 ou 21 LTS)
- **Framework: Spring Boot 3.x** (pode usar Spring Web, Spring Data JPA, Bean Validation)
- Build: Maven ou Gradle (escolha livre)
- Banco de dados: um banco relacional por serviço (H2 aceitável para desenvolvimento/testes; PostgreSQL recomendado para um cenário mais realista)
- Documentação de API: springdoc-openapi (OpenAPI 3), seguindo a abordagem **contract-first** no serviço de Pagamentos
- Testes de contrato consumidor-provedor: Pact (pact-jvm) — recomendado, pois é o padrão ensinado no caso

## Contexto de negócio

A ReservaFácil intermedeia reservas em cerca de 1.200 hotéis parceiros de médio porte. Hóspedes usam um aplicativo móvel para pesquisar hotéis, consultar disponibilidade, confirmar uma reserva e pagar por ela. Funcionários dos hotéis usam um painel Web para acompanhar as reservas recebidas.

O requisito central é permitir que o aplicativo finalize uma reserva completa (verificar disponibilidade, bloquear o quarto, cobrar o hóspede e confirmar a reserva) em **uma única interação percebida pelo usuário**, com uma experiência rápida mesmo em redes móveis instáveis.

## Arquitetura a construir

Implementar como **múltiplos serviços Spring Boot independentes** (um processo/projeto por serviço), cada um com seu próprio banco de dados:

1. **hoteis-service** — catálogo de hotéis, fotos, comodidades
2. **reservas-service** — disponibilidade de quartos e ciclo de vida da reserva
3. **pagamentos-service** — cobranças (cartão/Pix) e registro de transações
4. **bff-mobile** — Backend for Frontend do aplicativo móvel, que orquestra os três serviços acima

> O API Gateway e o BFF do painel Web das clínicas (mencionados no caso como porta de entrada única e segurança perimetral) são **opcionais/stretch goal** — o núcleo obrigatório é o BFF do aplicativo móvel orquestrando os três serviços de domínio.

## Requisitos funcionais e não funcionais

| Tipo | Requisito | Implicação arquitetural obrigatória |
|---|---|---|
| Funcional | Confirmar reserva com pagamento em uma única ação do hóspede | O BFF deve coordenar as chamadas a Reservas e Pagamentos |
| Desempenho | Tela de confirmação em até 2 segundos (p95, rede 4G) | Minimizar idas e voltas entre app e back-end (daí o BFF) |
| Confiabilidade | Nunca cobrar duas vezes a mesma reserva | Operações de pagamento devem ser idempotentes |
| Evolutividade | Cada serviço publicado de forma independente | Contratos explícitos (OpenAPI) e testes de contrato |
| Segurança | Apenas o titular pode consultar/alterar a própria reserva | Autenticação (pode ser simplificada, ex. header com id do usuário simulando um token) e autorização por objeto |

## Endpoints obrigatórios

### reservas-service

| Verbo | Rota | Comportamento |
|---|---|---|
| GET | `/v1/quartos/disponibilidade?hotel=&entrada=&saida=` | Consulta quartos livres no período; `200 OK` com a lista |
| POST | `/v1/reservas` | Cria uma **pré-reserva** com o quarto bloqueado por **15 minutos**; `201 Created` + header `Location` |
| GET | `/v1/reservas/{id}` | Consulta uma reserva; `200 OK`, `403` se não pertencer ao usuário, `404` se não existir |
| POST | `/v1/reservas/{id}/confirmacao` | Confirma a pré-reserva após o pagamento; `200 OK` ou `409 Conflict` se a pré-reserva expirou |
| DELETE | `/v1/reservas/{id}` | Cancela a reserva; `204 No Content` |

### pagamentos-service

| Verbo | Rota | Comportamento |
|---|---|---|
| POST | `/v1/pagamentos` | Cria uma cobrança; `201 Created`, `402 Payment Required` se recusada pela operadora |
| GET | `/v1/pagamentos/{id}` | Consulta o status de uma cobrança; `200 OK` |

**Idempotência obrigatória:** toda requisição `POST /v1/pagamentos` deve exigir o header `Idempotency-Key` (UUID gerado pelo cliente). O serviço armazena a chave junto com o resultado da primeira execução; uma segunda requisição com a mesma chave deve devolver o **mesmo resultado já registrado**, sem processar uma nova cobrança.

### bff-mobile

| Verbo | Rota | Comportamento |
|---|---|---|
| POST | `/app/v1/confirmacoes-reserva` | Orquestra: consulta o hotel → cria a pré-reserva → solicita a cobrança (com `Idempotency-Key`) → se aprovada, confirma a reserva; se recusada, **cancela a pré-reserva** (ação compensatória) e devolve erro claro de pagamento recusado |

**Timeouts internos obrigatórios no BFF:** cada chamada interna deve ter um timeout curto (sugestão: 300–500 ms) para que a lentidão de um serviço não trave a experiência do usuário. Na ausência da foto do hotel (timeout no `hoteis-service`), a resposta deve seguir com uma imagem/dado padrão em vez de falhar.

## Modelo de dados sugerido

**Reserva** (reservas-service): `id`, `hotelId`, `quartoId`, `hospedeId`, `status` (`PRE_RESERVADA`, `CONFIRMADA`, `CANCELADA`, `EXPIRADA`), `criadaEm`, `expiraEm`

**Pagamento** (pagamentos-service): `id`, `reservaId`, `valor`, `moeda`, `metodo` (`CARTAO`, `PIX`), `status` (`APROVADO`, `RECUSADO`), `idempotencyKey` (única)

## Padrões obrigatórios a demonstrar

1. **Versionamento de API por URI** (`/v1/...`), com uma política clara de mudanças compatíveis (ex. novo campo opcional) vs. incompatíveis (ex. remover/renomear campo) documentada no OpenAPI.
2. **Especificação OpenAPI** publicada por cada serviço, especialmente o `pagamentos-service` (contract-first).
3. **Padrão Backend for Frontend (BFF)** no `bff-mobile`, com ação compensatória em caso de falha parcial.
4. **Idempotência** na criação de pagamentos via header `Idempotency-Key`.
5. **Testes de contrato consumidor-provedor** (Pact) entre o `bff-mobile` (consumidor) e o `pagamentos-service` (provedor), verificando que o contrato publicado continua compatível a cada mudança.

## Critérios de aceite

- [ ] Os 4 serviços sobem independentemente e se comunicam via HTTP
- [ ] Fluxo completo de reserva (disponibilidade → pré-reserva → pagamento → confirmação) funciona de ponta a ponta via o endpoint do BFF
- [ ] Reenviar a mesma requisição de pagamento com a mesma `Idempotency-Key` não gera cobrança duplicada
- [ ] Se o pagamento for recusado, a pré-reserva é cancelada automaticamente (ação compensatória)
- [ ] Cada serviço expõe sua documentação OpenAPI (ex. `/v3/api-docs`, `/swagger-ui.html`)
- [ ] Teste de contrato Pact entre BFF e serviço de Pagamentos presente e passando

## Executar a implementação

Requisitos locais: Java 17 e Maven 3.8+. Na raiz do repositório, execute `mvn clean verify` para compilar os quatro módulos e rodar os testes, inclusive a verificação Pact do provedor.

Inicie cada comando em um terminal separado:

```bash
mvn -pl hoteis-service spring-boot:run
mvn -pl reservas-service spring-boot:run
mvn -pl pagamentos-service spring-boot:run
mvn -pl bff-mobile spring-boot:run
```

Os serviços usam as portas `8081` (hotéis), `8082` (reservas), `8083` (pagamentos) e `8080` (BFF). Cada um possui seu próprio banco H2 em memória. As URLs internas do BFF podem ser alteradas com `HOTEIS_SERVICE_URL`, `RESERVAS_SERVICE_URL` e `PAGAMENTOS_SERVICE_URL`; `INTERNAL_TIMEOUT` configura o timeout por chamada (padrão `400ms`).

Cada serviço publica OpenAPI em `/v3/api-docs` e Swagger UI em `/swagger-ui.html`. O contrato OpenAPI de pagamentos, mantido como especificação antes da implementação, também está em `/openapi.yaml`.

Exemplo de confirmação ponta a ponta:

```bash
curl -i http://localhost:8080/app/v1/confirmacoes-reserva \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: hospede-123' \
  -H 'Idempotency-Key: 123e4567-e89b-12d3-a456-426614174000' \
  -d '{"hotelId":1,"quartoId":101,"entrada":"2027-01-10","saida":"2027-01-12","metodo":"CARTAO"}'
```

`X-User-Id` simula a identidade autenticada do hóspede neste estudo. Reutilize a mesma `Idempotency-Key` ao repetir uma solicitação. Para demonstrar a compensação de pagamento recusado, inclua `"simularRecusa":true`; a pré-reserva será cancelada.
