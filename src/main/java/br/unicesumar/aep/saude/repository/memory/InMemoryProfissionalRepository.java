package br.unicesumar.aep.saude.repository.memory;

import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.repository.ProfissionalRepository;

import java.util.List;
import java.util.stream.Collectors;

public class InMemoryProfissionalRepository extends InMemoryRepository<Profissional>
        implements ProfissionalRepository {

    @Override
    protected String idDe(Profissional entidade) {
        return entidade.getId();
    }

    @Override
    public List<Profissional> buscarPorEspecialidade(String especialidade) {
        return dados.values().stream()
                .filter(p -> p.possuiEspecialidade(especialidade))
                .collect(Collectors.toList());
    }
}
