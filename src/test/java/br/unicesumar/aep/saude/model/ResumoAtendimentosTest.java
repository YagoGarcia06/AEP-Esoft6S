package br.unicesumar.aep.saude.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumoAtendimentosTest {

    @Test
    @DisplayName("calcula pendentes e percentual de faltas sobre os atendimentos ocorridos")
    void deveCalcularIndicadores() {
        ResumoAtendimentos resumo = new ResumoAtendimentos("Dr. Carlos Lima", 10, 6, 2, 1);

        assertEquals(1, resumo.getPendentes());
        assertEquals(25.0, resumo.getPercentualFaltas(), 0.001);
        assertEquals("Dr. Carlos Lima", resumo.getGrupo());
        assertEquals(10, resumo.getTotal());
        assertEquals(6, resumo.getRealizados());
        assertEquals(2, resumo.getFaltas());
        assertEquals(1, resumo.getCancelados());
        assertTrue(resumo.toString().contains("2 faltas"));
    }

    @Test
    @DisplayName("percentual de faltas é zero quando nada aconteceu ainda")
    void percentualZeroSemOcorridos() {
        assertEquals(0.0, new ResumoAtendimentos("Exame", 3, 0, 0, 1).getPercentualFaltas());
    }

    @Test
    @DisplayName("rejeita totais inconsistentes")
    void deveRejeitarTotaisInconsistentes() {
        assertThrows(NullPointerException.class, () -> new ResumoAtendimentos(null, 1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ResumoAtendimentos("X", -1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ResumoAtendimentos("X", 1, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ResumoAtendimentos("X", 2, 1, 1, 1));
    }

    @Test
    @DisplayName("item da agenda troca nome ausente por '(removido)'")
    void itemAgendaDeveTratarNomeAusente() {
        LocalDateTime nove = LocalDateTime.of(2026, 3, 9, 9, 0);
        ItemAgendaDia item = new ItemAgendaDia(nove, null, " ",
                TipoAtendimento.CONSULTA, StatusAgendamento.CONFIRMADO);

        assertEquals(ItemAgendaDia.REMOVIDO, item.getPacienteNome());
        assertEquals(ItemAgendaDia.REMOVIDO, item.getProfissionalNome());
        assertEquals(nove, item.getDataHora());
        assertEquals(TipoAtendimento.CONSULTA, item.getTipo());
        assertEquals(StatusAgendamento.CONFIRMADO, item.getStatus());
        assertEquals("09:00 Consulta - (removido) com (removido) (Confirmado)", item.toString());
        assertThrows(NullPointerException.class, () -> new ItemAgendaDia(
                null, "A", "B", TipoAtendimento.EXAME, StatusAgendamento.AGENDADO));
    }
}
