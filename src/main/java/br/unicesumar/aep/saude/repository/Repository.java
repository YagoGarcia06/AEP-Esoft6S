package br.unicesumar.aep.saude.repository;

import java.util.List;
import java.util.Optional;

/**
 * Contrato genérico de persistência. Hoje existe uma implementação em memória;
 * na próxima etapa entram as implementações MongoDB, sem que os serviços mudem.
 *
 * @param <T> tipo da entidade raiz gerenciada.
 */
public interface Repository<T> {

    T salvar(T entidade);

    Optional<T> buscarPorId(String id);

    List<T> listarTodos();

    boolean remover(String id);

    long contar();
}
