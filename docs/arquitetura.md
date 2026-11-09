# Arquitetura

A PoC é uma aplicação Java 17 sem framework, organizada em camadas. A interface
gráfica (Swing) fala só com os **serviços**, que concentram as regras de negócio; os
serviços falam só com **interfaces de repositório**, que têm duas implementações
(MongoDB e memória). Trocar o banco não altera nenhuma regra nem nenhuma tela.

## Camadas

```mermaid
flowchart TB
    ui["ui — interface Swing<br/>(uma aba por fluxo)"]
    app["app — Main e DemoConsole"]
    infra["infra — ContextoAplicacao<br/>(monta as dependências)"]
    service["service — regras de negócio<br/>Paciente · Profissional · Agendamento · Relatório"]
    model["model — domínio<br/>entidades, objetos de valor e enums"]
    repo["repository — interfaces"]
    mongo["repository/mongo<br/>driver oficial + aggregation pipeline"]
    memory["repository/memory<br/>testes e modo sem banco"]
    mapper["mapper — entidade ↔ documento BSON"]
    db[("MongoDB<br/>pacientes · profissionais · agendamentos")]

    app --> infra
    app --> ui
    infra --> service
    infra --> mongo
    infra --> memory
    ui --> service
    service --> repo
    service --> model
    repo -. implementada por .-> mongo
    repo -. implementada por .-> memory
    mongo --> mapper
    mongo --> db
    mapper --> model
```

| Pacote | Responsabilidade |
|---|---|
| `model` | Entidades (`Paciente`, `Profissional`, `Agendamento`), objetos de valor imutáveis (`Contato`, `Endereco`, `Disponibilidade`, `RegistroClinico`, `AlteracaoAgendamento`, `FiltroAgenda`, `ItemAgendaDia`, `ResumoAtendimentos`) e enums (`TipoAtendimento`, `StatusAgendamento`). As regras que dependem só do próprio objeto ficam aqui (validação, conflito de horário, transições de status). |
| `service` | Regras que envolvem mais de um objeto ou o relógio: agendar, remarcar, registrar falta, concluir gravando o histórico, validar períodos de relatório. |
| `repository` | Contratos de persistência. `Repository<T>` traz o CRUD; cada entidade acrescenta suas consultas. `RelatorioRepository` define as consultas de leitura que cruzam coleções. |
| `repository/mongo` | Implementação com o driver oficial. `MongoRepository<T>` concentra o CRUD comum; `AgregacoesRelatorio` monta os pipelines de agregação. |
| `repository/memory` | Implementação com mapas em memória, com o mesmo comportamento. Usada pelos testes de unidade e pelo modo "sem banco". |
| `mapper` | Conversão entre entidades e documentos BSON. Mantém o domínio sem dependência do driver. |
| `infra` | `MongoConnection` (URI configurável, ping) e `ContextoAplicacao` (escolhe MongoDB ou memória e monta os serviços). |
| `ui` | Swing: `JanelaPrincipal` e uma aba por fluxo (Pacientes, Profissionais, Agendamentos, Histórico, Relatórios). `PainelFormulario` e `TabelaSimples` evitam repetição entre as abas. |
| `app` | `Main` (abre a interface, com MongoDB ou em memória) e `DemoConsole` (demonstração em modo texto). |

## Diagrama de classes (núcleo)

```mermaid
classDiagram
    direction LR

    class Pessoa {
        <<abstract>>
        -String id
        -String nome
        -String cpf
        -Contato contato
        +tipo()* String
        +descricaoResumida() String
    }
    class Paciente {
        -LocalDate dataNascimento
        -Endereco endereco
        -List~RegistroClinico~ historico
        +adicionarRegistro(RegistroClinico)
        +idade() int
    }
    class Profissional {
        -String registroProfissional
        -List~String~ especialidades
        -List~Disponibilidade~ disponibilidades
        +atendeEm(inicio, fim) boolean
    }
    class Agendamento {
        -String pacienteId
        -String profissionalId
        -LocalDateTime dataHora
        -int duracaoMinutos
        -TipoAtendimento tipo
        -StatusAgendamento status
        -List~AlteracaoAgendamento~ alteracoes
        +conflitaCom(Agendamento) boolean
        +confirmar(momento)
        +remarcarPara(novaDataHora, momento)
        +realizar(momento)
        +registrarFalta(momento)
        +cancelar(momento, motivo)
    }
    class StatusAgendamento {
        <<enumeration>>
        AGENDADO
        CONFIRMADO
        REALIZADO
        NAO_COMPARECEU
        CANCELADO
        +isAtivo() boolean
        +permiteTransicaoPara(StatusAgendamento) boolean
    }
    class Repository~T~ {
        <<interface>>
        +salvar(T) T
        +buscarPorId(String) Optional~T~
        +listarTodos() List~T~
        +remover(String) boolean
        +contar() long
    }
    class AgendamentoRepository {
        <<interface>>
        +buscarPorProfissionalEData(id, data)
        +buscarPorPaciente(id)
        +buscarComFiltro(FiltroAgenda)
    }
    class RelatorioRepository {
        <<interface>>
        +agendaDoDia(data)
        +resumoPorProfissional(inicio, fim)
        +resumoPorTipo(inicio, fim)
    }
    class MongoAgendamentoRepository
    class InMemoryAgendamentoRepository
    class MongoRelatorioRepository
    class InMemoryRelatorioRepository
    class AgendamentoService {
        -Clock clock
        +agendar(...) Agendamento
        +confirmar(id)
        +remarcar(id, novaDataHora)
        +realizar(id, descricao, prescricoes)
        +registrarFalta(id)
        +cancelar(id, motivo)
        +filtrar(FiltroAgenda)
    }
    class RelatorioService

    Pessoa <|-- Paciente
    Pessoa <|-- Profissional
    Paciente "1" *-- "*" RegistroClinico
    Profissional "1" *-- "*" Disponibilidade
    Agendamento "1" *-- "*" AlteracaoAgendamento
    Agendamento --> StatusAgendamento
    Agendamento ..> Paciente : pacienteId
    Agendamento ..> Profissional : profissionalId
    Repository <|-- AgendamentoRepository
    AgendamentoRepository <|.. MongoAgendamentoRepository
    AgendamentoRepository <|.. InMemoryAgendamentoRepository
    RelatorioRepository <|.. MongoRelatorioRepository
    RelatorioRepository <|.. InMemoryRelatorioRepository
    AgendamentoService --> AgendamentoRepository
    RelatorioService --> RelatorioRepository
```

`PacienteRepository` e `ProfissionalRepository` seguem o mesmo desenho de
`AgendamentoRepository` (interface + implementação MongoDB + implementação em memória).

## Ciclo de vida do agendamento

```mermaid
stateDiagram-v2
    [*] --> AGENDADO : agendar
    AGENDADO --> CONFIRMADO : confirmar
    AGENDADO --> AGENDADO : remarcar
    CONFIRMADO --> AGENDADO : remarcar (precisa confirmar de novo)
    AGENDADO --> REALIZADO : concluir
    CONFIRMADO --> REALIZADO : concluir
    AGENDADO --> NAO_COMPARECEU : registrar falta
    CONFIRMADO --> NAO_COMPARECEU : registrar falta
    AGENDADO --> CANCELADO : cancelar (com motivo)
    CONFIRMADO --> CANCELADO : cancelar (com motivo)
    REALIZADO --> [*]
    NAO_COMPARECEU --> [*]
    CANCELADO --> [*]
```

AGENDADO e CONFIRMADO ocupam o horário do profissional; os outros três são finais.
Cada transição é registrada em `alteracoes`, com o momento, o status anterior, o novo
status e uma observação (motivo do cancelamento, datas da remarcação).

## Regras de negócio

| Regra | Onde fica |
|---|---|
| CPF guardado só com dígitos, com 11 dígitos e não todos iguais; CPF único por paciente | `Pessoa`, `PacienteService` e índice único no MongoDB |
| Não agendar nem remarcar para data/hora no passado | `AgendamentoService` (relógio injetável) |
| O atendimento precisa caber inteiro numa janela de disponibilidade do profissional | `Profissional.atendeEm` |
| Dois atendimentos ativos do mesmo profissional não se sobrepõem (horários encostados são permitidos) | `Agendamento.conflitaCom` |
| Transições de status só pelos caminhos do diagrama acima | `StatusAgendamento.permiteTransicaoPara` |
| Remarcação reaplica todas as regras de horário e volta o status para AGENDADO | `AgendamentoService.remarcar` |
| Falta só pode ser registrada depois do horário marcado | `AgendamentoService.registrarFalta` |
| Cancelamento exige motivo | `Agendamento.cancelar` |
| Concluir grava o atendimento no histórico clínico do paciente | `AgendamentoService.realizar` |
| Relatórios aceitam no máximo um ano de período, com fim depois do início | `RelatorioService` |

## Decisões de projeto

- **Repositórios por interface, com duas implementações.** Os serviços não sabem se os
  dados estão no MongoDB ou em memória. Isso permite testar todas as regras sem banco,
  rodar a demonstração sem infraestrutura e, nos testes de integração, comparar o
  resultado das agregações do MongoDB com a versão em memória.
- **Mapeadores separados do domínio.** Nenhuma classe de `model` importa o driver do
  MongoDB; a conversão fica nos `*Mapper`, testados com ida e volta.
- **Relógio injetável (`Clock`).** As regras de "data no passado" e "falta só depois do
  horário" são testadas de forma determinística, com um relógio fixo.
- **Agregações no banco, não na aplicação.** Agenda do dia e resumos usam `$lookup` e
  `$group`, para que o MongoDB faça o cruzamento e a contagem. Os pipelines ficam em
  `AgregacoesRelatorio`, separados do repositório, para serem testados sem banco.
- **Objetos de valor imutáveis** para o que não tem identidade própria (contato,
  endereço, janela de atendimento, registro clínico, alteração, filtro).
- **Sem framework.** Injeção de dependências feita à mão em `ContextoAplicacao`; deixa
  explícito o que cada camada recebe, que é o objetivo de uma PoC acadêmica de POO.

## Aplicação de POO

- **Herança e polimorfismo:** `Pessoa` abstrata, com `tipo()` implementado por `Paciente`
  e `Profissional`; `Profissional` sobrescreve `descricaoResumida()`.
- **Abstração:** `Repository<T>` e `RelatorioRepository`, cada um com duas implementações.
- **Classes base reutilizáveis:** `MongoRepository<T>` e `InMemoryRepository<T>` concentram
  o CRUD; as subclasses só informam o id e as consultas específicas.
- **Encapsulamento:** validação nos construtores; listas internas expostas somente leitura;
  o status do agendamento só muda pelos métodos de transição.
- **Responsabilidade única:** regra no serviço, persistência no repositório, conversão no
  mapper, apresentação na `ui`.
