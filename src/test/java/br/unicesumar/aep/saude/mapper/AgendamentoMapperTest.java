package br.unicesumar.aep.saude.mapper;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendamentoMapperTest {

    private static final LocalDateTime SEGUNDA_9H = LocalDateTime.of(2026, 3, 9, 9, 0);
    private static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 3, 2, 10, 0);

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
        exame.realizar(MOMENTO);

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
        consulta.cancelar(MOMENTO, "Paciente desistiu");

        assertEquals(StatusAgendamento.CANCELADO,
                mapper.paraEntidade(mapper.paraDocumento(consulta)).getStatus());
    }

    @Test
    @DisplayName("grava e recupera as alterações como lista de subdocumentos")
    void devePreservarAlteracoes() {
        Agendamento consulta =
                new Agendamento("pac-1", "prof-1", SEGUNDA_9H, TipoAtendimento.CONSULTA);
        consulta.confirmar(MOMENTO);
        consulta.cancelar(MOMENTO.plusHours(1), "Paciente desistiu");

        Document documento = mapper.paraDocumento(consulta);
        List<Document> alteracoes = documento.getList("alteracoes", Document.class);
        assertEquals(2, alteracoes.size());
        assertEquals("CONFIRMADO", alteracoes.get(0).getString("statusNovo"));
        assertFalse(alteracoes.get(0).containsKey("observacao"));
        assertEquals("Motivo: Paciente desistiu", alteracoes.get(1).getString("observacao"));

        Agendamento recuperado = mapper.paraEntidade(documento);
        assertEquals(2, recuperado.getAlteracoes().size());
        assertEquals(MOMENTO.plusHours(1), recuperado.getAlteracoes().get(1).getMomento());
        assertEquals(StatusAgendamento.CONFIRMADO, recuperado.getAlteracoes().get(1).getStatusAnterior());
    }

    @Test
    @DisplayName("lê documentos antigos, sem a lista de alterações")
    void deveLerDocumentoSemAlteracoes() {
        Document antigo = mapper.paraDocumento(
                new Agendamento("pac-1", "prof-1", SEGUNDA_9H, TipoAtendimento.CONSULTA));
        antigo.remove("alteracoes");

        assertTrue(mapper.paraEntidade(antigo).getAlteracoes().isEmpty());
    }
}
