package br.unicesumar.aep.saude.repository;

import br.unicesumar.aep.saude.model.Paciente;

import java.util.Optional;

public interface PacienteRepository extends Repository<Paciente> {

    Optional<Paciente> buscarPorCpf(String cpf);
}
