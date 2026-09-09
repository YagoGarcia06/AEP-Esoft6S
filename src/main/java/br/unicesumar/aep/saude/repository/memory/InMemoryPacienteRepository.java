package br.unicesumar.aep.saude.repository.memory;

import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.repository.PacienteRepository;

import java.util.Optional;

public class InMemoryPacienteRepository extends InMemoryRepository<Paciente>
        implements PacienteRepository {

    @Override
    protected String idDe(Paciente entidade) {
        return entidade.getId();
    }

    @Override
    public Optional<Paciente> buscarPorCpf(String cpf) {
        if (cpf == null) {
            return Optional.empty();
        }
        String digitos = cpf.replaceAll("\\D", "");
        return dados.values().stream()
                .filter(p -> p.getCpf().equals(digitos))
                .findFirst();
    }
}
