package br.unicesumar.aep.saude.repository.memory;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.repository.AgendamentoRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class InMemoryAgendamentoRepository extends InMemoryRepository<Agendamento>
        implements AgendamentoRepository {

    @Override
    protected String idDe(Agendamento entidade) {
        return entidade.getId();
    }

    @Override
    public List<Agendamento> buscarPorProfissionalEData(String profissionalId, LocalDate data) {
        if (profissionalId == null || data == null) {
            return List.of();
        }
        return dados.values().stream()
                .filter(a -> a.getProfissionalId().equals(profissionalId))
                .filter(a -> a.getData().equals(data))
                .sorted(Comparator.comparing(Agendamento::getDataHora))
                .collect(Collectors.toList());
    }

    @Override
    public List<Agendamento> buscarPorPaciente(String pacienteId) {
        if (pacienteId == null) {
            return List.of();
        }
        return dados.values().stream()
                .filter(a -> a.getPacienteId().equals(pacienteId))
                .sorted(Comparator.comparing(Agendamento::getDataHora))
                .collect(Collectors.toList());
    }
}
