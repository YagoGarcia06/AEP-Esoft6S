package br.unicesumar.aep.saude.service;

import br.unicesumar.aep.saude.exception.RecursoNaoEncontradoException;
import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.AlteracaoAgendamento;
import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Disponibilidade;
import br.unicesumar.aep.saude.model.Endereco;
import br.unicesumar.aep.saude.model.FiltroAgenda;
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

        service.cancelar(consulta.getId(), "Paciente desistiu");
        assertEquals(StatusAgendamento.CANCELADO, service.buscarPorId(consulta.getId()).getStatus());

        service.agendar(maria.getId(), carlos.getId(), PROXIMA_SEGUNDA_9H, TipoAtendimento.CONSULTA);
        assertEquals(2, agendamentos.contar());
    }

    @Test
    @DisplayName("falha ao operar agendamento inexistente")
    void deveFalharComAgendamentoInexistente() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorId("inexistente"));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.cancelar("inexistente", "Paciente desistiu"));
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

    private Agendamento agendarConsulta(LocalDateTime dataHora) {
        return service.agendar(maria.getId(), carlos.getId(), dataHora, TipoAtendimento.CONSULTA);
    }

    /** Serviço sobre os mesmos repositórios, com o relógio em outro instante. */
    private AgendamentoService servicoEm(LocalDateTime instante) {
        Clock relogio = Clock.fixed(instante.atZone(ZONA).toInstant(), ZONA);
        return new AgendamentoService(agendamentos, pacientes, profissionais, relogio);
    }

    @Test
    @DisplayName("confirma a presença e persiste o novo status")
    void deveConfirmar() {
        Agendamento consulta = agendarConsulta(PROXIMA_SEGUNDA_9H);

        service.confirmar(consulta.getId());

        assertEquals(StatusAgendamento.CONFIRMADO, service.buscarPorId(consulta.getId()).getStatus());
        assertThrows(RegraNegocioException.class, () -> service.confirmar(consulta.getId()));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.confirmar("inexistente"));
    }

    @Test
    @DisplayName("um atendimento confirmado continua bloqueando o horário")
    void confirmadoContinuaOcupandoHorario() {
        Agendamento consulta = agendarConsulta(PROXIMA_SEGUNDA_9H);
        service.confirmar(consulta.getId());

        assertThrows(RegraNegocioException.class,
                () -> agendarConsulta(PROXIMA_SEGUNDA_9H.plusMinutes(15)));
    }

    @Test
    @DisplayName("remarca para um horário livre e libera o horário antigo")
    void deveRemarcar() {
        Agendamento consulta = agendarConsulta(PROXIMA_SEGUNDA_9H);
        service.confirmar(consulta.getId());

        LocalDateTime novoHorario = PROXIMA_SEGUNDA_9H.plusDays(7).plusHours(1);
        service.remarcar(consulta.getId(), novoHorario);

        Agendamento remarcado = service.buscarPorId(consulta.getId());
        assertEquals(novoHorario, remarcado.getDataHora());
        assertEquals(StatusAgendamento.AGENDADO, remarcado.getStatus());
        assertEquals(1, service.agendaDoProfissional(carlos.getId(), novoHorario.toLocalDate()).size());

        agendarConsulta(PROXIMA_SEGUNDA_9H);
        assertEquals(2, agendamentos.contar());
    }

    @Test
    @DisplayName("permite remarcar para um horário que se sobrepõe ao próprio horário antigo")
    void deveRemarcarSobreOProprioHorario() {
        Agendamento consulta = agendarConsulta(PROXIMA_SEGUNDA_9H);

        service.remarcar(consulta.getId(), PROXIMA_SEGUNDA_9H.plusMinutes(15));

        assertEquals(PROXIMA_SEGUNDA_9H.plusMinutes(15), service.buscarPorId(consulta.getId()).getDataHora());
    }

    @Test
    @DisplayName("aplica na remarcação as mesmas regras do agendamento")
    void deveValidarRemarcacao() {
        Agendamento consulta = agendarConsulta(PROXIMA_SEGUNDA_9H);
        agendarConsulta(PROXIMA_SEGUNDA_9H.plusHours(1));

        assertThrows(RegraNegocioException.class,
                () -> service.remarcar(consulta.getId(), AGORA.minusDays(1)));
        assertThrows(RegraNegocioException.class,
                () -> service.remarcar(consulta.getId(), PROXIMA_SEGUNDA_9H.withHour(14)));
        assertThrows(RegraNegocioException.class,
                () -> service.remarcar(consulta.getId(), PROXIMA_SEGUNDA_9H.plusMinutes(50)));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.remarcar("inexistente", PROXIMA_SEGUNDA_9H));

        assertEquals(PROXIMA_SEGUNDA_9H, service.buscarPorId(consulta.getId()).getDataHora());
    }

    @Test
    @DisplayName("não remarca atendimento já encerrado")
    void naoDeveRemarcarEncerrado() {
        Agendamento consulta = agendarConsulta(PROXIMA_SEGUNDA_9H);
        service.cancelar(consulta.getId(), "Paciente desistiu");

        assertThrows(RegraNegocioException.class,
                () -> service.remarcar(consulta.getId(), PROXIMA_SEGUNDA_9H.plusDays(7)));
    }

    @Test
    @DisplayName("falha ao remarcar quando o profissional foi removido")
    void deveFalharRemarcacaoSemProfissional() {
        Agendamento consulta = agendarConsulta(PROXIMA_SEGUNDA_9H);
        profissionais.remover(carlos.getId());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.remarcar(consulta.getId(), PROXIMA_SEGUNDA_9H.plusDays(7)));
    }

    @Test
    @DisplayName("registra falta somente depois do horário do atendimento")
    void deveRegistrarFaltaAposOHorario() {
        Agendamento consulta = agendarConsulta(PROXIMA_SEGUNDA_9H);

        assertThrows(RegraNegocioException.class, () -> service.registrarFalta(consulta.getId()));

        servicoEm(PROXIMA_SEGUNDA_9H.plusMinutes(40)).registrarFalta(consulta.getId());

        assertEquals(StatusAgendamento.NAO_COMPARECEU, service.buscarPorId(consulta.getId()).getStatus());
        assertTrue(service.agendaDoPaciente(maria.getId()).stream()
                .noneMatch(a -> a.getStatus().isAtivo()));
    }

    @Test
    @DisplayName("registra cada mudança do agendamento com o horário do relógio")
    void deveRegistrarAlteracoes() {
        Agendamento consulta = agendarConsulta(PROXIMA_SEGUNDA_9H);
        assertTrue(consulta.getAlteracoes().isEmpty());

        service.confirmar(consulta.getId());
        service.remarcar(consulta.getId(), PROXIMA_SEGUNDA_9H.plusHours(1));
        service.cancelar(consulta.getId(), "Paciente viajou");

        List<AlteracaoAgendamento> alteracoes = service.buscarPorId(consulta.getId()).getAlteracoes();
        assertEquals(3, alteracoes.size());

        assertEquals(StatusAgendamento.AGENDADO, alteracoes.get(0).getStatusAnterior());
        assertEquals(StatusAgendamento.CONFIRMADO, alteracoes.get(0).getStatusNovo());
        assertEquals(AGORA, alteracoes.get(0).getMomento());

        assertEquals(StatusAgendamento.CONFIRMADO, alteracoes.get(1).getStatusAnterior());
        assertEquals(StatusAgendamento.AGENDADO, alteracoes.get(1).getStatusNovo());
        assertEquals("Remarcado de 09/03/2026 09:00 para 09/03/2026 10:00",
                alteracoes.get(1).getObservacao());

        assertEquals(StatusAgendamento.CANCELADO, alteracoes.get(2).getStatusNovo());
        assertEquals("Motivo: Paciente viajou", alteracoes.get(2).getObservacao());
    }

    @Test
    @DisplayName("exige motivo para cancelar")
    void deveExigirMotivoParaCancelar() {
        Agendamento consulta = agendarConsulta(PROXIMA_SEGUNDA_9H);

        assertThrows(RegraNegocioException.class, () -> service.cancelar(consulta.getId(), " "));
        assertThrows(RegraNegocioException.class, () -> service.cancelar(consulta.getId(), null));

        Agendamento inalterado = service.buscarPorId(consulta.getId());
        assertEquals(StatusAgendamento.AGENDADO, inalterado.getStatus());
        assertTrue(inalterado.getAlteracoes().isEmpty());
    }

    @Test
    @DisplayName("filtra a agenda por profissional, período e status")
    void deveFiltrarAgenda() {
        Profissional ana = new Profissional("Dra. Ana Prado", "615.406.180-73", "CRM-PR 54321",
                new Contato("ana@clinica.com", "44 92222-2222"));
        ana.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        profissionais.salvar(ana);

        Agendamento primeira = agendarConsulta(PROXIMA_SEGUNDA_9H);
        agendarConsulta(PROXIMA_SEGUNDA_9H.plusDays(7));
        service.agendar(maria.getId(), ana.getId(), PROXIMA_SEGUNDA_9H, TipoAtendimento.EXAME);
        service.confirmar(primeira.getId());

        LocalDate dia = PROXIMA_SEGUNDA_9H.toLocalDate();
        assertEquals(3, service.filtrar(FiltroAgenda.TODOS).size());
        assertEquals(2, service.filtrar(new FiltroAgenda(dia, dia, null, null)).size());
        assertEquals(2, service.filtrar(new FiltroAgenda(null, null, carlos.getId(), null)).size());

        List<Agendamento> confirmados = service.filtrar(
                new FiltroAgenda(dia, dia, carlos.getId(), StatusAgendamento.CONFIRMADO));
        assertEquals(1, confirmados.size());
        assertEquals(primeira.getId(), confirmados.get(0).getId());
        assertThrows(NullPointerException.class, () -> service.filtrar(null));
    }
}
