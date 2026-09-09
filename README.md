# PoC — Agendamento de Consultas e Exames com Histórico do Paciente

Prova de Conceito da **AEP 2026.2 — 6S**, curso de Engenharia de Software (UniCesumar).

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
paciente esteja disponível e íntegro no momento do atendimento.

## Tecnologias

| Item | Escolha |
|---|---|
| Linguagem | Java 17 (POO, sem framework) |
| Interface | Swing (incluída no JDK, sem dependência externa) |
| Build | Maven |
| Banco de dados | MongoDB (driver oficial `mongodb-driver-sync`) |
| Testes | JUnit 5 |
| Cobertura | JaCoCo, mínimo obrigatório de 70% |

## Persistência

A aplicação grava no **MongoDB**. Cada entidade tem duas implementações de repositório
por trás da mesma interface:

- `repository/mongo` — usada pela aplicação, com o driver oficial;
- `repository/memory` — usada nos testes e como alternativa de execução.

Como os serviços dependem apenas das interfaces `PacienteRepository`,
`ProfissionalRepository` e `AgendamentoRepository`, trocar o mecanismo não altera
nenhuma regra de negócio nem a interface gráfica.

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
| ![Agendamentos](docs/telas/03-agendamentos.png) | Agendamento com bloqueio de conflito, data passada e horário indisponível |
| ![Histórico](docs/telas/04-historico.png) | Histórico clínico consolidado do paciente |

As capturas acima foram feitas em modo memória; com o MongoDB conectado, o cabeçalho
mostra a URI e o nome do banco em uso.

## Evidência de cobertura

O comando abaixo é a evidência reproduzível exigida pela AEP:

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
moram os erros de mapeamento. Os repositórios `repository/mongo` não têm teste
automatizado nesta entrega: são casca fina sobre o driver, e testá-los exige um MongoDB
real ou embarcado.

## Modelagem NoSQL

Três coleções, atendendo aos requisitos de múltiplas coleções, relacionamento entre elas
e pelo menos uma com objetos complexos.

### `pacientes`

Contém objetos complexos: `contato` e `endereco` são documentos aninhados e `historico`
é uma lista de subdocumentos.

```json
{
  "_id": "9f2c...-uuid",
  "nome": "Maria Souza",
  "cpf": "52998224725",
  "dataNascimento": "1990-04-12",
  "contato": { "email": "maria@email.com", "telefone": "44 90000-0000" },
  "endereco": { "rua": "Rua A, 100", "cidade": "Maringá", "estado": "PR" },
  "historico": [
    {
      "data": "2026-03-09",
      "tipo": "CONSULTA",
      "profissionalId": "5a1b...-uuid",
      "descricao": "Consulta de rotina em cardiologia",
      "prescricoes": ["Losartana 50mg - 1x ao dia"]
    }
  ]
}
```

### `profissionais`

```json
{
  "_id": "5a1b...-uuid",
  "nome": "Dr. Carlos Lima",
  "cpf": "11144477735",
  "registroProfissional": "CRM-PR 12345",
  "contato": { "email": "carlos@clinica.com", "telefone": "44 91111-1111" },
  "especialidades": ["Cardiologia"],
  "disponibilidades": [
    { "diaSemana": "MONDAY", "horaInicio": "08:00", "horaFim": "12:00" }
  ]
}
```

### `agendamentos`

Relaciona as duas coleções anteriores por referência de id.

```json
{
  "_id": "c7d4...-uuid",
  "pacienteId": "9f2c...-uuid",
  "profissionalId": "5a1b...-uuid",
  "dataHora": "2026-03-09T09:00",
  "data": "2026-03-09",
  "duracaoMinutos": 30,
  "tipo": "CONSULTA",
  "status": "REALIZADO"
}
```

O campo `data` é derivado de `dataHora` e guarda apenas o dia. Ele existe para que a
consulta de agenda por profissional e data seja uma igualdade indexável, em vez de uma
varredura por intervalo de texto.

### Índices criados

| Coleção | Índice | Motivo |
|---|---|---|
| `pacientes` | `cpf` (único) | Garante no banco a mesma regra de CPF único aplicada no serviço |
| `agendamentos` | `profissionalId + data` | Consulta da agenda do dia, usada na checagem de conflito |
| `agendamentos` | `pacienteId` | Histórico de atendimentos do paciente |

## Estrutura do projeto

```
src/main/java/br/unicesumar/aep/saude/
├── model/        Domínio: Pessoa (abstrata) → Paciente, Profissional;
│                 Agendamento, RegistroClinico, Endereco, Contato,
│                 Disponibilidade, TipoAtendimento, StatusAgendamento
├── repository/   Repository<T> genérica + contratos por entidade
│   ├── memory/   Implementações em memória (testes e modo sem banco)
│   └── mongo/    Implementações MongoDB usadas pela aplicação
├── mapper/       Conversão entre entidades e documentos BSON
├── infra/        MongoConnection e ContextoAplicacao (montagem das dependências)
├── service/      PacienteService, ProfissionalService, AgendamentoService
│                 (regras de negócio)
├── exception/    RegraNegocioException, RecursoNaoEncontradoException
├── ui/           Interface Swing: JanelaPrincipal + uma aba por fluxo
└── app/          Main (ponto de entrada) e DemoConsole (modo texto)
```

## Regras de negócio implementadas

- CPF é normalizado e validado; não se cadastram dois pacientes com o mesmo CPF.
- Não se agenda em data/hora no passado.
- Só se agenda dentro de uma janela de disponibilidade do profissional, e o atendimento
  precisa caber inteiro na janela.
- Dois atendimentos ativos do mesmo profissional não podem se sobrepor. Horários apenas
  encostados (09:00–09:30 e 09:30–10:00) são permitidos.
- Cancelar um atendimento libera o horário.
- Um atendimento só vai de AGENDADO para REALIZADO ou CANCELADO — nunca a partir de um
  status final.
- Concluir um atendimento grava automaticamente o registro no histórico do paciente.

## Aplicação de POO

- **Herança e polimorfismo:** `Pessoa` abstrata com `tipo()` implementado por `Paciente`
  e `Profissional`; `Profissional` sobrescreve `descricaoResumida()`.
- **Abstração:** `Repository<T>` tem duas implementações — memória e MongoDB — e trocar
  uma pela outra não toca em nenhuma linha de serviço.
- **Encapsulamento:** validação nos construtores, listas internas expostas somente leitura.
- **Objetos de valor imutáveis:** `Contato`, `Endereco`, `Disponibilidade`, `RegistroClinico`.
- **Responsabilidade única:** regra de negócio nos serviços, persistência nos repositórios,
  apresentação na `ui`. Nenhuma aba conhece os repositórios: todas falam com os serviços.
- **Reuso na interface:** `PainelFormulario` e `TabelaSimples` concentram o que seria
  repetido em cada aba.

## Entregas

| Entrega | Tag |
|---|---|
| Primeira entrega | `entrega-1` |
| Segunda entrega | `entrega-2` |
