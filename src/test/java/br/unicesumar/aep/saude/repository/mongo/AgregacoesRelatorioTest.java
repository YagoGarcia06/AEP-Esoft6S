package br.unicesumar.aep.saude.repository.mongo;

import br.unicesumar.aep.saude.model.ItemAgendaDia;
import br.unicesumar.aep.saude.model.ResumoAtendimentos;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import com.mongodb.MongoClientSettings;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica os estágios dos pipelines e a conversão dos resultados sem precisar
 * de um MongoDB rodando. A execução contra o banco real fica nos testes de
 * integração.
 */
class AgregacoesRelatorioTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 3, 9);

    private static List<BsonDocument> renderizar(List<Bson> pipeline) {
        return pipeline.stream()
                .map(estagio -> estagio.toBsonDocument(BsonDocument.class,
                        MongoClientSettings.getDefaultCodecRegistry()))
                .toList();
    }

    private static String operador(BsonDocument estagio) {
        return estagio.getFirstKey();
    }

    @Test
    @DisplayName("agenda do dia: filtra o dia e junta pacientes e profissionais com $lookup")
    void pipelineAgendaDoDia() {
        List<BsonDocument> estagios = renderizar(AgregacoesRelatorio.agendaDoDia(SEGUNDA));

        assertEquals(List.of("$match", "$lookup", "$unwind", "$lookup", "$unwind", "$sort", "$project"),
                estagios.stream().map(AgregacoesRelatorioTest::operador).toList());
        assertEquals("2026-03-09",
                estagios.get(0).getDocument("$match").getString("data").getValue());

        BsonDocument lookupPaciente = estagios.get(1).getDocument("$lookup");
        assertEquals("pacientes", lookupPaciente.getString("from").getValue());
        assertEquals("pacienteId", lookupPaciente.getString("localField").getValue());
        assertEquals("_id", lookupPaciente.getString("foreignField").getValue());
        assertEquals("profissionais",
                estagios.get(3).getDocument("$lookup").getString("from").getValue());
        assertTrue(estagios.get(2).getDocument("$unwind")
                .getBoolean("preserveNullAndEmptyArrays").getValue());
        assertEquals("$paciente.nome",
                estagios.get(6).getDocument("$project").getString("pacienteNome").getValue());
    }

    @Test
    @DisplayName("resumo por profissional: período inclusivo, $group com contagem por status e $lookup do nome")
    void pipelineResumoPorProfissional() {
        List<BsonDocument> estagios = renderizar(
                AgregacoesRelatorio.resumoPorProfissional(SEGUNDA, SEGUNDA.plusDays(6)));

        assertEquals(List.of("$match", "$group", "$lookup", "$unwind", "$project", "$sort"),
                estagios.stream().map(AgregacoesRelatorioTest::operador).toList());

        String filtro = estagios.get(0).toJson();
        assertTrue(filtro.contains("\"$gte\": \"2026-03-09\""));
        assertTrue(filtro.contains("\"$lte\": \"2026-03-15\""));

        BsonDocument grupo = estagios.get(1).getDocument("$group");
        assertEquals("$profissionalId", grupo.getString("_id").getValue());
        assertTrue(grupo.toJson().contains("NAO_COMPARECEU"));
        assertTrue(grupo.containsKey("realizados"));
        assertTrue(grupo.containsKey("cancelados"));
        assertEquals("profissionais",
                estagios.get(2).getDocument("$lookup").getString("from").getValue());
    }

    @Test
    @DisplayName("resumo por tipo: agrupa pelo campo tipo e ordena pelo total")
    void pipelineResumoPorTipo() {
        List<BsonDocument> estagios = renderizar(AgregacoesRelatorio.resumoPorTipo(SEGUNDA, SEGUNDA));

        assertEquals(List.of("$match", "$group", "$sort"),
                estagios.stream().map(AgregacoesRelatorioTest::operador).toList());
        assertEquals("$tipo", estagios.get(1).getDocument("$group").getString("_id").getValue());
        assertEquals(-1, estagios.get(2).getDocument("$sort").getInt32("total").getValue());
    }

    @Test
    @DisplayName("converte o documento da agenda, inclusive sem nome de profissional")
    void deveConverterItemAgenda() {
        ItemAgendaDia item = AgregacoesRelatorio.paraItemAgenda(new Document("dataHora", "2026-03-09T09:00")
                .append("tipo", "EXAME")
                .append("status", "CONFIRMADO")
                .append("pacienteNome", "Maria Souza"));

        assertEquals(LocalDateTime.of(2026, 3, 9, 9, 0), item.getDataHora());
        assertEquals(TipoAtendimento.EXAME, item.getTipo());
        assertEquals(StatusAgendamento.CONFIRMADO, item.getStatus());
        assertEquals("Maria Souza", item.getPacienteNome());
        assertEquals(ItemAgendaDia.REMOVIDO, item.getProfissionalNome());
    }

    @Test
    @DisplayName("converte os resumos aceitando contagens Integer ou Long")
    void deveConverterResumos() {
        ResumoAtendimentos porProfissional = AgregacoesRelatorio.paraResumoPorProfissional(
                new Document("grupo", "Dr. Carlos Lima").append("total", 4)
                        .append("realizados", 2L).append("faltas", 1).append("cancelados", 1));
        ResumoAtendimentos porTipo = AgregacoesRelatorio.paraResumoPorTipo(
                new Document("_id", "CONSULTA").append("total", 3L).append("realizados", 3));

        assertEquals("Dr. Carlos Lima", porProfissional.getGrupo());
        assertEquals(4, porProfissional.getTotal());
        assertEquals(2, porProfissional.getRealizados());
        assertEquals(1, porProfissional.getFaltas());
        assertEquals("Consulta", porTipo.getGrupo());
        assertEquals(3, porTipo.getRealizados());
        assertEquals(0, porTipo.getFaltas());
    }
}
