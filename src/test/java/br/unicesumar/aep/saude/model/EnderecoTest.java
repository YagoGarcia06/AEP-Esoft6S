package br.unicesumar.aep.saude.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnderecoTest {

    @Test
    @DisplayName("normaliza UF para maiúsculo")
    void deveNormalizarUf() {
        Endereco endereco = new Endereco("Rua A, 100", "Maringá", "pr");
        assertEquals("PR", endereco.getEstado());
        assertEquals("Maringá", endereco.getCidade());
        assertEquals("Rua A, 100", endereco.getRua());
    }

    @Test
    @DisplayName("rejeita UF com tamanho diferente de dois")
    void deveRejeitarUfInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new Endereco("Rua A", "Maringá", "Paraná"));
    }

    @Test
    @DisplayName("rejeita rua e cidade em branco")
    void deveRejeitarCamposObrigatorios() {
        assertThrows(IllegalArgumentException.class, () -> new Endereco("  ", "Maringá", "PR"));
        assertThrows(IllegalArgumentException.class, () -> new Endereco("Rua A", "  ", "PR"));
    }

    @Test
    @DisplayName("compara endereços por valor")
    void deveCompararPorValor() {
        Endereco a = new Endereco("Rua A", "Maringá", "PR");
        Endereco b = new Endereco("Rua A", "Maringá", "pr");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals("Rua A, Maringá - PR", a.toString());
    }
}
