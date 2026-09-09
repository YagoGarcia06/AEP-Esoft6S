package br.unicesumar.aep.saude.repository.mongo;

import br.unicesumar.aep.saude.mapper.ProfissionalMapper;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.repository.ProfissionalRepository;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import java.util.List;
import java.util.regex.Pattern;

public class MongoProfissionalRepository extends MongoRepository<Profissional>
        implements ProfissionalRepository {

    public static final String COLECAO = "profissionais";

    public MongoProfissionalRepository(MongoDatabase database) {
        super(database, COLECAO, new ProfissionalMapper());
    }

    @Override
    protected String idDe(Profissional entidade) {
        return entidade.getId();
    }

    /**
     * Consulta direto no array de especialidades, ignorando diferenças de caixa
     * para acompanhar o comportamento de {@code Profissional.possuiEspecialidade}.
     */
    @Override
    public List<Profissional> buscarPorEspecialidade(String especialidade) {
        if (especialidade == null || especialidade.isBlank()) {
            return List.of();
        }
        String termo = "^" + Pattern.quote(especialidade.trim()) + "$";
        return converter(colecao.find(Filters.regex("especialidades", termo, "i")));
    }
}
