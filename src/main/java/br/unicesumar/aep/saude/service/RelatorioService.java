package br.unicesumar.aep.saude.service;

import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.model.ItemAgendaDia;
import br.unicesumar.aep.saude.model.ResumoAtendimentos;
import br.unicesumar.aep.saude.repository.RelatorioRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Relatórios gerenciais da clínica: agenda do dia e totais de atendimentos,
 * faltas e cancelamentos por profissional e por tipo de atendimento.
 */
public class RelatorioService {

    /** Limite para evitar consultas acidentais sobre anos de dados. */
    static final long PERIODO_MAXIMO_DIAS = 366;

    private final RelatorioRepository repository;

    public RelatorioService(RelatorioRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<ItemAgendaDia> agendaDoDia(LocalDate data) {
        Objects.requireNonNull(data, "Data é obrigatória.");
        return repository.agendaDoDia(data);
    }

    public List<ResumoAtendimentos> resumoPorProfissional(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        return repository.resumoPorProfissional(inicio, fim);
    }

    public List<ResumoAtendimentos> resumoPorTipo(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        return repository.resumoPorTipo(inicio, fim);
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        Objects.requireNonNull(inicio, "Data inicial é obrigatória.");
        Objects.requireNonNull(fim, "Data final é obrigatória.");
        if (fim.isBefore(inicio)) {
            throw new RegraNegocioException("A data final não pode ser anterior à inicial.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) >= PERIODO_MAXIMO_DIAS) {
            throw new RegraNegocioException("O período máximo do relatório é de um ano.");
        }
    }
}
