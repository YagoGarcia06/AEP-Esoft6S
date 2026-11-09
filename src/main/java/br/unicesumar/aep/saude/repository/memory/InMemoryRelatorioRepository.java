package br.unicesumar.aep.saude.repository.memory;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.ItemAgendaDia;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.ResumoAtendimentos;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.repository.AgendamentoRepository;
import br.unicesumar.aep.saude.repository.PacienteRepository;
import br.unicesumar.aep.saude.repository.ProfissionalRepository;
import br.unicesumar.aep.saude.repository.RelatorioRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Equivalente em memória dos relatórios do MongoDB. Produz os mesmos resultados
 * que os pipelines de agregação, para os testes e para o modo sem banco.
 */
public class InMemoryRelatorioRepository implements RelatorioRepository {

    private final AgendamentoRepository agendamentos;
    private final PacienteRepository pacientes;
    private final ProfissionalRepository profissionais;

    public InMemoryRelatorioRepository(AgendamentoRepository agendamentos,
                                       PacienteRepository pacientes,
                                       ProfissionalRepository profissionais) {
        this.agendamentos = Objects.requireNonNull(agendamentos);
        this.pacientes = Objects.requireNonNull(pacientes);
        this.profissionais = Objects.requireNonNull(profissionais);
    }

    @Override
    public List<ItemAgendaDia> agendaDoDia(LocalDate data) {
        return agendamentos.listarTodos().stream()
                .filter(a -> a.getData().equals(data))
                .sorted(Comparator.comparing(Agendamento::getDataHora))
                .map(a -> new ItemAgendaDia(a.getDataHora(),
                        nomePaciente(a.getPacienteId()),
                        nomeProfissional(a.getProfissionalId()),
                        a.getTipo(), a.getStatus()))
                .toList();
    }

    @Override
    public List<ResumoAtendimentos> resumoPorProfissional(LocalDate inicio, LocalDate fim) {
        return resumir(inicio, fim, a -> {
            String nome = nomeProfissional(a.getProfissionalId());
            return nome == null ? ItemAgendaDia.REMOVIDO : nome;
        });
    }

    @Override
    public List<ResumoAtendimentos> resumoPorTipo(LocalDate inicio, LocalDate fim) {
        return resumir(inicio, fim, a -> a.getTipo().getDescricao());
    }

    private List<ResumoAtendimentos> resumir(LocalDate inicio, LocalDate fim,
                                             Function<Agendamento, String> grupoDe) {
        Map<String, long[]> contagens = new LinkedHashMap<>();
        for (Agendamento agendamento : agendamentos.listarTodos()) {
            LocalDate dia = agendamento.getData();
            if (dia.isBefore(inicio) || dia.isAfter(fim)) {
                continue;
            }
            long[] c = contagens.computeIfAbsent(grupoDe.apply(agendamento), g -> new long[4]);
            c[0]++;
            StatusAgendamento status = agendamento.getStatus();
            if (status == StatusAgendamento.REALIZADO) {
                c[1]++;
            } else if (status == StatusAgendamento.NAO_COMPARECEU) {
                c[2]++;
            } else if (status == StatusAgendamento.CANCELADO) {
                c[3]++;
            }
        }
        List<ResumoAtendimentos> resumos = new ArrayList<>();
        contagens.forEach((grupo, c) -> resumos.add(new ResumoAtendimentos(grupo, c[0], c[1], c[2], c[3])));
        resumos.sort(Comparator.comparingLong(ResumoAtendimentos::getTotal).reversed()
                .thenComparing(ResumoAtendimentos::getGrupo));
        return resumos;
    }

    private String nomePaciente(String id) {
        return pacientes.buscarPorId(id).map(Paciente::getNome).orElse(null);
    }

    private String nomeProfissional(String id) {
        return profissionais.buscarPorId(id).map(Profissional::getNome).orElse(null);
    }
}
