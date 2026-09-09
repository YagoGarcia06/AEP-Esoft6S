package br.unicesumar.aep.saude.model;

/**
 * Ciclo de vida de um agendamento. Só um agendamento AGENDADO pode
 * transitar para REALIZADO ou CANCELADO.
 */
public enum StatusAgendamento {

    AGENDADO,
    REALIZADO,
    CANCELADO;

    public boolean isAtivo() {
        return this == AGENDADO;
    }

    public boolean permiteTransicaoPara(StatusAgendamento novoStatus) {
        return isAtivo() && (novoStatus == REALIZADO || novoStatus == CANCELADO);
    }
}
