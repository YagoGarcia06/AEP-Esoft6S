package br.unicesumar.aep.saude.service;

import br.unicesumar.aep.saude.exception.RecursoNaoEncontradoException;
import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.model.Disponibilidade;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.repository.ProfissionalRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Regras de cadastro e consulta de profissionais de saúde.
 */
public class ProfissionalService {

    private final ProfissionalRepository repository;

    public ProfissionalService(ProfissionalRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repositório de profissionais é obrigatório.");
    }

    public Profissional cadastrar(Profissional profissional) {
        Objects.requireNonNull(profissional, "Profissional é obrigatório.");
        boolean registroEmUso = repository.listarTodos().stream()
                .anyMatch(p -> p.getRegistroProfissional().equals(profissional.getRegistroProfissional()));
        if (registroEmUso) {
            throw new RegraNegocioException(
                    "Já existe profissional com o registro " + profissional.getRegistroProfissional() + ".");
        }
        return repository.salvar(profissional);
    }

    public Profissional buscarPorId(String id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrado: " + id));
    }

    public List<Profissional> listar() {
        return repository.listarTodos().stream()
                .sorted(Comparator.comparing(Profissional::getNome))
                .collect(Collectors.toList());
    }

    public List<Profissional> buscarPorEspecialidade(String especialidade) {
        return repository.buscarPorEspecialidade(especialidade);
    }

    public Profissional adicionarDisponibilidade(String id, Disponibilidade disponibilidade) {
        Profissional profissional = buscarPorId(id);
        profissional.adicionarDisponibilidade(disponibilidade);
        return repository.salvar(profissional);
    }

    public Profissional adicionarEspecialidade(String id, String especialidade) {
        Profissional profissional = buscarPorId(id);
        profissional.adicionarEspecialidade(especialidade);
        return repository.salvar(profissional);
    }

    public boolean remover(String id) {
        return repository.remover(id);
    }
}
