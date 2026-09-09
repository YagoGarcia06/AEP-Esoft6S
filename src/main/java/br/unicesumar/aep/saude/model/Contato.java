package br.unicesumar.aep.saude.model;

import java.util.Objects;

/**
 * Objeto de valor imutável com os dados de contato de uma pessoa.
 * Será mapeado como documento aninhado no MongoDB.
 */
public final class Contato {

    private final String email;
    private final String telefone;

    public Contato(String email, String telefone) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("E-mail inválido: " + email);
        }
        if (telefone == null || telefone.isBlank()) {
            throw new IllegalArgumentException("Telefone é obrigatório.");
        }
        this.email = email.trim().toLowerCase();
        this.telefone = telefone.trim();
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Contato)) {
            return false;
        }
        Contato outro = (Contato) obj;
        return email.equals(outro.email) && telefone.equals(outro.telefone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, telefone);
    }

    @Override
    public String toString() {
        return email + " / " + telefone;
    }
}
