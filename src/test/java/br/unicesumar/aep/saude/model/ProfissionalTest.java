package br.unicesumar.aep.saude.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfissionalTest {

    private Profissional novoCarlos() {
        return new Profissional("Dr. Carlos Lima", "111.444.777-35", "crm-pr 12345",
                new Contato("carlos@clinica.com", "44 91111-1111"));
    }

    @Test
    @DisplayName("normaliza o registro profissional e identifica o tipo")
    void deveCriarProfissionalValido() {
        Profissional carlos = novoCarlos();
        assertEquals("CRM-PR 12345", carlos.getRegistroProfissional());
        assertEquals("Profissional", carlos.tipo());
        assertTrue(carlos.descricaoResumida().contains("CRM-PR 12345"));
    }

    @Test
    @DisplayName("rejeita registro profissional em branco")
    void deveRejeitarRegistroEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> new Profissional(
                "Dr. Carlos", "111.444.777-35", "  ",
                new Contato("carlos@clinica.com", "44 91111-1111")));
    }

    @Test
    @DisplayName("não duplica especialidades e ignora diferenças de caixa")
    void deveGerenciarEspecialidades() {
        Profissional carlos = novoCarlos();
        carlos.adicionarEspecialidade("Cardiologia");
        carlos.adicionarEspecialidade(" cardiologia ");
        carlos.adicionarEspecialidade("Clínica Geral");

        assertEquals(2, carlos.getEspecialidades().size());
        assertTrue(carlos.possuiEspecialidade("CARDIOLOGIA"));
        assertFalse(carlos.possuiEspecialidade("Pediatria"));
        assertFalse(carlos.possuiEspecialidade(null));
        assertThrows(IllegalArgumentException.class, () -> carlos.adicionarEspecialidade("  "));
    }

    @Test
    @DisplayName("não duplica janelas de disponibilidade iguais")
    void deveGerenciarDisponibilidades() {
        Profissional carlos = novoCarlos();
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

        assertEquals(2, carlos.getDisponibilidades().size());
        assertThrows(NullPointerException.class, () -> carlos.adicionarDisponibilidade(null));
    }

    @Test
    @DisplayName("responde se atende em um intervalo específico")
    void deveResponderAtendeEm() {
        Profissional carlos = novoCarlos();
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertTrue(carlos.atendeEm(
                LocalDateTime.of(2026, 3, 9, 9, 0), LocalDateTime.of(2026, 3, 9, 9, 30)));
        assertFalse(carlos.atendeEm(
                LocalDateTime.of(2026, 3, 9, 14, 0), LocalDateTime.of(2026, 3, 9, 14, 30)));
        assertFalse(carlos.atendeEm(
                LocalDateTime.of(2026, 3, 10, 9, 0), LocalDateTime.of(2026, 3, 10, 9, 30)));
    }

    @Test
    @DisplayName("protege as listas internas contra alteração externa")
    void deveProtegerListas() {
        Profissional carlos = novoCarlos();
        assertThrows(UnsupportedOperationException.class,
                () -> carlos.getEspecialidades().add("Pediatria"));
        assertThrows(UnsupportedOperationException.class,
                () -> carlos.getDisponibilidades().add(
                        new Disponibilidade(DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(9, 0))));
    }
}
