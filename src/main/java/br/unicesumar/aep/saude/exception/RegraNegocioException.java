package br.unicesumar.aep.saude.exception;

/**
 * Lançada quando uma regra de negócio da PoC é violada
 * (horário indisponível, conflito de agenda, CPF duplicado etc.).
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
