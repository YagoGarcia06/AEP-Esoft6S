package br.unicesumar.aep.saude.mapper;

import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Disponibilidade;
import br.unicesumar.aep.saude.model.Profissional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfissionalMapperTest {

    private ProfissionalMapper mapper;
    private Profissional carlos;

    @BeforeEach
    void preparar() {
        mapper = new ProfissionalMapper();
        carlos = new Profissional("prof-1", "Dr. Carlos Lima", "111.444.777-35",
                "CRM-PR 12345", new Contato("carlos@clinica.com", "44 91111-1111"));
        carlos.adicionarEspecialidade("Cardiologia");
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("grava especialidades e disponibilidades como listas")
    void deveGravarListas() {
        Document documento = mapper.paraDocumento(carlos);

        assertEquals("prof-1", documento.getString("_id"));
        assertEquals("CRM-PR 12345", documento.getString("registroProfissional"));
        assertEquals(List.of("Cardiologia"), documento.getList("especialidades", String.class));

        List<Document> disponibilidades = documento.getList("disponibilidades", Document.class);
        assertEquals(1, disponibilidades.size());
        assertEquals("MONDAY", disponibilidades.get(0).getString("diaSemana"));
        assertEquals("08:00", disponibilidades.get(0).getString("horaInicio"));
        assertEquals("12:00", disponibilidades.get(0).getString("horaFim"));
    }

    @Test
    @DisplayName("recupera o profissional completo na ida e volta")
    void devePreservarNaIdaEVolta() {
        carlos.adicionarEspecialidade("Clínica Geral");
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

        Profissional recuperado = mapper.paraEntidade(mapper.paraDocumento(carlos));

        assertEquals(carlos.getId(), recuperado.getId());
        assertEquals(carlos.getNome(), recuperado.getNome());
        assertEquals(carlos.getCpf(), recuperado.getCpf());
        assertEquals(carlos.getContato(), recuperado.getContato());
        assertEquals(2, recuperado.getEspecialidades().size());
        assertTrue(recuperado.possuiEspecialidade("cardiologia"));
        assertEquals(2, recuperado.getDisponibilidades().size());
        assertTrue(recuperado.atendeEm(
                LocalDateTime.of(2026, 3, 9, 9, 0), LocalDateTime.of(2026, 3, 9, 9, 30)));
    }

    @Test
    @DisplayName("aceita documento sem listas opcionais")
    void deveAceitarDocumentoSemListas() {
        Document documento = mapper.paraDocumento(carlos);
        documento.remove("especialidades");
        documento.remove("disponibilidades");

        Profissional recuperado = mapper.paraEntidade(documento);

        assertTrue(recuperado.getEspecialidades().isEmpty());
        assertTrue(recuperado.getDisponibilidades().isEmpty());
    }
}
