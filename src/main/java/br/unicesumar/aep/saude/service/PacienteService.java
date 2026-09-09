package br.unicesumar.aep.saude.service;

import br.unicesumar.aep.saude.exception.RecursoNaoEncontradoException;
import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.RegistroClinico;
import br.unicesumar.aep.saude.repository.PacienteRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Regras de cadastro e consulta de pacientes.
 */
public class PacienteService {

    private final PacienteRepository repository;

    public PacienteService(PacienteRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repositório de pacientes é obrigatório.");
    }

    public Paciente cadastrar(Paciente paciente) {
        Objects.requireNonNull(paciente, "Paciente é obrigatório.");
        repository.buscarPorCpf(paciente.getCpf()).ifPresent(existente -> {
            throw new RegraNegocioException(
                    "Já existe paciente cadastrado com o CPF " + paciente.getCpfFormatado() + ".");
        });
        return repository.salvar(paciente);
    }

    public Paciente buscarPorId(String id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente não encontrado: " + id));
    }

    public List<Paciente> listar() {
        return repository.listarTodos().stream()
                .sorted(Comparator.comparing(Paciente::getNome))
                .collect(Collectors.toList());
    }

    public Paciente atualizarContato(String id, Contato novoContato) {
        Paciente paciente = buscarPorId(id);
        paciente.setContato(novoContato);
        return repository.salvar(paciente);
    }

    public boolean remover(String id) {
        return repository.remover(id);
    }

    /** Histórico clínico do paciente, do atendimento mais recente para o mais antigo. */
    public List<RegistroClinico> historico(String pacienteId) {
        return buscarPorId(pacienteId).getHistorico().stream()
                .sorted(Comparator.comparing(RegistroClinico::getData).reversed())
                .collect(Collectors.toList());
    }
}
