package br.unicesumar.aep.saude.repository.mongo;

import br.unicesumar.aep.saude.mapper.AgendamentoMapper;
import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.repository.AgendamentoRepository;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;

import java.time.LocalDate;
import java.util.List;

public class MongoAgendamentoRepository extends MongoRepository<Agendamento>
        implements AgendamentoRepository {

    public static final String COLECAO = "agendamentos";

    public MongoAgendamentoRepository(MongoDatabase database) {
        super(database, COLECAO, new AgendamentoMapper());
        colecao.createIndex(Indexes.ascending("profissionalId", "data"));
        colecao.createIndex(Indexes.ascending("pacienteId"));
    }

    @Override
    protected String idDe(Agendamento entidade) {
        return entidade.getId();
    }

    @Override
    public List<Agendamento> buscarPorProfissionalEData(String profissionalId, LocalDate data) {
        if (profissionalId == null || data == null) {
            return List.of();
        }
        return converter(colecao
                .find(Filters.and(
                        Filters.eq("profissionalId", profissionalId),
                        Filters.eq("data", data.toString())))
                .sort(Sorts.ascending("dataHora")));
    }

    @Override
    public List<Agendamento> buscarPorPaciente(String pacienteId) {
        if (pacienteId == null) {
            return List.of();
        }
        return converter(colecao
                .find(Filters.eq("pacienteId", pacienteId))
                .sort(Sorts.ascending("dataHora")));
    }
}
