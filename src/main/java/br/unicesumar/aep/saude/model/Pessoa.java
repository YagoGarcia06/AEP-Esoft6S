package br.unicesumar.aep.saude.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Superclasse abstrata das pessoas do domínio. Concentra identidade, nome,
 * CPF e contato, deixando para as subclasses o que é específico de cada papel.
 */
public abstract class Pessoa {

    private final String id;
    private final String cpf;
    private String nome;
    private Contato contato;

    protected Pessoa(String id, String nome, String cpf, Contato contato) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        this.cpf = normalizarCpf(cpf);
        setNome(nome);
        setContato(contato);
    }

    private static String normalizarCpf(String cpf) {
        if (cpf == null) {
            throw new IllegalArgumentException("CPF é obrigatório.");
        }
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            throw new IllegalArgumentException("CPF deve conter 11 dígitos: " + cpf);
        }
        if (digitos.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("CPF inválido: " + cpf);
        }
        return digitos;
    }

    /** Rótulo do papel da pessoa no domínio. Implementado por cada subclasse. */
    public abstract String tipo();

    public String descricaoResumida() {
        return tipo() + " " + nome + " (CPF " + getCpfFormatado() + ")";
    }

    public final void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public final void setContato(Contato contato) {
        this.contato = Objects.requireNonNull(contato, "Contato é obrigatório.");
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getCpfFormatado() {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "."
                + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    public Contato getContato() {
        return contato;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Pessoa)) {
            return false;
        }
        return id.equals(((Pessoa) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return descricaoResumida();
    }
}
