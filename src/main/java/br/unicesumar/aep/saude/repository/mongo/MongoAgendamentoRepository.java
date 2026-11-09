package br.unicesumar.aep.saude.repository.mongo;

import br.unicesumar.aep.saude.mapper.AgendamentoMapper;
import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.FiltroAgenda;
import br.unicesumar.aep.saude.repository.AgendamentoRepository;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MongoAgendamentoRepository extends MongoRepository<Agendamento>
        implements AgendamentoRepository {

    public static final String COLECAO = "agendamentos";

    public MongoAgendamentoRepository(MongoDatabase database) {
        super(database, COLECAO, new AgendamentoMapper());
        colecao.createIndex(Indexes.ascending("profissionalId", "data"));
        colecao.createIndex(Indexes.ascending("pacienteId"));
        colecao.createIndex(Indexes.ascending("data", "status"));
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

    @Override
    public List<Agendamento> buscarComFiltro(FiltroAgenda filtro) {
        Objects.requireNonNull(filtro, "Filtro é obrigatório.");
        return converter(colecao.find(paraConsulta(filtro)).sort(Sorts.ascending("dataHora")));
    }

    /**
     * Traduz o filtro para uma consulta do MongoDB. Com profissional, usa o índice
     * (profissionalId, data); sem ele, o índice (data, status).
     */
    static Bson paraConsulta(FiltroAgenda filtro) {
        List<Bson> condicoes = new ArrayList<>();
        filtro.getProfissionalId().ifPresent(id -> condicoes.add(Filters.eq("profissionalId", id)));
        filtro.getInicio().ifPresent(inicio -> condicoes.add(Filters.gte("data", inicio.toString())));
        filtro.getFim().ifPresent(fim -> condicoes.add(Filters.lte("data", fim.toString())));
        filtro.getStatus().ifPresent(status -> condicoes.add(Filters.eq("status", status.name())));
        return condicoes.isEmpty() ? Filters.empty() : Filters.and(condicoes);
    }
}
