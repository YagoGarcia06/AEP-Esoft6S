package br.unicesumar.aep.saude.service;

import br.unicesumar.aep.saude.exception.RecursoNaoEncontradoException;
import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Disponibilidade;
import br.unicesumar.aep.saude.model.Endereco;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.repository.memory.InMemoryAgendamentoRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryPacienteRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendamentoServiceTest {

    /** Segunda-feira, 02/03/2026 às 07:00 — "agora" fixo para os testes. */
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 2, 7, 0);

    /** Segunda-feira seguinte, dentro da disponibilidade do profissional. */
    private static final LocalDateTime PROXIMA_SEGUNDA_9H = LocalDateTime.of(2026, 3, 9, 9, 0);

    private static final ZoneId ZONA = ZoneId.systemDefault();

    private InMemoryPacienteRepository pacientes;
    private InMemoryProfissionalRepository profissionais;
    private InMemoryAgendamentoRepository agendamentos;
    private AgendamentoService service;
    private Paciente maria;
    private Profissional carlos;

    @BeforeEach
    void preparar() {
        pacientes = new InMemoryPacienteRepository();
        profissionais = new InMemoryProfissionalRepository();
        agendamentos = new InMemoryAgendamentoRepository();

        Clock relogioFixo = Clock.fixed(AGORA.atZone(ZONA).toInstant(), ZONA);
        service = new AgendamentoService(agendamentos, pacientes, profissionais, relogioFixo);

        maria = pacientes.salvar(new Paciente("Maria Souza", "529.982.247-25",
                LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR")));

        carlos = new Profissional("Dr. Carlos Lima", "111.444.777-35", "CRM-PR 12345",
                new Contato("carlos@clinica.com", "44 91111-1111"));
        carlos.adicionarEspecialidade("Cardiologia");
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        profissionais.salvar(carlos);
    }

    @Test
    @DisplayName("agenda uma consulta válida e persiste o registro")
    void deveAgendar() {
        Agendamento consulta = service.agendar(
                maria.getId(), carlos.getId(), PROXIMA_SEGUNDA_9H, TipoAtendimento.CONSULTA);

        assertEquals(StatusAgendamento.AGENDADO, consulta.getStatus());
        assertEquals(maria.getId(), consulta.getPacienteId());
        assertEquals(carlos.getId(), consulta.getProfissionalId());
        assertEquals(30, consulta.getDuracaoMinutos());
        assertEquals(1, agendamentos.contar());
    }

    @Test
    @DisplayName("exige paciente e profissional existentes")
    void deveExigirEntidadesExistentes() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.agendar(
                "inexistente", carlos.getId(), PROXIMA_SEGUNDA_9H, TipoAtendimento.CONSULTA));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.agendar(
                maria.getId(), "inexistente", PROXIMA_SEGUNDA_9H, TipoAtendimento.CONSULTA));
        assertEquals(0, agendamentos.contar());
    }

    @Test
    @DisplayName("exige data e tipo de atendimento")
    void deveExigirParametrosObrigatorios() {
        assertThrows(NullPointerException.class, () -> service.agendar(
                maria.getId(), carlos.getId(), null, TipoAtendimento.CONSULTA));
        assertThrows(NullPointerException.class, () -> service.agendar(
                maria.getId(), carlos.getId(), PROXIMA_SEGUNDA_9H, null));
    }

    @Test
    @DisplayName("bloqueia agendamento em data passada")
    void deveBloquearDataPassada() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class, () -> service.agendar(
                maria.getId(), carlos.getId(),
                LocalDateTime.of(2026, 2, 23, 9, 0), TipoAtendimento.CONSULTA));

        assertTrue(erro.getMessage().contains("passado"));
        assertEquals(0, agendamentos.contar());
    }

    @Test
    @DisplayName("bloqueia horário fora da disponibilidade do profissional")
    void deveBloquearForaDaDisponibilidade() {
        assertThrows(RegraNegocioException.class, () -> service.agendar(
                maria.getId(), carlos.getId(),
                LocalDateTime.of(2026, 3, 9, 14, 0), TipoAtendimento.CONSULTA));

        assertThrows(RegraNegocioException.class, () -> service.agendar(
                maria.getId(), carlos.getId(),
                LocalDateTime.of(2026, 3, 10, 9, 0), TipoAtendimento.CONSULTA));

        assertEquals(0, agendamentos.contar());
    }

    @Test
    @DisplayName("bloqueia agendamento que ultrapassa o fim da janela")
    void deveBloquearQueUltrapassaJanela() {
        assertThrows(RegraNegocioException.class, () -> service.agendar(
                maria.getId(), carlos.getId(),
                LocalDateTime.of(2026, 3, 9, 11, 45), TipoAtendimento.CONSULTA));
    }

    @Test
    @DisplayName("bloqueia conflito de horário na agenda do profissional")
    void deveBloquearConflito() {
        service.agendar(maria.getId(), carlos.getId(), PROXIMA_SEGUNDA_9H, TipoAtendimento.CONSULTA);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class, () -> service.agendar(
                maria.getId(), carlos.getId(),
                PROXIMA_SEGUNDA_9H.plusMinutes(15), TipoAtendimento.EXAME));

        assertTrue(erro.getMessage().contains("Conflito"));
        assertEquals(1, agendamentos.contar());
    }

    @Test
    @DisplayName("permite agendar em horário encostado no anterior")
    void devePermitirHorarioAdjacente() {
        service.agendar(maria.getId(), carlos.getId(), PROXIMA_SEGUNDA_9H, TipoAtendimento.CONSULTA);
        service.agendar(maria.getId(), carlos.getId(),
                PROXIMA_SEGUNDA_9H.plusMinutes(30), TipoAtendimento.CONSULTA);

        assertEquals(2, agendamentos.contar());
    }

    @Test
    @DisplayName("libera o horário depois do cancelamento")
    void deveLiberarHorarioAposCancelamento() {
        Agendamento consulta = service.agendar(
                maria.getId(), carlos.getId(), PROXIMA_SEGUNDA_9H, TipoAtendimento.CONSULTA);

        service.cancelar(consulta.getId());
        assertEquals(StatusAgendamento.CANCELADO, service.buscarPorId(consulta.getId()).getStatus());

        service.agendar(maria.getId(), carlos.getId(), PROXIMA_SEGUNDA_9H, TipoAtendimento.CONSULTA);
        assertEquals(2, agendamentos.contar());
    }

    @Test
    @DisplayName("falha ao operar agendamento inexistente")
    void deveFalharComAgendamentoInexistente() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorId("inexistente"));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.cancelar("inexistente"));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.realizar("inexistente", "Descrição", null));
    }

    @Test
    @DisplayName("conclui o atendimento e grava o histórico do paciente")
    void deveRealizarEGravarHistorico() {
        Agendamento consulta = service.agendar(
                maria.getId(), carlos.getId(), PROXIMA_SEGUNDA_9H, TipoAtendimento.CONSULTA);

        service.realizar(consulta.getId(), "Consulta de rotina em cardiologia",
                List.of("Losartana 50mg"));

        Paciente atualizada = pacientes.buscarPorId(maria.getId()).orElseThrow();
        assertEquals(StatusAgendamento.REALIZADO, service.buscarPorId(consulta.getId()).getStatus());
        assertEquals(1, atualizada.getHistorico().size());
        assertEquals("Consulta de rotina em cardiologia",
                atualizada.getHistorico().get(0).getDescricao());
        assertEquals(LocalDate.of(2026, 3, 9), atualizada.getHistorico().get(0).getData());
        assertEquals(List.of("Losartana 50mg"), atualizada.getHistorico().get(0).getPrescricoes());
    }

    @Test
    @DisplayName("não conclui duas vezes o mesmo atendimento")
    void deveBloquearConclusaoDuplicada() {
        Agendamento consulta = service.agendar(
                maria.getId(), carlos.getId(), PROXIMA_SEGUNDA_9H, TipoAtendimento.CONSULTA);
        service.realizar(consulta.getId(), "Consulta de rotina", null);

        assertThrows(RegraNegocioException.class,
                () -> service.realizar(consulta.getId(), "Outra vez", null));
    }

    @Test
    @DisplayName("consulta as agendas do profissional e do paciente")
    void deveConsultarAgendas() {
        service.agendar(maria.getId(), carlos.getId(), PROXIMA_SEGUNDA_9H, TipoAtendimento.CONSULTA);
        service.agendar(maria.getId(), carlos.getId(),
                PROXIMA_SEGUNDA_9H.plusMinutes(30), TipoAtendimento.CONSULTA);

        assertEquals(2, service.agendaDoProfissional(carlos.getId(), LocalDate.of(2026, 3, 9)).size());
        assertEquals(0, service.agendaDoProfissional(carlos.getId(), LocalDate.of(2026, 3, 16)).size());
        assertEquals(2, service.agendaDoPaciente(maria.getId()).size());
    }

    @Test
    @DisplayName("exige todos os repositórios na construção")
    void deveExigirDependencias() {
        assertThrows(NullPointerException.class,
                () -> new AgendamentoService(null, pacientes, profissionais));
        assertThrows(NullPointerException.class,
                () -> new AgendamentoService(agendamentos, null, profissionais));
        assertThrows(NullPointerException.class,
                () -> new AgendamentoService(agendamentos, pacientes, null));
    }
}
