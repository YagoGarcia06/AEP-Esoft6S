package br.unicesumar.aep.saude.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro imutável de uma mudança no agendamento: confirmação, remarcação,
 * conclusão, falta ou cancelamento. É um subdocumento da lista
 * "alteracoes" dentro da coleção "agendamentos".
 */
public final class AlteracaoAgendamento {

    private final LocalDateTime momento;
    private final StatusAgendamento statusAnterior;
    private final StatusAgendamento statusNovo;
    private final String observacao;

    public AlteracaoAgendamento(LocalDateTime momento, StatusAgendamento statusAnterior,
                                StatusAgendamento statusNovo, String observacao) {
        this.momento = Objects.requireNonNull(momento, "Momento da alteração é obrigatório.");
        this.statusAnterior = Objects.requireNonNull(statusAnterior, "Status anterior é obrigatório.");
        this.statusNovo = Objects.requireNonNull(statusNovo, "Novo status é obrigatório.");
        this.observacao = (observacao == null || observacao.isBlank()) ? null : observacao.trim();
    }

    public LocalDateTime getMomento() {
        return momento;
    }

    public StatusAgendamento getStatusAnterior() {
        return statusAnterior;
    }

    public StatusAgendamento getStatusNovo() {
        return statusNovo;
    }

    /** Texto livre opcional, como o motivo do cancelamento. Pode ser nulo. */
    public String getObservacao() {
        return observacao;
    }

    @Override
    public String toString() {
        String texto = momento + ": " + statusAnterior + " -> " + statusNovo;
        return observacao == null ? texto : texto + " (" + observacao + ")";
    }
}
