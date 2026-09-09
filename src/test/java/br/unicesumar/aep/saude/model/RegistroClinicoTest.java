package br.unicesumar.aep.saude.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroClinicoTest {

    @Test
    @DisplayName("cria registro com prescrições em lista imutável")
    void deveCriarRegistroValido() {
        List<String> prescricoes = new ArrayList<>();
        prescricoes.add("Losartana 50mg");

        RegistroClinico registro = new RegistroClinico(LocalDate.of(2026, 3, 9),
                TipoAtendimento.CONSULTA, "prof-1", "  Consulta de rotina  ", prescricoes);

        prescricoes.add("Alterado depois");

        assertEquals("Consulta de rotina", registro.getDescricao());
        assertEquals(1, registro.getPrescricoes().size());
        assertEquals(TipoAtendimento.CONSULTA, registro.getTipo());
        assertEquals("prof-1", registro.getProfissionalId());
        assertEquals(LocalDate.of(2026, 3, 9), registro.getData());
        assertTrue(registro.toString().contains("Consulta"));
        assertThrows(UnsupportedOperationException.class,
                () -> registro.getPrescricoes().add("Nova"));
    }

    @Test
    @DisplayName("aceita registro sem prescrições")
    void deveAceitarSemPrescricoes() {
        RegistroClinico registro = new RegistroClinico(LocalDate.of(2026, 3, 9),
                TipoAtendimento.EXAME, "prof-1", "Hemograma completo", null);
        assertTrue(registro.getPrescricoes().isEmpty());
    }

    @Test
    @DisplayName("rejeita campos obrigatórios ausentes")
    void deveRejeitarCamposInvalidos() {
        assertThrows(NullPointerException.class, () -> new RegistroClinico(
                null, TipoAtendimento.EXAME, "prof-1", "Hemograma", null));
        assertThrows(NullPointerException.class, () -> new RegistroClinico(
                LocalDate.of(2026, 3, 9), null, "prof-1", "Hemograma", null));
        assertThrows(IllegalArgumentException.class, () -> new RegistroClinico(
                LocalDate.of(2026, 3, 9), TipoAtendimento.EXAME, "  ", "Hemograma", null));
        assertThrows(IllegalArgumentException.class, () -> new RegistroClinico(
                LocalDate.of(2026, 3, 9), TipoAtendimento.EXAME, "prof-1", "  ", null));
    }
}
