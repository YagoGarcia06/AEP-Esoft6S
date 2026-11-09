package br.unicesumar.aep.saude.service;

import br.unicesumar.aep.saude.exception.RecursoNaoEncontradoException;
import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.RegistroClinico;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.repository.AgendamentoRepository;
import br.unicesumar.aep.saude.repository.PacienteRepository;
import br.unicesumar.aep.saude.repository.ProfissionalRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Núcleo de regras da PoC: agendar, confirmar, remarcar, cancelar, registrar
 * falta e concluir atendimentos,
 * alimentando o histórico clínico do paciente.
 *
 * O Clock é injetável para que as regras de data possam ser testadas
 * de forma determinística.
 */
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final Clock clock;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              PacienteRepository pacienteRepository,
                              ProfissionalRepository profissionalRepository) {
        this(agendamentoRepository, pacienteRepository, profissionalRepository, Clock.systemDefaultZone());
    }

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              PacienteRepository pacienteRepository,
                              ProfissionalRepository profissionalRepository,
                              Clock clock) {
        this.agendamentoRepository = Objects.requireNonNull(agendamentoRepository);
        this.pacienteRepository = Objects.requireNonNull(pacienteRepository);
        this.profissionalRepository = Objects.requireNonNull(profissionalRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Agenda um atendimento validando, nesta ordem: existência do paciente e do
     * profissional, data futura, disponibilidade do profissional e ausência de
     * conflito na agenda dele.
     */
    public Agendamento agendar(String pacienteId, String profissionalId,
                               LocalDateTime dataHora, TipoAtendimento tipo) {
        Objects.requireNonNull(dataHora, "Data e hora são obrigatórias.");
        Objects.requireNonNull(tipo, "Tipo de atendimento é obrigatório.");

        Paciente paciente = pacienteRepository.buscarPorId(pacienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente não encontrado: " + pacienteId));
        Profissional profissional = profissionalRepository.buscarPorId(profissionalId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrado: " + profissionalId));

        Agendamento novo = new Agendamento(paciente.getId(), profissional.getId(), dataHora, tipo);
        validarHorario(novo, profissional);
        return agendamentoRepository.salvar(novo);
    }

    /**
     * Regras de horário comuns a agendar e remarcar: data futura, disponibilidade
     * do profissional e ausência de conflito com outros atendimentos ativos.
     */
    private void validarHorario(Agendamento candidato, Profissional profissional) {
        if (!candidato.getDataHora().isAfter(agora())) {
            throw new RegraNegocioException("Não é possível agendar em data/hora no passado.");
        }

        if (!profissional.atendeEm(candidato.getDataHora(), candidato.getFim())) {
            throw new RegraNegocioException(
                    "O profissional " + profissional.getNome() + " não atende nesse horário.");
        }

        for (Agendamento existente : agendamentoRepository
                .buscarPorProfissionalEData(profissional.getId(), candidato.getData())) {
            if (candidato.conflitaCom(existente)) {
                throw new RegraNegocioException(
                        "Conflito de agenda: o profissional já tem atendimento das "
                                + existente.getDataHora().toLocalTime() + " às "
                                + existente.getFim().toLocalTime() + ".");
            }
        }
    }

    public Agendamento buscarPorId(String id) {
        return agendamentoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + id));
    }

    /** Registra que o paciente confirmou presença. */
    public Agendamento confirmar(String agendamentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        agendamento.confirmar(agora());
        return agendamentoRepository.salvar(agendamento);
    }

    /**
     * Move o atendimento para outro horário, aplicando as mesmas regras do
     * agendamento original. O conflito consigo mesmo é ignorado, então é possível
     * remarcar para um horário que se sobrepõe ao antigo.
     */
    public Agendamento remarcar(String agendamentoId, LocalDateTime novaDataHora) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        agendamento.verificarRemarcacao(novaDataHora);
        Profissional profissional = profissionalRepository.buscarPorId(agendamento.getProfissionalId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Profissional do agendamento não encontrado: " + agendamento.getProfissionalId()));

        Agendamento candidato = new Agendamento(agendamento.getId(), agendamento.getPacienteId(),
                agendamento.getProfissionalId(), novaDataHora, agendamento.getDuracaoMinutos(),
                agendamento.getTipo(), StatusAgendamento.AGENDADO);
        validarHorario(candidato, profissional);

        agendamento.remarcarPara(novaDataHora, agora());
        return agendamentoRepository.salvar(agendamento);
    }

    /**
     * Marca que o paciente faltou. Só faz sentido depois do horário marcado.
     */
    public Agendamento registrarFalta(String agendamentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        if (agendamento.getDataHora().isAfter(agora())) {
            throw new RegraNegocioException(
                    "Só é possível registrar falta depois do horário do atendimento.");
        }
        agendamento.registrarFalta(agora());
        return agendamentoRepository.salvar(agendamento);
    }

    /** Cancela o atendimento, liberando o horário; o motivo fica no histórico. */
    public Agendamento cancelar(String agendamentoId, String motivo) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        agendamento.cancelar(agora(), motivo);
        return agendamentoRepository.salvar(agendamento);
    }

    /**
     * Conclui o atendimento e grava o registro correspondente no histórico do paciente.
     */
    public Agendamento realizar(String agendamentoId, String descricao, List<String> prescricoes) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        Paciente paciente = pacienteRepository.buscarPorId(agendamento.getPacienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Paciente do agendamento não encontrado: " + agendamento.getPacienteId()));

        agendamento.realizar(agora());
        paciente.adicionarRegistro(new RegistroClinico(
                agendamento.getData(),
                agendamento.getTipo(),
                agendamento.getProfissionalId(),
                descricao,
                prescricoes));

        pacienteRepository.salvar(paciente);
        return agendamentoRepository.salvar(agendamento);
    }

    public List<Agendamento> agendaDoProfissional(String profissionalId, LocalDate data) {
        return agendamentoRepository.buscarPorProfissionalEData(profissionalId, data);
    }

    public List<Agendamento> agendaDoPaciente(String pacienteId) {
        return agendamentoRepository.buscarPorPaciente(pacienteId);
    }

    /** Todos os agendamentos, do mais próximo para o mais distante. */
    public List<Agendamento> listarTodos() {
        List<Agendamento> todos = new ArrayList<>(agendamentoRepository.listarTodos());
        todos.sort(Comparator.comparing(Agendamento::getDataHora));
        return todos;
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock).withNano(0);
    }
}
