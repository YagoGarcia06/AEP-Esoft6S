package br.unicesumar.aep.saude.repository.mongo;

import br.unicesumar.aep.saude.model.ItemAgendaDia;
import br.unicesumar.aep.saude.model.ResumoAtendimentos;
import br.unicesumar.aep.saude.repository.RelatorioRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Relatórios executados no próprio MongoDB com o aggregation pipeline, a partir
 * da coleção "agendamentos" e cruzando com "pacientes" e "profissionais".
 */
public class MongoRelatorioRepository implements RelatorioRepository {

    private final MongoCollection<Document> agendamentos;

    public MongoRelatorioRepository(MongoDatabase database) {
        Objects.requireNonNull(database, "Banco de dados é obrigatório.");
        this.agendamentos = database.getCollection(MongoAgendamentoRepository.COLECAO);
    }

    @Override
    public List<ItemAgendaDia> agendaDoDia(LocalDate data) {
        return executar(AgregacoesRelatorio.agendaDoDia(data), AgregacoesRelatorio::paraItemAgenda);
    }

    @Override
    public List<ResumoAtendimentos> resumoPorProfissional(LocalDate inicio, LocalDate fim) {
        return executar(AgregacoesRelatorio.resumoPorProfissional(inicio, fim),
                AgregacoesRelatorio::paraResumoPorProfissional);
    }

    @Override
    public List<ResumoAtendimentos> resumoPorTipo(LocalDate inicio, LocalDate fim) {
        return executar(AgregacoesRelatorio.resumoPorTipo(inicio, fim),
                AgregacoesRelatorio::paraResumoPorTipo);
    }

    private <T> List<T> executar(List<? extends org.bson.conversions.Bson> pipeline,
                                 Function<Document, T> conversor) {
        List<T> resultado = new ArrayList<>();
        for (Document documento : agendamentos.aggregate(pipeline)) {
            resultado.add(conversor.apply(documento));
        }
        return resultado;
    }
}
