package br.unicesumar.aep.saude.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entrada do histórico clínico do paciente. É um subdocumento da lista
 * "historico" dentro da coleção "pacientes".
 */
public final class RegistroClinico {

    private final LocalDate data;
    private final TipoAtendimento tipo;
    private final String profissionalId;
    private final String descricao;
    private final List<String> prescricoes;

    public RegistroClinico(LocalDate data, TipoAtendimento tipo, String profissionalId,
                           String descricao, List<String> prescricoes) {
        this.data = Objects.requireNonNull(data, "Data do registro é obrigatória.");
        this.tipo = Objects.requireNonNull(tipo, "Tipo de atendimento é obrigatório.");
        if (profissionalId == null || profissionalId.isBlank()) {
            throw new IllegalArgumentException("Id do profissional é obrigatório no registro clínico.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição do registro clínico é obrigatória.");
        }
        this.profissionalId = profissionalId;
        this.descricao = descricao.trim();
        this.prescricoes = prescricoes == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(prescricoes));
    }

    public LocalDate getData() {
        return data;
    }

    public TipoAtendimento getTipo() {
        return tipo;
    }

    public String getProfissionalId() {
        return profissionalId;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<String> getPrescricoes() {
        return prescricoes;
    }

    @Override
    public String toString() {
        return data + " [" + tipo.getDescricao() + "] " + descricao;
    }
}
