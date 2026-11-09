package br.unicesumar.aep.saude.model;

import java.util.Objects;

/**
 * Totais de atendimentos de um grupo (um profissional ou um tipo de
 * atendimento) em um período. Produzido por agregação.
 */
public final class ResumoAtendimentos {

    private final String grupo;
    private final long total;
    private final long realizados;
    private final long faltas;
    private final long cancelados;

    public ResumoAtendimentos(String grupo, long total, long realizados, long faltas, long cancelados) {
        this.grupo = Objects.requireNonNull(grupo, "Grupo é obrigatório.");
        if (total < 0 || realizados < 0 || faltas < 0 || cancelados < 0) {
            throw new IllegalArgumentException("Totais não podem ser negativos.");
        }
        if (realizados + faltas + cancelados > total) {
            throw new IllegalArgumentException("A soma por status não pode passar do total.");
        }
        this.total = total;
        this.realizados = realizados;
        this.faltas = faltas;
        this.cancelados = cancelados;
    }

    public String getGrupo() {
        return grupo;
    }

    public long getTotal() {
        return total;
    }

    public long getRealizados() {
        return realizados;
    }

    public long getFaltas() {
        return faltas;
    }

    public long getCancelados() {
        return cancelados;
    }

    /** Agendamentos ainda ativos (agendados ou confirmados). */
    public long getPendentes() {
        return total - realizados - faltas - cancelados;
    }

    /**
     * Percentual de faltas entre os atendimentos que já aconteceram
     * (realizados + faltas). Zero quando nenhum aconteceu ainda.
     */
    public double getPercentualFaltas() {
        long ocorridos = realizados + faltas;
        return ocorridos == 0 ? 0.0 : faltas * 100.0 / ocorridos;
    }

    @Override
    public String toString() {
        return String.format("%s: %d agendamentos, %d realizados, %d faltas (%.1f%%), %d cancelados",
                grupo, total, realizados, faltas, getPercentualFaltas(), cancelados);
    }
}
