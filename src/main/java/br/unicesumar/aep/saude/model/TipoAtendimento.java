package br.unicesumar.aep.saude.model;

/**
 * Tipos de atendimento suportados pela PoC, cada um com sua duração padrão.
 */
public enum TipoAtendimento {

    CONSULTA("Consulta", 30),
    EXAME("Exame", 45);

    private final String descricao;
    private final int duracaoPadraoMinutos;

    TipoAtendimento(String descricao, int duracaoPadraoMinutos) {
        this.descricao = descricao;
        this.duracaoPadraoMinutos = duracaoPadraoMinutos;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getDuracaoPadraoMinutos() {
        return duracaoPadraoMinutos;
    }
}
