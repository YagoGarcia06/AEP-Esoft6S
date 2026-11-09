package br.unicesumar.aep.saude.integracao;

import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.repository.mongo.MongoAgendamentoRepository;
import br.unicesumar.aep.saude.repository.mongo.MongoPacienteRepository;
import br.unicesumar.aep.saude.repository.mongo.MongoProfissionalRepository;
import br.unicesumar.aep.saude.service.AgendamentoService;
import br.unicesumar.aep.saude.service.PacienteService;
import br.unicesumar.aep.saude.service.ProfissionalService;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Fluxo principal da PoC, de ponta a ponta, com as regras de negócio sobre o MongoDB. */
class FluxoCompletoMongoIntegracaoTest extends MongoIntegracaoBase {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 2, 7, 0);
    private static final LocalDateTime SEGUNDA_9H = LocalDateTime.of(2026, 3, 9, 9, 0);
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private PacienteService pacienteService;
    private ProfissionalService profissionalService;
    private AgendamentoService agendamentoService;

    @Override
    protected void prepararRepositorios(MongoDatabase database) {
        MongoPacienteRepository pacientes = new MongoPacienteRepository(database);
        MongoProfissionalRepository profissionais = new MongoProfissionalRepository(database);
        pacienteService = new PacienteService(pacientes);
        profissionalService = new ProfissionalService(profissionais);
        agendamentoService = new AgendamentoService(new MongoAgendamentoRepository(database),
                pacientes, profissionais, Clock.fixed(AGORA.atZone(ZONA).toInstant(), ZONA));
    }

    @Test
    @DisplayName("cadastra, agenda, bloqueia conflito, confirma, conclui e grava o histórico")
    void fluxoPrincipal() {
        Paciente maria = pacienteService.cadastrar(Fixtures.maria());
        Profissional carlos = profissionalService.cadastrar(Fixtures.carlos());

        Agendamento consulta = agendamentoService.agendar(
                maria.getId(), carlos.getId(), SEGUNDA_9H, TipoAtendimento.CONSULTA);
        assertThrows(RegraNegocioException.class, () -> agendamentoService.agendar(
                maria.getId(), carlos.getId(), SEGUNDA_9H.plusMinutes(15), TipoAtendimento.EXAME));
        assertThrows(RegraNegocioException.class, () -> pacienteService.cadastrar(Fixtures.maria()));

        agendamentoService.confirmar(consulta.getId());
        agendamentoService.realizar(consulta.getId(), "Consulta de rotina", List.of("Losartana 50mg"));

        Agendamento concluida = agendamentoService.buscarPorId(consulta.getId());
        assertEquals(StatusAgendamento.REALIZADO, concluida.getStatus());
        assertEquals(2, concluida.getAlteracoes().size());
        assertEquals("Consulta de rotina", pacienteService.historico(maria.getId()).get(0).getDescricao());
    }
}
