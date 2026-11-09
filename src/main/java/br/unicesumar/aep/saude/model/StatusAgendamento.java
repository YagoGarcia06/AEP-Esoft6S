package br.unicesumar.aep.saude.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de um agendamento.
 *
 * <pre>
 * AGENDADO ──► CONFIRMADO ──► REALIZADO
 *    │             │
 *    │             ├────────► NAO_COMPARECEU
 *    │             └────────► CANCELADO
 *    ├──────────────────────► REALIZADO / NAO_COMPARECEU / CANCELADO
 * </pre>
 *
 * AGENDADO e CONFIRMADO ocupam a agenda do profissional; os demais são finais.
 */
public enum StatusAgendamento {

    AGENDADO("Agendado"),
    CONFIRMADO("Confirmado"),
    REALIZADO("Realizado"),
    NAO_COMPARECEU("Não compareceu"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusAgendamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Status que ainda ocupam o horário do profissional. */
    public boolean isAtivo() {
        return this == AGENDADO || this == CONFIRMADO;
    }

    public boolean permiteTransicaoPara(StatusAgendamento novoStatus) {
        return destinosPermitidos().contains(novoStatus);
    }

    private Set<StatusAgendamento> destinosPermitidos() {
        return switch (this) {
            case AGENDADO -> EnumSet.of(CONFIRMADO, REALIZADO, NAO_COMPARECEU, CANCELADO);
            case CONFIRMADO -> EnumSet.of(REALIZADO, NAO_COMPARECEU, CANCELADO);
            case REALIZADO, NAO_COMPARECEU, CANCELADO -> EnumSet.noneOf(StatusAgendamento.class);
        };
    }

    @Override
    public String toString() {
        return descricao;
    }
}
