package br.unicesumar.aep.saude.repository;

import br.unicesumar.aep.saude.model.ItemAgendaDia;
import br.unicesumar.aep.saude.model.ResumoAtendimentos;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de leitura que cruzam as três coleções. No MongoDB são feitas com
 * o aggregation pipeline ($lookup e $group); em memória, com streams.
 */
public interface RelatorioRepository {

    /** Atendimentos do dia, em ordem de horário, com nomes de paciente e profissional. */
    List<ItemAgendaDia> agendaDoDia(LocalDate data);

    /** Totais por profissional no período (datas inclusivas), do maior para o menor. */
    List<ResumoAtendimentos> resumoPorProfissional(LocalDate inicio, LocalDate fim);

    /** Totais por tipo de atendimento no período (datas inclusivas), do maior para o menor. */
    List<ResumoAtendimentos> resumoPorTipo(LocalDate inicio, LocalDate fim);
}
