package br.unicesumar.aep.saude.integracao;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.ItemAgendaDia;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.ResumoAtendimentos;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.repository.RelatorioRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryAgendamentoRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryPacienteRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryProfissionalRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryRelatorioRepository;
import br.unicesumar.aep.saude.repository.mongo.MongoAgendamentoRepository;
import br.unicesumar.aep.saude.repository.mongo.MongoPacienteRepository;
import br.unicesumar.aep.saude.repository.mongo.MongoProfissionalRepository;
import br.unicesumar.aep.saude.repository.mongo.MongoRelatorioRepository;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Executa os pipelines de agregação no MongoDB real e compara o resultado com
 * a implementação em memória, alimentadas com os mesmos dados.
 */
class RelatoriosMongoIntegracaoTest extends MongoIntegracaoBase {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 3, 9);

    private MongoPacienteRepository pacientesMongo;
    private MongoProfissionalRepository profissionaisMongo;
    private MongoAgendamentoRepository agendamentosMongo;
    private RelatorioRepository relatorioMongo;

    private InMemoryPacienteRepository pacientesMemoria;
    private InMemoryProfissionalRepository profissionaisMemoria;
    private InMemoryAgendamentoRepository agendamentosMemoria;
    private RelatorioRepository relatorioMemoria;

    @Override
    protected void prepararRepositorios(MongoDatabase database) {
        pacientesMongo = new MongoPacienteRepository(database);
        profissionaisMongo = new MongoProfissionalRepository(database);
        agendamentosMongo = new MongoAgendamentoRepository(database);
        relatorioMongo = new MongoRelatorioRepository(database);

        pacientesMemoria = new InMemoryPacienteRepository();
        profissionaisMemoria = new InMemoryProfissionalRepository();
        agendamentosMemoria = new InMemoryAgendamentoRepository();
        relatorioMemoria = new InMemoryRelatorioRepository(
                agendamentosMemoria, pacientesMemoria, profissionaisMemoria);
    }

    private void emAmbos(Paciente paciente) {
        pacientesMongo.salvar(paciente);
        pacientesMemoria.salvar(paciente);
    }

    private void emAmbos(Profissional profissional) {
        profissionaisMongo.salvar(profissional);
        profissionaisMemoria.salvar(profissional);
    }

    private void agendarEmAmbos(Paciente paciente, Profissional profissional, LocalDateTime dataHora,
                                TipoAtendimento tipo, StatusAgendamento status) {
        Agendamento agendamento = new Agendamento(null, paciente.getId(), profissional.getId(),
                dataHora, tipo.getDuracaoPadraoMinutos(), tipo, status);
        agendamentosMongo.salvar(agendamento);
        agendamentosMemoria.salvar(agendamento);
    }

    private void carregarCenario() {
        Paciente maria = Fixtures.maria();
        Paciente joao = Fixtures.joao();
        Profissional carlos = Fixtures.carlos();
        Profissional ana = Fixtures.ana();
        emAmbos(maria);
        emAmbos(joao);
        emAmbos(carlos);
        emAmbos(ana);

        agendarEmAmbos(maria, carlos, SEGUNDA.atTime(8, 0), TipoAtendimento.CONSULTA, StatusAgendamento.REALIZADO);
        agendarEmAmbos(joao, carlos, SEGUNDA.atTime(9, 0), TipoAtendimento.CONSULTA, StatusAgendamento.NAO_COMPARECEU);
        agendarEmAmbos(maria, ana, SEGUNDA.atTime(10, 0), TipoAtendimento.EXAME, StatusAgendamento.CONFIRMADO);
        agendarEmAmbos(joao, carlos, SEGUNDA.atTime(11, 0), TipoAtendimento.EXAME, StatusAgendamento.CANCELADO);
        agendarEmAmbos(joao, ana, SEGUNDA.plusDays(7).atTime(8, 0), TipoAtendimento.CONSULTA, StatusAgendamento.AGENDADO);
        agendarEmAmbos(maria, carlos, SEGUNDA.plusDays(30).atTime(8, 0), TipoAtendimento.CONSULTA, StatusAgendamento.AGENDADO);
    }

    private static String descrever(List<?> itens) {
        return itens.stream().map(Object::toString).reduce("", (a, b) -> a + b + "\n");
    }

    @Test
    @DisplayName("agenda do dia com $lookup igual à versão em memória")
    void agendaDoDiaIgualAMemoria() {
        carregarCenario();

        List<ItemAgendaDia> doMongo = relatorioMongo.agendaDoDia(SEGUNDA);

        assertEquals(4, doMongo.size());
        assertEquals("Maria Souza", doMongo.get(0).getPacienteNome());
        assertEquals("Dr. Carlos Lima", doMongo.get(0).getProfissionalNome());
        assertEquals(descrever(relatorioMemoria.agendaDoDia(SEGUNDA)), descrever(doMongo));
    }

    @Test
    @DisplayName("resumos com $group iguais à versão em memória")
    void resumosIguaisAMemoria() {
        carregarCenario();
        LocalDate fim = SEGUNDA.plusDays(13);

        List<ResumoAtendimentos> porProfissional = relatorioMongo.resumoPorProfissional(SEGUNDA, fim);
        List<ResumoAtendimentos> porTipo = relatorioMongo.resumoPorTipo(SEGUNDA, fim);

        assertEquals("Dr. Carlos Lima", porProfissional.get(0).getGrupo());
        assertEquals(3, porProfissional.get(0).getTotal());
        assertEquals(1, porProfissional.get(0).getFaltas());
        assertEquals(descrever(relatorioMemoria.resumoPorProfissional(SEGUNDA, fim)), descrever(porProfissional));
        assertEquals(descrever(relatorioMemoria.resumoPorTipo(SEGUNDA, fim)), descrever(porTipo));
    }

    @Test
    @DisplayName("mostra '(removido)' quando o profissional foi apagado")
    void profissionalRemovido() {
        carregarCenario();
        Profissional carlos = profissionaisMongo.buscarPorEspecialidade("Cardiologia").get(0);
        profissionaisMongo.remover(carlos.getId());

        assertEquals(ItemAgendaDia.REMOVIDO, relatorioMongo.agendaDoDia(SEGUNDA).get(0).getProfissionalNome());
        assertEquals(ItemAgendaDia.REMOVIDO,
                relatorioMongo.resumoPorProfissional(SEGUNDA, SEGUNDA).get(0).getGrupo());
    }
}
