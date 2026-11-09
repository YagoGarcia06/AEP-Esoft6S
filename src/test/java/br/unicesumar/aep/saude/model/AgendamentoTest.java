package br.unicesumar.aep.saude.model;

import br.unicesumar.aep.saude.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendamentoTest {

    private static final LocalDateTime SEGUNDA_9H = LocalDateTime.of(2026, 3, 9, 9, 0);

    private Agendamento consulta(LocalDateTime dataHora) {
        return new Agendamento("pac-1", "prof-1", dataHora, TipoAtendimento.CONSULTA);
    }

    @Test
    @DisplayName("usa a duração padrão do tipo de atendimento")
    void deveUsarDuracaoPadrao() {
        Agendamento consulta = consulta(SEGUNDA_9H);
        assertNotNull(consulta.getId());
        assertEquals(30, consulta.getDuracaoMinutos());
        assertEquals(LocalDateTime.of(2026, 3, 9, 9, 30), consulta.getFim());
        assertEquals(LocalDate.of(2026, 3, 9), consulta.getData());
        assertEquals(StatusAgendamento.AGENDADO, consulta.getStatus());

        Agendamento exame = new Agendamento("pac-1", "prof-1", SEGUNDA_9H, TipoAtendimento.EXAME);
        assertEquals(45, exame.getDuracaoMinutos());
    }

    @Test
    @DisplayName("rejeita dados obrigatórios ausentes")
    void deveRejeitarDadosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new Agendamento("  ", "prof-1", SEGUNDA_9H, TipoAtendimento.CONSULTA));
        assertThrows(IllegalArgumentException.class,
                () -> new Agendamento("pac-1", null, SEGUNDA_9H, TipoAtendimento.CONSULTA));
        assertThrows(NullPointerException.class,
                () -> new Agendamento("pac-1", "prof-1", SEGUNDA_9H, null));
        assertThrows(NullPointerException.class,
                () -> new Agendamento("pac-1", "prof-1", null, TipoAtendimento.CONSULTA));
        assertThrows(IllegalArgumentException.class,
                () -> new Agendamento(null, "pac-1", "prof-1", SEGUNDA_9H, 0,
                        TipoAtendimento.CONSULTA, StatusAgendamento.AGENDADO));
    }

    @Test
    @DisplayName("detecta sobreposição na agenda do mesmo profissional")
    void deveDetectarConflito() {
        Agendamento existente = consulta(SEGUNDA_9H);
        Agendamento sobreposto = consulta(SEGUNDA_9H.plusMinutes(15));
        assertTrue(sobreposto.conflitaCom(existente));
        assertTrue(existente.conflitaCom(sobreposto));
    }

    @Test
    @DisplayName("não considera conflito horários apenas encostados")
    void naoDeveConflitarHorarioAdjacente() {
        Agendamento existente = consulta(SEGUNDA_9H);
        Agendamento seguinte = consulta(SEGUNDA_9H.plusMinutes(30));
        assertFalse(seguinte.conflitaCom(existente));
    }

    @Test
    @DisplayName("não considera conflito entre profissionais diferentes")
    void naoDeveConflitarOutroProfissional() {
        Agendamento existente = consulta(SEGUNDA_9H);
        Agendamento outroProfissional =
                new Agendamento("pac-2", "prof-2", SEGUNDA_9H, TipoAtendimento.CONSULTA);
        assertFalse(outroProfissional.conflitaCom(existente));
    }

    @Test
    @DisplayName("não considera conflito com agendamento cancelado ou consigo mesmo")
    void naoDeveConflitarComInativoOuConsigoMesmo() {
        Agendamento existente = consulta(SEGUNDA_9H);
        Agendamento novo = consulta(SEGUNDA_9H.plusMinutes(10));
        existente.cancelar();

        assertFalse(novo.conflitaCom(existente));
        assertFalse(novo.conflitaCom(novo));
        assertFalse(novo.conflitaCom(null));
    }

    @Test
    @DisplayName("conclui um atendimento agendado")
    void deveRealizar() {
        Agendamento consulta = consulta(SEGUNDA_9H);
        consulta.realizar();
        assertEquals(StatusAgendamento.REALIZADO, consulta.getStatus());
    }

    @Test
    @DisplayName("cancela um atendimento agendado")
    void deveCancelar() {
        Agendamento consulta = consulta(SEGUNDA_9H);
        consulta.cancelar();
        assertEquals(StatusAgendamento.CANCELADO, consulta.getStatus());
    }

    @Test
    @DisplayName("bloqueia transições a partir de um status final")
    void deveBloquearTransicaoInvalida() {
        Agendamento realizada = consulta(SEGUNDA_9H);
        realizada.realizar();
        assertThrows(RegraNegocioException.class, realizada::cancelar);
        assertThrows(RegraNegocioException.class, realizada::realizar);

        Agendamento cancelada = consulta(SEGUNDA_9H);
        cancelada.cancelar();
        assertThrows(RegraNegocioException.class, cancelada::realizar);
    }

    @Test
    @DisplayName("descreve as regras de transição do status")
    void deveDescreverTransicoes() {
        assertTrue(StatusAgendamento.AGENDADO.isAtivo());
        assertFalse(StatusAgendamento.REALIZADO.isAtivo());
        assertTrue(StatusAgendamento.AGENDADO.permiteTransicaoPara(StatusAgendamento.CANCELADO));
        assertFalse(StatusAgendamento.AGENDADO.permiteTransicaoPara(StatusAgendamento.AGENDADO));
        assertFalse(StatusAgendamento.CANCELADO.permiteTransicaoPara(StatusAgendamento.REALIZADO));
        assertEquals("Consulta", TipoAtendimento.CONSULTA.getDescricao());
        assertEquals(45, TipoAtendimento.EXAME.getDuracaoPadraoMinutos());
    }

    @Test
    @DisplayName("compara agendamentos por id")
    void deveCompararPorId() {
        Agendamento consulta = consulta(SEGUNDA_9H);
        assertTrue(consulta.equals(consulta));
        assertFalse(consulta.equals(consulta(SEGUNDA_9H)));
        assertFalse(consulta.equals("texto"));
        assertEquals(consulta.getId().hashCode(), consulta.hashCode());
        assertTrue(consulta.toString().contains("Agendado"));
        assertEquals("pac-1", consulta.getPacienteId());
        assertEquals("prof-1", consulta.getProfissionalId());
        assertEquals(SEGUNDA_9H, consulta.getDataHora());
        assertEquals(TipoAtendimento.CONSULTA, consulta.getTipo());
    }

    @Test
    @DisplayName("confirma e depois conclui um atendimento")
    void deveConfirmarEConcluir() {
        Agendamento consulta = consulta(SEGUNDA_9H);
        consulta.confirmar();
        assertEquals(StatusAgendamento.CONFIRMADO, consulta.getStatus());
        assertTrue(consulta.getStatus().isAtivo());

        consulta.realizar();
        assertEquals(StatusAgendamento.REALIZADO, consulta.getStatus());
    }

    @Test
    @DisplayName("não confirma duas vezes nem confirma atendimento encerrado")
    void deveBloquearConfirmacaoInvalida() {
        Agendamento confirmada = consulta(SEGUNDA_9H);
        confirmada.confirmar();
        assertThrows(RegraNegocioException.class, confirmada::confirmar);

        Agendamento cancelada = consulta(SEGUNDA_9H);
        cancelada.cancelar();
        assertThrows(RegraNegocioException.class, cancelada::confirmar);
    }

    @Test
    @DisplayName("registra falta e trata a falta como status final")
    void deveRegistrarFalta() {
        Agendamento consulta = consulta(SEGUNDA_9H);
        consulta.confirmar();
        consulta.registrarFalta();

        assertEquals(StatusAgendamento.NAO_COMPARECEU, consulta.getStatus());
        assertFalse(consulta.getStatus().isAtivo());
        assertThrows(RegraNegocioException.class, consulta::realizar);
        assertThrows(RegraNegocioException.class, consulta::cancelar);
    }

    @Test
    @DisplayName("atendimento com falta não ocupa mais o horário")
    void faltaNaoGeraConflito() {
        Agendamento faltou = consulta(SEGUNDA_9H);
        faltou.registrarFalta();
        assertFalse(consulta(SEGUNDA_9H).conflitaCom(faltou));
    }

    @Test
    @DisplayName("remarca um atendimento confirmado e volta para AGENDADO")
    void deveRemarcar() {
        Agendamento consulta = consulta(SEGUNDA_9H);
        consulta.confirmar();

        consulta.remarcarPara(SEGUNDA_9H.plusDays(7));

        assertEquals(SEGUNDA_9H.plusDays(7), consulta.getDataHora());
        assertEquals(LocalDate.of(2026, 3, 16), consulta.getData());
        assertEquals(StatusAgendamento.AGENDADO, consulta.getStatus());
    }

    @Test
    @DisplayName("bloqueia remarcação inválida")
    void deveBloquearRemarcacaoInvalida() {
        Agendamento consulta = consulta(SEGUNDA_9H);
        assertThrows(NullPointerException.class, () -> consulta.remarcarPara(null));
        assertThrows(RegraNegocioException.class, () -> consulta.remarcarPara(SEGUNDA_9H));

        consulta.realizar();
        assertThrows(RegraNegocioException.class, () -> consulta.remarcarPara(SEGUNDA_9H.plusDays(1)));
        assertEquals(SEGUNDA_9H, consulta.getDataHora());
    }

    @Test
    @DisplayName("descreve todas as transições do ciclo de vida")
    void deveDescreverCicloCompleto() {
        assertTrue(StatusAgendamento.AGENDADO.permiteTransicaoPara(StatusAgendamento.CONFIRMADO));
        assertTrue(StatusAgendamento.AGENDADO.permiteTransicaoPara(StatusAgendamento.NAO_COMPARECEU));
        assertTrue(StatusAgendamento.CONFIRMADO.permiteTransicaoPara(StatusAgendamento.REALIZADO));
        assertTrue(StatusAgendamento.CONFIRMADO.permiteTransicaoPara(StatusAgendamento.CANCELADO));
        assertFalse(StatusAgendamento.CONFIRMADO.permiteTransicaoPara(StatusAgendamento.AGENDADO));
        assertFalse(StatusAgendamento.CONFIRMADO.permiteTransicaoPara(StatusAgendamento.CONFIRMADO));
        assertFalse(StatusAgendamento.NAO_COMPARECEU.permiteTransicaoPara(StatusAgendamento.CANCELADO));
        assertFalse(StatusAgendamento.REALIZADO.permiteTransicaoPara(StatusAgendamento.NAO_COMPARECEU));
        assertEquals("Não compareceu", StatusAgendamento.NAO_COMPARECEU.getDescricao());
        assertEquals("Confirmado", StatusAgendamento.CONFIRMADO.toString());
    }
}
