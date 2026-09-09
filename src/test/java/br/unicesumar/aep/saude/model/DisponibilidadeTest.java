package br.unicesumar.aep.saude.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisponibilidadeTest {

    private final Disponibilidade segundaManha =
            new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

    @Test
    @DisplayName("cobre intervalo dentro da janela")
    void deveCobrirIntervaloInterno() {
        assertTrue(segundaManha.cobre(
                LocalDateTime.of(2026, 3, 9, 9, 0),
                LocalDateTime.of(2026, 3, 9, 9, 30)));
    }

    @Test
    @DisplayName("cobre intervalo que encosta nos limites da janela")
    void deveCobrirLimites() {
        assertTrue(segundaManha.cobre(
                LocalDateTime.of(2026, 3, 9, 8, 0),
                LocalDateTime.of(2026, 3, 9, 12, 0)));
    }

    @Test
    @DisplayName("não cobre outro dia da semana")
    void naoDeveCobrirOutroDia() {
        assertFalse(segundaManha.cobre(
                LocalDateTime.of(2026, 3, 10, 9, 0),
                LocalDateTime.of(2026, 3, 10, 9, 30)));
    }

    @Test
    @DisplayName("não cobre intervalo que ultrapassa o fim da janela")
    void naoDeveCobrirAlemDoFim() {
        assertFalse(segundaManha.cobre(
                LocalDateTime.of(2026, 3, 9, 11, 45),
                LocalDateTime.of(2026, 3, 9, 12, 15)));
    }

    @Test
    @DisplayName("não cobre intervalo que começa antes do início da janela")
    void naoDeveCobrirAntesDoInicio() {
        assertFalse(segundaManha.cobre(
                LocalDateTime.of(2026, 3, 9, 7, 30),
                LocalDateTime.of(2026, 3, 9, 8, 30)));
    }

    @Test
    @DisplayName("não cobre intervalo nulo ou que vira o dia")
    void naoDeveCobrirCasosDegenerados() {
        assertFalse(segundaManha.cobre(null, LocalDateTime.of(2026, 3, 9, 9, 0)));
        assertFalse(segundaManha.cobre(LocalDateTime.of(2026, 3, 9, 9, 0), null));
        assertFalse(segundaManha.cobre(
                LocalDateTime.of(2026, 3, 9, 23, 50),
                LocalDateTime.of(2026, 3, 10, 0, 20)));
    }

    @Test
    @DisplayName("rejeita janela com fim anterior ou igual ao início")
    void deveRejeitarJanelaInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(8, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(8, 0)));
        assertThrows(NullPointerException.class,
                () -> new Disponibilidade(null, LocalTime.of(8, 0), LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("compara janelas por valor")
    void deveCompararPorValor() {
        Disponibilidade igual =
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        assertTrue(segundaManha.equals(igual));
        assertTrue(segundaManha.hashCode() == igual.hashCode());
        assertTrue(segundaManha.toString().contains("MONDAY"));
    }
}
