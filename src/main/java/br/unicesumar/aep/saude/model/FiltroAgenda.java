package br.unicesumar.aep.saude.model;

import br.unicesumar.aep.saude.exception.RegraNegocioException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Critérios opcionais para consultar a agenda. Um critério vazio não filtra;
 * {@link #TODOS} devolve todos os agendamentos.
 *
 * <p>As datas são inclusivas e se referem ao dia do atendimento.</p>
 */
public final class FiltroAgenda {

    public static final FiltroAgenda TODOS = new FiltroAgenda(null, null, null, null);

    private final LocalDate inicio;
    private final LocalDate fim;
    private final String profissionalId;
    private final StatusAgendamento status;

    public FiltroAgenda(LocalDate inicio, LocalDate fim, String profissionalId, StatusAgendamento status) {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new RegraNegocioException("A data final do filtro não pode ser anterior à inicial.");
        }
        this.inicio = inicio;
        this.fim = fim;
        this.profissionalId = (profissionalId == null || profissionalId.isBlank()) ? null : profissionalId;
        this.status = status;
    }

    public Optional<LocalDate> getInicio() {
        return Optional.ofNullable(inicio);
    }

    public Optional<LocalDate> getFim() {
        return Optional.ofNullable(fim);
    }

    public Optional<String> getProfissionalId() {
        return Optional.ofNullable(profissionalId);
    }

    public Optional<StatusAgendamento> getStatus() {
        return Optional.ofNullable(status);
    }

    /** Verdadeiro quando o agendamento atende a todos os critérios preenchidos. */
    public boolean aceita(Agendamento agendamento) {
        LocalDate dia = agendamento.getData();
        return (inicio == null || !dia.isBefore(inicio))
                && (fim == null || !dia.isAfter(fim))
                && (profissionalId == null || profissionalId.equals(agendamento.getProfissionalId()))
                && (status == null || status == agendamento.getStatus());
    }

    public boolean isVazio() {
        return inicio == null && fim == null && profissionalId == null && status == null;
    }
}
