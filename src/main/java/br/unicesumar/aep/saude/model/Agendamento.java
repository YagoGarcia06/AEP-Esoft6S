package br.unicesumar.aep.saude.model;

import br.unicesumar.aep.saude.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Agendamento de consulta ou exame. É a entidade raiz da coleção
 * "agendamentos" e se relaciona com "pacientes" e "profissionais" por
 * referência de id.
 */
public class Agendamento {

    private final String id;
    private final String pacienteId;
    private final String profissionalId;
    private LocalDateTime dataHora;
    private final int duracaoMinutos;
    private final TipoAtendimento tipo;
    private StatusAgendamento status;

    public Agendamento(String pacienteId, String profissionalId,
                       LocalDateTime dataHora, TipoAtendimento tipo) {
        this(null, pacienteId, profissionalId, dataHora,
                tipo == null ? 0 : tipo.getDuracaoPadraoMinutos(), tipo, StatusAgendamento.AGENDADO);
    }

    public Agendamento(String id, String pacienteId, String profissionalId, LocalDateTime dataHora,
                       int duracaoMinutos, TipoAtendimento tipo, StatusAgendamento status) {
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

    public void confirmar() {
        transitarPara(StatusAgendamento.CONFIRMADO);
    }

    public void realizar() {
        transitarPara(StatusAgendamento.REALIZADO);
    }

    public void registrarFalta() {
        transitarPara(StatusAgendamento.NAO_COMPARECEU);
    }

    public void cancelar() {
        transitarPara(StatusAgendamento.CANCELADO);
    }

    /**
     * Move o atendimento para outro horário. Só vale para agendamentos ativos;
     * um atendimento já confirmado volta para AGENDADO, pois a nova data
     * precisa ser confirmada de novo com o paciente.
     */
    public void remarcarPara(LocalDateTime novaDataHora) {
        verificarRemarcacao(novaDataHora);
        this.dataHora = novaDataHora;
        this.status = StatusAgendamento.AGENDADO;
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

    private void transitarPara(StatusAgendamento novoStatus) {
        if (!status.permiteTransicaoPara(novoStatus)) {
            throw new RegraNegocioException(
                    "Não é possível mudar o agendamento de " + status + " para " + novoStatus + ".");
        }
        this.status = novoStatus;
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
