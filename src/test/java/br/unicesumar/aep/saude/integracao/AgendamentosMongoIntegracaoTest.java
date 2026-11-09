package br.unicesumar.aep.saude.integracao;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.FiltroAgenda;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.repository.mongo.MongoAgendamentoRepository;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Coleção "agendamentos" no MongoDB real: consultas, índices e subdocumentos. */
class AgendamentosMongoIntegracaoTest extends MongoIntegracaoBase {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 3, 9);
    private static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 3, 2, 10, 0);

    private MongoAgendamentoRepository agendamentos;

    @Override
    protected void prepararRepositorios(MongoDatabase database) {
        agendamentos = new MongoAgendamentoRepository(database);
    }

    private Agendamento salvar(String paciente, String profissional, LocalDateTime dataHora,
                               StatusAgendamento status) {
        return agendamentos.salvar(new Agendamento(null, paciente, profissional, dataHora, 30,
                TipoAtendimento.CONSULTA, status));
    }

    @Test
    @DisplayName("cria os índices usados pelas consultas")
    void deveCriarIndices() {
        List<Document> indices = conexao().getDatabase().getCollection("agendamentos")
                .listIndexes().into(new ArrayList<>());
        List<Document> chaves = indices.stream().map(i -> i.get("key", Document.class)).toList();

        assertTrue(chaves.contains(new Document("profissionalId", 1).append("data", 1)));
        assertTrue(chaves.contains(new Document("pacienteId", 1)));
        assertTrue(chaves.contains(new Document("data", 1).append("status", 1)));
    }

    @Test
    @DisplayName("busca a agenda por profissional e dia e por paciente, em ordem de horário")
    void deveBuscarAgendas() {
        salvar("pac-1", "prof-1", SEGUNDA.atTime(10, 0), StatusAgendamento.AGENDADO);
        salvar("pac-2", "prof-1", SEGUNDA.atTime(8, 0), StatusAgendamento.AGENDADO);
        salvar("pac-1", "prof-2", SEGUNDA.atTime(9, 0), StatusAgendamento.AGENDADO);
        salvar("pac-1", "prof-1", SEGUNDA.plusDays(7).atTime(9, 0), StatusAgendamento.AGENDADO);

        List<Agendamento> doProf1 = agendamentos.buscarPorProfissionalEData("prof-1", SEGUNDA);
        assertEquals(2, doProf1.size());
        assertEquals(SEGUNDA.atTime(8, 0), doProf1.get(0).getDataHora());

        List<Agendamento> daPac1 = agendamentos.buscarPorPaciente("pac-1");
        assertEquals(3, daPac1.size());
        assertEquals(SEGUNDA.atTime(9, 0), daPac1.get(0).getDataHora());
        assertTrue(agendamentos.buscarPorPaciente(null).isEmpty());
        assertTrue(agendamentos.buscarPorProfissionalEData("prof-1", null).isEmpty());
    }

    @Test
    @DisplayName("aplica o filtro da agenda no banco")
    void deveFiltrar() {
        salvar("pac-1", "prof-1", SEGUNDA.atTime(8, 0), StatusAgendamento.CONFIRMADO);
        salvar("pac-1", "prof-1", SEGUNDA.plusDays(6).atTime(8, 0), StatusAgendamento.AGENDADO);
        salvar("pac-1", "prof-2", SEGUNDA.plusDays(3).atTime(8, 0), StatusAgendamento.CONFIRMADO);
        salvar("pac-1", "prof-1", SEGUNDA.plusDays(7).atTime(8, 0), StatusAgendamento.CONFIRMADO);

        assertEquals(4, agendamentos.buscarComFiltro(FiltroAgenda.TODOS).size());
        assertEquals(3, agendamentos.buscarComFiltro(
                new FiltroAgenda(SEGUNDA, SEGUNDA.plusDays(6), null, null)).size());
        assertEquals(2, agendamentos.buscarComFiltro(
                new FiltroAgenda(SEGUNDA, SEGUNDA.plusDays(6), null, StatusAgendamento.CONFIRMADO)).size());

        List<Agendamento> filtrados = agendamentos.buscarComFiltro(
                new FiltroAgenda(SEGUNDA, null, "prof-1", StatusAgendamento.CONFIRMADO));
        assertEquals(2, filtrados.size());
        assertEquals(SEGUNDA.atTime(8, 0), filtrados.get(0).getDataHora());
    }

    @Test
    @DisplayName("persiste remarcação e histórico de alterações e mantém o campo 'data' coerente")
    void devePersistirAlteracoes() {
        Agendamento consulta = salvar("pac-1", "prof-1", SEGUNDA.atTime(9, 0), StatusAgendamento.AGENDADO);
        consulta.confirmar(MOMENTO);
        consulta.remarcarPara(SEGUNDA.plusDays(7).atTime(10, 0), MOMENTO.plusHours(1));
        consulta.cancelar(MOMENTO.plusHours(2), "Paciente viajou");
        agendamentos.salvar(consulta);

        Agendamento lido = agendamentos.buscarPorId(consulta.getId()).orElseThrow();
        assertEquals(StatusAgendamento.CANCELADO, lido.getStatus());
        assertEquals(3, lido.getAlteracoes().size());
        assertEquals("Motivo: Paciente viajou", lido.getAlteracoes().get(2).getObservacao());

        assertTrue(agendamentos.buscarPorProfissionalEData("prof-1", SEGUNDA).isEmpty());
        assertEquals(1, agendamentos.buscarPorProfissionalEData("prof-1", SEGUNDA.plusDays(7)).size());
    }
}
