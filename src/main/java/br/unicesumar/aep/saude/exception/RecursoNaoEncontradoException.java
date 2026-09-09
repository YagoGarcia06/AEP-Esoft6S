package br.unicesumar.aep.saude.exception;

/**
 * Lançada quando uma entidade referenciada por id não existe no repositório.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
