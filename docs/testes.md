# Testes

A PoC tem duas camadas de testes automatizados, ambas com **JUnit 5**, e a cobertura é
medida pelo **JaCoCo** com regra mínima de **70% de linhas e de branches**: abaixo disso,
`mvn verify` falha.

| Camada | Onde | Precisa de banco? | O que cobre |
|---|---|---|---|
| Unidade | `model`, `service`, `mapper`, `repository`, `repository/mongo`, `infra` | Não | Regras do domínio e dos serviços (com relógio fixo), repositórios em memória, ida e volta dos mapeadores, pipelines de agregação e consultas geradas para o MongoDB. |
| Integração | `integracao` | Sim (MongoDB real) | CRUD e índices nas três coleções, subdocumentos, filtros executados no banco, agregações comparadas com a versão em memória e o fluxo completo da PoC. |

## Como rodar

```bash
# todos os testes + regra de cobertura (sem MongoDB, a integração é ignorada)
./mvnw clean verify

# com MongoDB local, para rodar também a integração
docker run -d --name mongo-poc -p 27017:27017 mongo:7
./mvnw clean verify

# só os testes de integração
./mvnw clean test -Dgroups=integracao
```

No Windows, use `.\mvnw.cmd` no lugar de `./mvnw`. No IntelliJ, as configurações
`Testes (mvn test)` e `Cobertura 70% (mvn verify)` já vêm prontas na pasta `.run/`.

O relatório de cobertura fica em `target/site/jacoco/index.html`. No GitHub, cada execução
do CI mostra o total de testes e a cobertura na aba *Summary* e disponibiliza o relatório
completo em *Artifacts*.

## Testes de integração

- Cada classe cria um banco descartável (`poc_saude_teste_<aleatório>`), zerado antes de
  cada teste e apagado no final; o banco da aplicação nunca é usado.
- Endereço padrão `mongodb://localhost:27017`; outro servidor por `MONGODB_URI_TESTES` ou
  `-Dmongodb.uri.testes=...`.
- Se o MongoDB não responder em 2 segundos, as classes de integração são ignoradas e o
  `verify` continua passando. No CI isso é tratado como erro, para garantir que rodaram.

## Exclusões da medição

Os pacotes `ui` (Swing) e `app` (pontos de entrada) não entram na medição de cobertura:
são camadas de apresentação, sem regra de negócio. As exclusões estão declaradas no
`pom.xml`. A interface foi conferida manualmente a cada funcionalidade nova.

## Números da 2ª entrega

| Ambiente | Testes | Linhas | Branches |
|---|---|---|---|
| Com MongoDB (CI) | 164 | 96,9% | 85,8% |
| Sem MongoDB | 152 | 88,9% | 80,9% |

Na 1ª entrega eram 103 testes, todos de unidade, com 84,6% de linhas e 76,5% de branches.
