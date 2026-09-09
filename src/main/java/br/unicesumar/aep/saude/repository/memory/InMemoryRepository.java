package br.unicesumar.aep.saude.repository.memory;

import br.unicesumar.aep.saude.repository.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Base das implementações em memória. Concentra o CRUD comum e deixa para as
 * subclasses apenas a extração do id da entidade.
 *
 * @param <T> tipo da entidade raiz gerenciada.
 */
public abstract class InMemoryRepository<T> implements Repository<T> {

    protected final Map<String, T> dados = new LinkedHashMap<>();

    protected abstract String idDe(T entidade);

    @Override
    public T salvar(T entidade) {
        Objects.requireNonNull(entidade, "Entidade é obrigatória.");
        dados.put(idDe(entidade), entidade);
        return entidade;
    }

    @Override
    public Optional<T> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public List<T> listarTodos() {
        return new ArrayList<>(dados.values());
    }

    @Override
    public boolean remover(String id) {
        if (id == null) {
            return false;
        }
        return dados.remove(id) != null;
    }

    @Override
    public long contar() {
        return dados.size();
    }

    public void limpar() {
        dados.clear();
    }
}
