package br.unicesumar.aep.saude.ui;

import br.unicesumar.aep.saude.model.Pessoa;

/**
 * Item leve para as caixas de seleção: guarda o id e exibe o nome.
 */
public class ItemPessoa {

    private final String id;
    private final String nome;

    public ItemPessoa(Pessoa pessoa) {
        this.id = pessoa.getId();
        this.nome = pessoa.getNome();
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return nome;
    }
}
