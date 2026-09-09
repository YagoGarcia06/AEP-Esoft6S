package br.unicesumar.aep.saude.mapper;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgendamentoMapperTest {

    private static final LocalDateTime SEGUNDA_9H = LocalDateTime.of(2026, 3, 9, 9, 0);

    private final AgendamentoMapper mapper = new AgendamentoMapper();

    @Test
    @DisplayName("grava o campo derivado 'data' para consulta indexada por dia")
    void deveGravarCampoDerivadoData() {
        Agendamento consulta =
                new Agendamento("pac-1", "prof-1", SEGUNDA_9H, TipoAtendimento.CONSULTA);

        Document documento = mapper.paraDocumento(consulta);

        assertEquals("2026-03-09T09:00", documento.getString("dataHora"));
        assertEquals("2026-03-09", documento.getString("data"));
        assertEquals("pac-1", documento.getString("pacienteId"));
        assertEquals("prof-1", documento.getString("profissionalId"));
        assertEquals(30, documento.getInteger("duracaoMinutos").intValue());
        assertEquals("CONSULTA", documento.getString("tipo"));
        assertEquals("AGENDADO", documento.getString("status"));
    }

    @Test
    @DisplayName("recupera o agendamento completo na ida e volta")
    void devePreservarNaIdaEVolta() {
        Agendamento exame = new Agendamento("ag-1", "pac-1", "prof-1", SEGUNDA_9H,
                45, TipoAtendimento.EXAME, StatusAgendamento.AGENDADO);
        exame.realizar();

        Agendamento recuperado = mapper.paraEntidade(mapper.paraDocumento(exame));

        assertEquals("ag-1", recuperado.getId());
        assertEquals("pac-1", recuperado.getPacienteId());
        assertEquals("prof-1", recuperado.getProfissionalId());
        assertEquals(SEGUNDA_9H, recuperado.getDataHora());
        assertEquals(45, recuperado.getDuracaoMinutos());
        assertEquals(TipoAtendimento.EXAME, recuperado.getTipo());
        assertEquals(StatusAgendamento.REALIZADO, recuperado.getStatus());
        assertEquals(LocalDateTime.of(2026, 3, 9, 9, 45), recuperado.getFim());
    }

    @Test
    @DisplayName("preserva o status cancelado")
    void devePreservarCancelado() {
        Agendamento consulta =
                new Agendamento("pac-1", "prof-1", SEGUNDA_9H, TipoAtendimento.CONSULTA);
        consulta.cancelar();

        assertEquals(StatusAgendamento.CANCELADO,
                mapper.paraEntidade(mapper.paraDocumento(consulta)).getStatus());
    }
}
