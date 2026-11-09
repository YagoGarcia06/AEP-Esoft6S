package br.unicesumar.aep.saude.repository;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.FiltroAgenda;

import java.time.LocalDate;
import java.util.List;

public interface AgendamentoRepository extends Repository<Agendamento> {

    List<Agendamento> buscarPorProfissionalEData(String profissionalId, LocalDate data);

    List<Agendamento> buscarPorPaciente(String pacienteId);

    /** Agendamentos que atendem ao filtro, em ordem de data e hora. */
    List<Agendamento> buscarComFiltro(FiltroAgenda filtro);
}
