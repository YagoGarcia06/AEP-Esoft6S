package br.unicesumar.aep.saude.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContatoTest {

    @Test
    @DisplayName("normaliza e-mail para minúsculo e remove espaços")
    void deveNormalizarDados() {
        Contato contato = new Contato("  Maria@Email.COM ", " 44 90000-0000 ");
        assertEquals("maria@email.com", contato.getEmail());
        assertEquals("44 90000-0000", contato.getTelefone());
    }

    @Test
    @DisplayName("rejeita e-mail sem arroba")
    void deveRejeitarEmailInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Contato("maria.email.com", "44 90000-0000"));
    }

    @Test
    @DisplayName("rejeita e-mail nulo")
    void deveRejeitarEmailNulo() {
        assertThrows(IllegalArgumentException.class, () -> new Contato(null, "44 90000-0000"));
    }

    @Test
    @DisplayName("rejeita telefone em branco")
    void deveRejeitarTelefoneEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> new Contato("maria@email.com", "   "));
    }

    @Test
    @DisplayName("compara contatos por valor")
    void deveCompararPorValor() {
        Contato a = new Contato("maria@email.com", "44 90000-0000");
        Contato b = new Contato("MARIA@email.com", "44 90000-0000");
        Contato c = new Contato("joao@email.com", "44 90000-0000");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
        assertTrue(a.toString().contains("maria@email.com"));
    }
}
