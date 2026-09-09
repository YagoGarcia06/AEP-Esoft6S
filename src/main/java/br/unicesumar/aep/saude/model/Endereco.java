package br.unicesumar.aep.saude.model;

import java.util.Objects;

/**
 * Objeto de valor imutável com o endereço do paciente.
 * Será mapeado como documento aninhado no MongoDB.
 */
public final class Endereco {

    private final String rua;
    private final String cidade;
    private final String estado;

    public Endereco(String rua, String cidade, String estado) {
        if (rua == null || rua.isBlank()) {
            throw new IllegalArgumentException("Rua é obrigatória.");
        }
        if (cidade == null || cidade.isBlank()) {
            throw new IllegalArgumentException("Cidade é obrigatória.");
        }
        if (estado == null || estado.trim().length() != 2) {
            throw new IllegalArgumentException("Estado deve ter 2 letras (UF): " + estado);
        }
        this.rua = rua.trim();
        this.cidade = cidade.trim();
        this.estado = estado.trim().toUpperCase();
    }

    public String getRua() {
        return rua;
    }

    public String getCidade() {
        return cidade;
    }

    public String getEstado() {
        return estado;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Endereco)) {
            return false;
        }
        Endereco outro = (Endereco) obj;
        return rua.equals(outro.rua) && cidade.equals(outro.cidade) && estado.equals(outro.estado);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rua, cidade, estado);
    }

    @Override
    public String toString() {
        return rua + ", " + cidade + " - " + estado;
    }
}
