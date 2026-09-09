package br.unicesumar.aep.saude.service;

import br.unicesumar.aep.saude.exception.RecursoNaoEncontradoException;
import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.RegistroClinico;
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
 * Núcleo de regras da PoC: agendar, cancelar e concluir atendimentos,
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

        if (!dataHora.isAfter(LocalDateTime.now(clock))) {
            throw new RegraNegocioException("Não é possível agendar em data/hora no passado.");
        }

        Agendamento novo = new Agendamento(paciente.getId(), profissional.getId(), dataHora, tipo);

        if (!profissional.atendeEm(novo.getDataHora(), novo.getFim())) {
            throw new RegraNegocioException(
                    "O profissional " + profissional.getNome() + " não atende nesse horário.");
        }

        for (Agendamento existente : agendamentoRepository
                .buscarPorProfissionalEData(profissional.getId(), novo.getData())) {
            if (novo.conflitaCom(existente)) {
                throw new RegraNegocioException(
                        "Conflito de agenda: o profissional já tem atendimento das "
                                + existente.getDataHora().toLocalTime() + " às "
                                + existente.getFim().toLocalTime() + ".");
            }
        }

        return agendamentoRepository.salvar(novo);
    }

    public Agendamento buscarPorId(String id) {
        return agendamentoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + id));
    }

    public Agendamento cancelar(String agendamentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        agendamento.cancelar();
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

        agendamento.realizar();
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
}
