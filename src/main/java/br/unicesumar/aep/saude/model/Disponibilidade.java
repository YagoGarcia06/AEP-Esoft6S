package br.unicesumar.aep.saude.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Janela semanal de atendimento de um profissional (ex.: segunda, 08:00 às 12:00).
 * É um subdocumento da coleção "profissionais".
 */
public final class Disponibilidade {

    private final DayOfWeek diaSemana;
    private final LocalTime horaInicio;
    private final LocalTime horaFim;

    public Disponibilidade(DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim) {
        this.diaSemana = Objects.requireNonNull(diaSemana, "Dia da semana é obrigatório.");
        this.horaInicio = Objects.requireNonNull(horaInicio, "Hora de início é obrigatória.");
        this.horaFim = Objects.requireNonNull(horaFim, "Hora de fim é obrigatória.");
        if (!horaFim.isAfter(horaInicio)) {
            throw new IllegalArgumentException("Hora de fim deve ser posterior à hora de início.");
        }
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    /**
     * Indica se o intervalo informado cabe inteiramente dentro desta janela.
     */
    public boolean cobre(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            return false;
        }
        if (inicio.getDayOfWeek() != diaSemana) {
            return false;
        }
        if (!inicio.toLocalDate().equals(fim.toLocalDate())) {
            return false;
        }
        return !inicio.toLocalTime().isBefore(horaInicio) && !fim.toLocalTime().isAfter(horaFim);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Disponibilidade)) {
            return false;
        }
        Disponibilidade outra = (Disponibilidade) obj;
        return diaSemana == outra.diaSemana
                && horaInicio.equals(outra.horaInicio)
                && horaFim.equals(outra.horaFim);
    }

    @Override
    public int hashCode() {
        return Objects.hash(diaSemana, horaInicio, horaFim);
    }

    @Override
    public String toString() {
        return diaSemana + " " + horaInicio + "-" + horaFim;
    }
}
