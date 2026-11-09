package br.unicesumar.aep.saude.model;

import br.unicesumar.aep.saude.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltroAgendaTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 3, 9);

    private Agendamento consulta(String profissionalId, LocalDateTime dataHora, StatusAgendamento status) {
        return new Agendamento(null, "pac-1", profissionalId, dataHora, 30, TipoAtendimento.CONSULTA, status);
    }

    @Test
    @DisplayName("filtro vazio aceita qualquer agendamento")
    void filtroVazio() {
        assertTrue(FiltroAgenda.TODOS.isVazio());
        assertTrue(FiltroAgenda.TODOS.aceita(consulta("prof-1", SEGUNDA.atTime(9, 0), StatusAgendamento.CANCELADO)));
        assertTrue(new FiltroAgenda(null, null, " ", null).isVazio());
    }

    @Test
    @DisplayName("aplica período inclusivo, profissional e status juntos")
    void deveCombinarCriterios() {
        FiltroAgenda filtro = new FiltroAgenda(SEGUNDA, SEGUNDA.plusDays(6), "prof-1", StatusAgendamento.CONFIRMADO);

        assertFalse(filtro.isVazio());
        assertTrue(filtro.aceita(consulta("prof-1", SEGUNDA.atTime(8, 0), StatusAgendamento.CONFIRMADO)));
        assertTrue(filtro.aceita(consulta("prof-1", SEGUNDA.plusDays(6).atTime(17, 0), StatusAgendamento.CONFIRMADO)));
        assertFalse(filtro.aceita(consulta("prof-1", SEGUNDA.minusDays(1).atTime(9, 0), StatusAgendamento.CONFIRMADO)));
        assertFalse(filtro.aceita(consulta("prof-1", SEGUNDA.plusDays(7).atTime(9, 0), StatusAgendamento.CONFIRMADO)));
        assertFalse(filtro.aceita(consulta("prof-2", SEGUNDA.atTime(9, 0), StatusAgendamento.CONFIRMADO)));
        assertFalse(filtro.aceita(consulta("prof-1", SEGUNDA.atTime(9, 0), StatusAgendamento.AGENDADO)));
    }

    @Test
    @DisplayName("aceita período aberto em uma das pontas")
    void periodoAberto() {
        FiltroAgenda aPartirDe = new FiltroAgenda(SEGUNDA, null, null, null);
        FiltroAgenda ate = new FiltroAgenda(null, SEGUNDA, null, null);

        assertTrue(aPartirDe.aceita(consulta("p", SEGUNDA.plusYears(1).atTime(9, 0), StatusAgendamento.AGENDADO)));
        assertFalse(aPartirDe.aceita(consulta("p", SEGUNDA.minusDays(1).atTime(9, 0), StatusAgendamento.AGENDADO)));
        assertTrue(ate.aceita(consulta("p", SEGUNDA.minusYears(1).atTime(9, 0), StatusAgendamento.AGENDADO)));
        assertEquals(SEGUNDA, aPartirDe.getInicio().orElseThrow());
        assertTrue(aPartirDe.getFim().isEmpty());
        assertTrue(aPartirDe.getProfissionalId().isEmpty());
        assertTrue(aPartirDe.getStatus().isEmpty());
    }

    @Test
    @DisplayName("rejeita período com fim antes do início")
    void deveRejeitarPeriodoInvertido() {
        assertThrows(RegraNegocioException.class,
                () -> new FiltroAgenda(SEGUNDA, SEGUNDA.minusDays(1), null, null));
    }
}
