package br.unicesumar.aep.saude.repository.mongo;

import br.unicesumar.aep.saude.model.FiltroAgenda;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import com.mongodb.MongoClientSettings;
import org.bson.BsonDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifica a tradução do filtro da agenda para uma consulta do MongoDB. */
class MongoAgendamentoConsultaTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 3, 9);

    private static BsonDocument renderizar(FiltroAgenda filtro) {
        return MongoAgendamentoRepository.paraConsulta(filtro)
                .toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
    }

    @Test
    @DisplayName("filtro vazio vira consulta vazia")
    void filtroVazio() {
        assertTrue(renderizar(FiltroAgenda.TODOS).isEmpty());
    }

    @Test
    @DisplayName("combina profissional, período e status em um $and")
    void deveCombinarCriterios() {
        String consulta = renderizar(new FiltroAgenda(SEGUNDA, SEGUNDA.plusDays(6), "prof-1",
                StatusAgendamento.NAO_COMPARECEU)).toJson();

        assertTrue(consulta.contains("\"profissionalId\": \"prof-1\""));
        assertTrue(consulta.contains("\"$gte\": \"2026-03-09\""));
        assertTrue(consulta.contains("\"$lte\": \"2026-03-15\""));
        assertTrue(consulta.contains("\"status\": \"NAO_COMPARECEU\""));
    }

    @Test
    @DisplayName("usa só os critérios preenchidos")
    void soCriteriosPreenchidos() {
        String consulta = renderizar(new FiltroAgenda(null, null, null, StatusAgendamento.CANCELADO)).toJson();

        assertTrue(consulta.contains("\"status\": \"CANCELADO\""));
        assertFalse(consulta.contains("\"data\""));
        assertFalse(consulta.contains("profissionalId"));
    }
}
