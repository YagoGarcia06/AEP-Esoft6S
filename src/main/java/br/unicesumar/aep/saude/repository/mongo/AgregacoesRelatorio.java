package br.unicesumar.aep.saude.repository.mongo;

import br.unicesumar.aep.saude.model.ItemAgendaDia;
import br.unicesumar.aep.saude.model.ResumoAtendimentos;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UnwindOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pipelines de agregação dos relatórios e a conversão dos documentos que eles
 * devolvem. Fica separado do repositório para que os estágios possam ser
 * testados sem um banco rodando.
 *
 * <p>O campo derivado {@code data} (yyyy-MM-dd) permite filtrar por dia e por
 * período com comparação de texto, que respeita a ordem cronológica.</p>
 */
final class AgregacoesRelatorio {

    private AgregacoesRelatorio() {
    }

    /**
     * $match no dia → $lookup do paciente e do profissional → $unwind (mantendo
     * agendamentos cujo paciente/profissional foi removido) → $sort → $project.
     */
    static List<Bson> agendaDoDia(LocalDate data) {
        return List.of(
                Aggregates.match(Filters.eq("data", data.toString())),
                Aggregates.lookup(MongoPacienteRepository.COLECAO, "pacienteId", "_id", "paciente"),
                Aggregates.unwind("$paciente", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                Aggregates.lookup(MongoProfissionalRepository.COLECAO, "profissionalId", "_id", "profissional"),
                Aggregates.unwind("$profissional", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                Aggregates.sort(Sorts.ascending("dataHora")),
                Aggregates.project(new Document("_id", 0)
                        .append("dataHora", 1)
                        .append("tipo", 1)
                        .append("status", 1)
                        .append("pacienteNome", "$paciente.nome")
                        .append("profissionalNome", "$profissional.nome")));
    }

    /**
     * $match no período → $group por profissional contando cada status →
     * $lookup do nome do profissional → $project → $sort.
     */
    static List<Bson> resumoPorProfissional(LocalDate inicio, LocalDate fim) {
        return List.of(
                filtroPeriodo(inicio, fim),
                agrupar("$profissionalId"),
                Aggregates.lookup(MongoProfissionalRepository.COLECAO, "_id", "_id", "profissional"),
                Aggregates.unwind("$profissional", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                Aggregates.project(new Document("_id", 0)
                        .append("grupo", new Document("$ifNull",
                                List.of("$profissional.nome", ItemAgendaDia.REMOVIDO)))
                        .append("total", 1)
                        .append("realizados", 1)
                        .append("faltas", 1)
                        .append("cancelados", 1)),
                Aggregates.sort(Sorts.orderBy(Sorts.descending("total"), Sorts.ascending("grupo"))));
    }

    /** $match no período → $group por tipo contando cada status → $sort. */
    static List<Bson> resumoPorTipo(LocalDate inicio, LocalDate fim) {
        return List.of(
                filtroPeriodo(inicio, fim),
                agrupar("$tipo"),
                Aggregates.sort(Sorts.orderBy(Sorts.descending("total"), Sorts.ascending("_id"))));
    }

    private static Bson filtroPeriodo(LocalDate inicio, LocalDate fim) {
        return Aggregates.match(Filters.and(
                Filters.gte("data", inicio.toString()),
                Filters.lte("data", fim.toString())));
    }

    private static Bson agrupar(String campo) {
        return Aggregates.group(campo,
                Accumulators.sum("total", 1),
                Accumulators.sum("realizados", contarStatus(StatusAgendamento.REALIZADO)),
                Accumulators.sum("faltas", contarStatus(StatusAgendamento.NAO_COMPARECEU)),
                Accumulators.sum("cancelados", contarStatus(StatusAgendamento.CANCELADO)));
    }

    /** {@code {$cond: [{$eq: ["$status", "X"]}, 1, 0]}} */
    private static Document contarStatus(StatusAgendamento status) {
        return new Document("$cond", List.of(
                new Document("$eq", List.of("$status", status.name())), 1, 0));
    }

    static ItemAgendaDia paraItemAgenda(Document documento) {
        return new ItemAgendaDia(
                LocalDateTime.parse(documento.getString("dataHora")),
                documento.getString("pacienteNome"),
                documento.getString("profissionalNome"),
                TipoAtendimento.valueOf(documento.getString("tipo")),
                StatusAgendamento.valueOf(documento.getString("status")));
    }

    static ResumoAtendimentos paraResumoPorProfissional(Document documento) {
        return paraResumo(documento.getString("grupo"), documento);
    }

    static ResumoAtendimentos paraResumoPorTipo(Document documento) {
        return paraResumo(TipoAtendimento.valueOf(documento.getString("_id")).getDescricao(), documento);
    }

    private static ResumoAtendimentos paraResumo(String grupo, Document documento) {
        return new ResumoAtendimentos(grupo,
                numero(documento, "total"),
                numero(documento, "realizados"),
                numero(documento, "faltas"),
                numero(documento, "cancelados"));
    }

    /** O $sum pode devolver Integer ou Long conforme o volume; trata os dois. */
    private static long numero(Document documento, String campo) {
        Number valor = documento.get(campo, Number.class);
        return valor == null ? 0 : valor.longValue();
    }
}
