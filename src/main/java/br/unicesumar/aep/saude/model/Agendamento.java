package br.unicesumar.aep.saude.model;

import br.unicesumar.aep.saude.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agendamento de consulta ou exame. É a entidade raiz da coleção
 * "agendamentos" e se relaciona com "pacientes" e "profissionais" por
 * referência de id. Cada mudança de status fica registrada na lista de
 * subdocumentos "alteracoes".
 */
public class Agendamento {

    private final String id;
    private final String pacienteId;
    private final String profissionalId;
    private LocalDateTime dataHora;
    private final int duracaoMinutos;
    private final TipoAtendimento tipo;
    private StatusAgendamento status;
    private final List<AlteracaoAgendamento> alteracoes;

    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public Agendamento(String pacienteId, String profissionalId,
                       LocalDateTime dataHora, TipoAtendimento tipo) {
        this(null, pacienteId, profissionalId, dataHora,
                tipo == null ? 0 : tipo.getDuracaoPadraoMinutos(), tipo, StatusAgendamento.AGENDADO);
    }

    public Agendamento(String id, String pacienteId, String profissionalId, LocalDateTime dataHora,
                       int duracaoMinutos, TipoAtendimento tipo, StatusAgendamento status) {
        this(id, pacienteId, profissionalId, dataHora, duracaoMinutos, tipo, status, List.of());
    }

    public Agendamento(String id, String pacienteId, String profissionalId, LocalDateTime dataHora,
                       int duracaoMinutos, TipoAtendimento tipo, StatusAgendamento status,
                       List<AlteracaoAgendamento> alteracoes) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("Id do paciente é obrigatório.");
        }
        if (profissionalId == null || profissionalId.isBlank()) {
            throw new IllegalArgumentException("Id do profissional é obrigatório.");
        }
        this.tipo = Objects.requireNonNull(tipo, "Tipo de atendimento é obrigatório.");
        this.dataHora = Objects.requireNonNull(dataHora, "Data e hora são obrigatórias.");
        if (duracaoMinutos <= 0) {
            throw new IllegalArgumentException("Duração deve ser maior que zero.");
        }
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        this.pacienteId = pacienteId;
        this.profissionalId = profissionalId;
        this.duracaoMinutos = duracaoMinutos;
        this.status = Objects.requireNonNull(status, "Status é obrigatório.");
        this.alteracoes = new ArrayList<>(alteracoes == null ? List.of() : alteracoes);
    }

    public LocalDateTime getFim() {
        return dataHora.plusMinutes(duracaoMinutos);
    }

    public LocalDate getData() {
        return dataHora.toLocalDate();
    }

    /**
     * Dois agendamentos conflitam quando pertencem ao mesmo profissional,
     * ambos estão ativos e seus intervalos se sobrepõem.
     */
    public boolean conflitaCom(Agendamento outro) {
        if (outro == null || this.equals(outro)) {
            return false;
        }
        if (!profissionalId.equals(outro.profissionalId)) {
            return false;
        }
        if (!status.isAtivo() || !outro.status.isAtivo()) {
            return false;
        }
        return dataHora.isBefore(outro.getFim()) && outro.dataHora.isBefore(getFim());
    }

    public void confirmar(LocalDateTime momento) {
        transitarPara(StatusAgendamento.CONFIRMADO, momento, null);
    }

    public void realizar(LocalDateTime momento) {
        transitarPara(StatusAgendamento.REALIZADO, momento, null);
    }

    public void registrarFalta(LocalDateTime momento) {
        transitarPara(StatusAgendamento.NAO_COMPARECEU, momento, null);
    }

    /** Cancela o atendimento; o motivo é obrigatório e fica no histórico. */
    public void cancelar(LocalDateTime momento, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("Informe o motivo do cancelamento.");
        }
        transitarPara(StatusAgendamento.CANCELADO, momento, "Motivo: " + motivo.trim());
    }

    /**
     * Move o atendimento para outro horário. Só vale para agendamentos ativos;
     * um atendimento já confirmado volta para AGENDADO, pois a nova data
     * precisa ser confirmada de novo com o paciente.
     */
    public void remarcarPara(LocalDateTime novaDataHora, LocalDateTime momento) {
        verificarRemarcacao(novaDataHora);
        String observacao = "Remarcado de " + dataHora.format(FORMATO_DATA_HORA)
                + " para " + novaDataHora.format(FORMATO_DATA_HORA);
        registrar(momento, StatusAgendamento.AGENDADO, observacao);
        this.dataHora = novaDataHora;
    }

    /** Lança exceção se o agendamento não puder ir para a nova data e hora. */
    public void verificarRemarcacao(LocalDateTime novaDataHora) {
        Objects.requireNonNull(novaDataHora, "Nova data e hora são obrigatórias.");
        if (!status.isAtivo()) {
            throw new RegraNegocioException(
                    "Não é possível remarcar um agendamento com status " + status + ".");
        }
        if (novaDataHora.equals(dataHora)) {
            throw new RegraNegocioException("A nova data e hora é igual à atual.");
        }
    }

    private void transitarPara(StatusAgendamento novoStatus, LocalDateTime momento, String observacao) {
        if (!status.permiteTransicaoPara(novoStatus)) {
            throw new RegraNegocioException(
                    "Não é possível mudar o agendamento de " + status + " para " + novoStatus + ".");
        }
        registrar(momento, novoStatus, observacao);
    }

    private void registrar(LocalDateTime momento, StatusAgendamento novoStatus, String observacao) {
        alteracoes.add(new AlteracaoAgendamento(momento, status, novoStatus, observacao));
        this.status = novoStatus;
    }

    /** Histórico de mudanças, da mais antiga para a mais recente. */
    public List<AlteracaoAgendamento> getAlteracoes() {
        return Collections.unmodifiableList(alteracoes);
    }

    public String getId() {
        return id;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public String getProfissionalId() {
        return profissionalId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public TipoAtendimento getTipo() {
        return tipo;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Agendamento)) {
            return false;
        }
        return id.equals(((Agendamento) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return tipo.getDescricao() + " em " + dataHora + " (" + duracaoMinutos + "min) - " + status;
    }
}
