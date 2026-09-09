package br.unicesumar.aep.saude.repository;

import br.unicesumar.aep.saude.model.Profissional;

import java.util.List;

public interface ProfissionalRepository extends Repository<Profissional> {

    List<Profissional> buscarPorEspecialidade(String especialidade);
}
