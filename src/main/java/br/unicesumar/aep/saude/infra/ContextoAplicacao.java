package br.unicesumar.aep.saude.infra;

import br.unicesumar.aep.saude.repository.AgendamentoRepository;
import br.unicesumar.aep.saude.repository.PacienteRepository;
import br.unicesumar.aep.saude.repository.ProfissionalRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryAgendamentoRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryPacienteRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryProfissionalRepository;
import br.unicesumar.aep.saude.repository.mongo.MongoAgendamentoRepository;
import br.unicesumar.aep.saude.repository.mongo.MongoPacienteRepository;
import br.unicesumar.aep.saude.repository.mongo.MongoProfissionalRepository;
import br.unicesumar.aep.saude.service.AgendamentoService;
import br.unicesumar.aep.saude.service.PacienteService;
import br.unicesumar.aep.saude.service.ProfissionalService;

import java.util.Objects;

/**
 * Monta as dependências da aplicação em um único lugar.
 *
 * <p>É aqui que se escolhe o mecanismo de persistência. Como os serviços
 * dependem apenas das interfaces de repositório, trocar MongoDB por memória
 * não exige nenhuma mudança nas regras de negócio nem na interface gráfica.</p>
 */
public class ContextoAplicacao implements AutoCloseable {

    private final PacienteService pacienteService;
    private final ProfissionalService profissionalService;
    private final AgendamentoService agendamentoService;
    private final String descricaoArmazenamento;
    private final MongoConnection conexao;

    private ContextoAplicacao(PacienteRepository pacientes,
                              ProfissionalRepository profissionais,
                              AgendamentoRepository agendamentos,
                              String descricaoArmazenamento,
                              MongoConnection conexao) {
        this.pacienteService = new PacienteService(pacientes);
        this.profissionalService = new ProfissionalService(profissionais);
        this.agendamentoService = new AgendamentoService(agendamentos, pacientes, profissionais);
        this.descricaoArmazenamento = descricaoArmazenamento;
        this.conexao = conexao;
    }

    public static ContextoAplicacao comMongo(MongoConnection conexao) {
        Objects.requireNonNull(conexao, "Conexão com o MongoDB é obrigatória.");
        return new ContextoAplicacao(
                new MongoPacienteRepository(conexao.getDatabase()),
                new MongoProfissionalRepository(conexao.getDatabase()),
                new MongoAgendamentoRepository(conexao.getDatabase()),
                conexao.getDescricao(),
                conexao);
    }

    public static ContextoAplicacao emMemoria() {
        return new ContextoAplicacao(
                new InMemoryPacienteRepository(),
                new InMemoryProfissionalRepository(),
                new InMemoryAgendamentoRepository(),
                "Memória (dados não são persistidos)",
                null);
    }

    public PacienteService getPacienteService() {
        return pacienteService;
    }

    public ProfissionalService getProfissionalService() {
        return profissionalService;
    }

    public AgendamentoService getAgendamentoService() {
        return agendamentoService;
    }

    public String getDescricaoArmazenamento() {
        return descricaoArmazenamento;
    }

    @Override
    public void close() {
        if (conexao != null) {
            conexao.close();
        }
    }
}
