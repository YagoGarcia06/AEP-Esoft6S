package br.unicesumar.aep.saude.ui;

import br.unicesumar.aep.saude.model.Pessoa;

/**
 * Item leve para as caixas de seleção: guarda o id e exibe o nome.
 */
public class ItemPessoa {

    /** Opção "Todos" dos filtros: não tem id. */
    public static final ItemPessoa TODOS = new ItemPessoa(null, "Todos");

    private final String id;
    private final String nome;

    public ItemPessoa(Pessoa pessoa) {
        this(pessoa.getId(), pessoa.getNome());
    }

    private ItemPessoa(String id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return nome;
    }
}
