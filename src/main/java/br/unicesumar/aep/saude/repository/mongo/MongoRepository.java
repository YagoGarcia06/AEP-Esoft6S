package br.unicesumar.aep.saude.repository.mongo;

import br.unicesumar.aep.saude.mapper.DocumentMapper;
import br.unicesumar.aep.saude.repository.Repository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Base das implementações MongoDB. Concentra o CRUD comum sobre uma coleção e
 * deixa para as subclasses apenas as consultas específicas de cada entidade.
 *
 * @param <T> tipo da entidade raiz gerenciada.
 */
public abstract class MongoRepository<T> implements Repository<T> {

    protected final MongoCollection<Document> colecao;
    protected final DocumentMapper<T> mapper;

    protected MongoRepository(MongoDatabase database, String nomeColecao, DocumentMapper<T> mapper) {
        Objects.requireNonNull(database, "Banco de dados é obrigatório.");
        this.colecao = database.getCollection(Objects.requireNonNull(nomeColecao));
        this.mapper = Objects.requireNonNull(mapper, "Mapper é obrigatório.");
    }

    protected abstract String idDe(T entidade);

    /** Grava a entidade inteira: insere quando nova, substitui quando já existe. */
    @Override
    public T salvar(T entidade) {
        Objects.requireNonNull(entidade, "Entidade é obrigatória.");
        colecao.replaceOne(Filters.eq("_id", idDe(entidade)),
                mapper.paraDocumento(entidade),
                new ReplaceOptions().upsert(true));
        return entidade;
    }

    @Override
    public Optional<T> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(colecao.find(Filters.eq("_id", id)).first())
                .map(mapper::paraEntidade);
    }

    @Override
    public List<T> listarTodos() {
        return converter(colecao.find());
    }

    @Override
    public boolean remover(String id) {
        if (id == null) {
            return false;
        }
        return colecao.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    @Override
    public long contar() {
        return colecao.countDocuments();
    }

    protected List<T> converter(Iterable<Document> documentos) {
        List<T> entidades = new ArrayList<>();
        for (Document documento : documentos) {
            entidades.add(mapper.paraEntidade(documento));
        }
        return entidades;
    }

    protected Optional<T> primeiro(Bson filtro) {
        return Optional.ofNullable(colecao.find(filtro).first()).map(mapper::paraEntidade);
    }
}
