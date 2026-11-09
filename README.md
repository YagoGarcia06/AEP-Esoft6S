# PoC — Agendamento de Consultas e Exames com Histórico do Paciente

[![CI](https://github.com/YagoGarcia06/AEP-Esoft6S/actions/workflows/ci.yml/badge.svg)](https://github.com/YagoGarcia06/AEP-Esoft6S/actions/workflows/ci.yml)

Prova de Conceito da **AEP 2026.2 — 6S**, curso de Engenharia de Software (UniCesumar).

## Documentação

| Documento | Conteúdo |
|---|---|
| [docs/arquitetura.md](docs/arquitetura.md) | Camadas, diagrama de classes, ciclo de vida do agendamento, regras de negócio e decisões de projeto |
| [docs/banco-de-dados.md](docs/banco-de-dados.md) | Coleções, documentos de exemplo, relacionamentos, índices e pipelines de agregação |
| [docs/testes.md](docs/testes.md) | Estratégia de testes, como rodar, testes de integração e cobertura |
| [docs/processo.md](docs/processo.md) | Metodologia (Kanban), quadro de tarefas, fluxo de branches e PRs, CI |
| [CHANGELOG.md](CHANGELOG.md) | O que mudou da 1ª para a 2ª entrega |

## Problema

Clínicas e unidades de saúde de pequeno porte ainda organizam a agenda em papel ou
planilha. Isso produz três efeitos recorrentes:

- sobreposição de horários do mesmo profissional;
- atendimentos concluídos sem registro estruturado;
- ausência de histórico consolidado — o profissional atende sem enxergar o que já foi
  feito com aquele paciente.

**Público-alvo:** clínicas e consultórios com poucos profissionais, sem sistema de gestão
próprio, onde a agenda é controlada manualmente pela recepção.

## ODS atendido

**ODS 3 — Saúde e Bem-Estar.** A PoC contribui para a meta de acesso a serviços de saúde
de qualidade ao reduzir falhas de agendamento e garantir que o histórico clínico do
paciente esteja disponível e íntegro no momento do atendimento. Na 2ª entrega, a
confirmação de presença, o registro de faltas e o relatório de **% de faltas** por
profissional ajudam a clínica a enxergar e reduzir o absenteísmo, que desperdiça horários
de atendimento.

## Tecnologias

| Item | Escolha |
|---|---|
| Linguagem | Java 17 (POO, sem framework) |
| Interface | Swing (incluída no JDK, sem dependência externa) |
| Build | Maven |
| Banco de dados | MongoDB (driver oficial `mongodb-driver-sync`) |
| Testes | JUnit 5 (unidade + integração com MongoDB real) |
| Cobertura | JaCoCo, mínimo obrigatório de 70% |
| CI | GitHub Actions, com MongoDB 7 como serviço |
| Qualidade | SonarCloud (análise estática e Quality Gate em cada PR) |
| Processo | Kanban no GitHub Projects, issues e pull requests |

## Persistência

A aplicação grava no **MongoDB**. Cada entidade tem duas implementações de repositório
por trás da mesma interface:

- `repository/mongo` — usada pela aplicação, com o driver oficial;
- `repository/memory` — usada nos testes e como alternativa de execução.

Como os serviços dependem apenas das interfaces `PacienteRepository`,
`ProfissionalRepository`, `AgendamentoRepository` e `RelatorioRepository`, trocar o
mecanismo não altera nenhuma regra de negócio nem a interface gráfica. Detalhes em
[docs/arquitetura.md](docs/arquitetura.md).

### Configuração da conexão

Sem nenhuma configuração, a aplicação usa `mongodb://localhost:27017` e o banco
`poc_saude`. Para apontar para outro servidor (MongoDB Atlas, por exemplo):

```bash
export MONGODB_URI="mongodb+srv://usuario:senha@cluster0.abcde.mongodb.net/"
export MONGODB_DATABASE="poc_saude"
```

Também funciona por propriedade de sistema: `-Dmongodb.uri=... -Dmongodb.database=...`.

Se o banco não estiver acessível na hora de abrir, a aplicação avisa e oferece continuar
em memória, para que uma demonstração não fique travada por causa do ambiente.

## Abrindo no IntelliJ IDEA

1. Descompacte o arquivo e abra o IntelliJ em **File → Open**, apontando para a pasta que
   contém o `pom.xml` **diretamente** — não para uma pasta acima dela.
2. Quando aparecer a notificação *Maven build script found*, clique em **Load Maven
   Project**. Se ela não aparecer, clique com o botão direito no `pom.xml` e escolha
   **Add as Maven Project**.
3. Em **File → Project Structure → Project**, selecione um **JDK 17 ou superior**.
4. Aguarde o Maven baixar as dependências (aba Maven, botão de reload).

O projeto já vem com configurações de execução prontas na pasta `.run/`. Depois do passo 4
elas aparecem na lista do canto superior direito:

| Configuração | O que faz |
|---|---|
| `PoC - Interface (MongoDB)` | Abre a interface conectando ao MongoDB |
| `PoC - Interface (sem banco)` | Abre a interface guardando tudo em memória |
| `PoC - Demonstracao no console` | Roda a demonstração em modo texto |
| `Testes (mvn test)` | Executa toda a suíte de testes |
| `Cobertura 70% (mvn verify)` | Roda os testes e valida a cobertura mínima |

## Como executar

Pré-requisitos: JDK 17+ e um MongoDB acessível.

O projeto inclui o **Maven Wrapper**, então não é preciso instalar o Maven. Use `./mvnw`
no Linux e no macOS, ou `.\mvnw.cmd` no Windows (PowerShell). Se você já tiver o Maven
instalado, `mvn` funciona igual — basta trocar o comando.

```bash
# subir um MongoDB local via Docker, se você ainda não tiver um
docker run -d --name mongo-poc -p 27017:27017 mongo:7
```

```bash
# compilar e rodar os testes
./mvnw clean test

# rodar os testes e validar a cobertura mínima de 70% (o build falha abaixo disso)
./mvnw clean verify

# abrir a interface gráfica (Swing), conectando ao MongoDB
./mvnw compile exec:java

# abrir a interface sem banco, guardando tudo em memória
./mvnw compile exec:java -Dexec.args="--memoria"

# gerar o .jar executável e abrir com dois cliques ou pelo terminal
./mvnw clean package
java -jar target/poc-saude-agendamento-1.0.0.jar

# executar a demonstração em modo texto (útil em servidor sem ambiente gráfico)
java -jar target/poc-saude-agendamento-1.0.0.jar --console
```

O botão **Carregar dados de exemplo**, no topo da janela, popula dois pacientes, dois
profissionais com suas janelas de atendimento e três agendamentos — o suficiente para
demonstrar todos os fluxos sem digitar nada.

## Telas

| Aba | O que demonstra |
|---|---|
| ![Pacientes](docs/telas/01-pacientes.png) | Cadastro de pacientes com validação de CPF, e-mail e endereço |
| ![Profissionais](docs/telas/02-profissionais.png) | Cadastro de profissionais, especialidades e janelas de atendimento |
| ![Agendamentos](docs/telas/03-agendamentos.png) | Agendamento com bloqueio de conflito, data passada e horário indisponível; confirmar, remarcar, concluir, registrar falta, cancelar com motivo; filtros por período, profissional e status |
| ![Histórico](docs/telas/04-historico.png) | Histórico clínico consolidado do paciente |
| ![Relatórios](docs/telas/05-relatorios.png) | Agenda do dia (`$lookup`) e resumo de atendimentos por profissional ou tipo (`$group`), com % de faltas |

As capturas acima foram feitas em modo memória; com o MongoDB conectado, o cabeçalho
mostra a URI e o nome do banco em uso.

## Evidência de cobertura

A cada push e pull request, o **GitHub Actions** sobe um MongoDB 7, roda todos os testes
(unidade e integração) e aplica a regra de 70%. O selo no topo deste README mostra o
resultado da última execução; em cada execução, a aba *Summary* traz o total de testes e a
cobertura, e o relatório completo do JaCoCo fica disponível para download em *Artifacts*.
O workflow está em [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

Localmente, o comando abaixo é a evidência reproduzível exigida pela AEP:

```bash
./mvnw clean verify
```

No Windows, `.\mvnw.cmd clean verify`. Ele executa os testes, gera o relatório e aplica a regra de 70% de linhas e de branches.
O relatório em HTML fica em:

```
target/site/jacoco/index.html
```

Os pacotes `app` e `ui` são excluídos da medição por serem apenas camadas de
apresentação, sem regra de negócio. As exclusões estão declaradas no `pom.xml`.

Os mapeadores `Document ↔ objeto` são cobertos por testes de ida e volta, que é onde
moram os erros de mapeamento.

### Testes de integração com MongoDB

Os repositórios `repository/mongo`, os pipelines de agregação dos relatórios e o fluxo
completo da PoC são testados contra um **MongoDB real**, no pacote `integracao`. Cada
classe usa um banco descartável com nome aleatório e apaga tudo ao final, sem tocar no
banco da aplicação. Os relatórios são comparados com a implementação em memória,
alimentada com os mesmos dados.

```bash
# subir um MongoDB local
docker run -d --name mongo-poc -p 27017:27017 mongo:7

# rodar todos os testes, inclusive os de integração
./mvnw clean verify
```

Por padrão os testes procuram o MongoDB em `localhost:27017`. Para usar outro servidor,
defina `MONGODB_URI_TESTES` (variável de ambiente) ou `-Dmongodb.uri.testes=...`.

**Sem MongoDB acessível, os testes de integração são ignorados** e o `verify` continua
passando com os testes de unidade (cobertura acima de 70% nos dois casos). Para rodar
só os de integração: `./mvnw test -Dgroups=integracao`.

## Modelagem NoSQL

Três coleções — `pacientes`, `profissionais` e `agendamentos` — relacionadas por
referência de id (`pacienteId`, `profissionalId`), com objetos complexos nas três:
documentos aninhados (`contato`, `endereco`) e listas de subdocumentos (`historico`,
`disponibilidades`, `alteracoes`). Os relatórios usam o aggregation pipeline (`$lookup`,
`$group`).

Documentos de exemplo, índices, a justificativa de embutir ou referenciar cada dado e os
pipelines completos estão em [docs/banco-de-dados.md](docs/banco-de-dados.md).

## Estrutura do projeto

```
src/main/java/br/unicesumar/aep/saude/
├── model/        Domínio: Pessoa (abstrata) → Paciente, Profissional; Agendamento,
│                 AlteracaoAgendamento, RegistroClinico, FiltroAgenda, objetos de valor e enums
├── repository/   Repository<T>, contratos por entidade e RelatorioRepository
│   ├── memory/   Implementações em memória (testes e modo sem banco)
│   └── mongo/    Implementações MongoDB e pipelines de agregação
├── mapper/       Conversão entre entidades e documentos BSON
├── infra/        MongoConnection e ContextoAplicacao (montagem das dependências)
├── service/      Paciente, Profissional, Agendamento e Relatorio (regras de negócio)
├── exception/    RegraNegocioException, RecursoNaoEncontradoException
├── ui/           Interface Swing: JanelaPrincipal + uma aba por fluxo
└── app/          Main (ponto de entrada) e DemoConsole (modo texto)

src/test/java/br/unicesumar/aep/saude/
├── ...           Testes de unidade, espelhando os pacotes acima
└── integracao/   Testes contra MongoDB real

docs/             Documentação técnica e capturas de tela
.github/workflows CI (GitHub Actions)
```

As regras de negócio, o ciclo de vida do agendamento e a aplicação de POO estão em
[docs/arquitetura.md](docs/arquitetura.md).

## Entregas

| Entrega | Versão | Commit |
|---|---|---|
| 1ª entrega | Release [`v1.0-entrega1`](https://github.com/YagoGarcia06/AEP-Esoft6S/releases/tag/v1.0-entrega1) | `5764b76` |
| 2ª entrega | Release `v2.0-entrega2` | indicado na release |

O que mudou entre as duas está no [CHANGELOG](CHANGELOG.md).
