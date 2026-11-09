# Changelog

Mudanças relevantes da PoC por entrega da AEP 2026.2.

## 2ª entrega — `v2.0-entrega2`

### Adicionado
- **Ciclo de vida do agendamento:** confirmar presença, remarcar (com as mesmas regras do
  agendamento) e registrar falta (só depois do horário). Novos status `CONFIRMADO` e
  `NAO_COMPARECEU`.
- **Histórico de alterações** de cada agendamento como lista de subdocumentos
  (`alteracoes`), com momento, status anterior, novo status e observação. Botão
  "Ver alterações" na interface.
- **Relatórios com agregação no MongoDB:** agenda do dia com `$lookup` (nomes de paciente e
  profissional) e resumo por profissional ou por tipo com `$group` (realizados, faltas,
  cancelados, pendentes e % de faltas). Nova aba **Relatórios**.
- **Filtros de agenda** por período, profissional e status, com contagem de resultados.
- Índice `{ data: 1, status: 1 }` na coleção `agendamentos`.
- **Testes de integração** contra MongoDB real (CRUD, índices, subdocumentos, filtros,
  agregações comparadas com a versão em memória e fluxo completo).
- **CI no GitHub Actions** com MongoDB 7, regra de cobertura, resumo e relatório do JaCoCo
  como artefato; selo de status no README.
- **Documentação técnica** em `docs/` (arquitetura, banco de dados, processo, testes).
- Quadro Kanban no GitHub Projects e fluxo issue → branch → PR.

### Alterado
- Cancelar um atendimento passa a exigir um **motivo**.
- Remarcar um atendimento confirmado volta o status para `AGENDADO`.
- README reorganizado, apontando para a documentação em `docs/`.

### Corrigido
- `mvnw` sem permissão de execução no Linux e no macOS.

### Números
- Testes: 103 → **164** (152 sem MongoDB).
- Cobertura: 84,6% → **96,9%** de linhas e 76,5% → **85,8%** de branches (com MongoDB).

## 1ª entrega — `v1.0-entrega1`

Commit `5764b76` · tag/release [`v1.0-entrega1`](https://github.com/YagoGarcia06/AEP-Esoft6S/releases/tag/v1.0-entrega1)

- Cadastro de pacientes (CPF único, endereço, contato) e de profissionais (especialidades
  e janelas de atendimento).
- Agendamento de consultas e exames com bloqueio de data passada, horário fora da
  disponibilidade e conflito de agenda.
- Conclusão do atendimento gravando o registro no histórico clínico do paciente;
  cancelamento liberando o horário.
- Persistência em MongoDB com três coleções, documentos aninhados, listas de
  subdocumentos e índices; implementação em memória para testes e modo sem banco.
- Interface Swing com abas Pacientes, Profissionais, Agendamentos e Histórico;
  demonstração em modo texto.
- 103 testes de unidade, JaCoCo com regra de 70%.
