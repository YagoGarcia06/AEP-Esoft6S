package br.unicesumar.aep.saude.repository.mongo;

import br.unicesumar.aep.saude.mapper.PacienteMapper;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.repository.PacienteRepository;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import java.util.Optional;

public class MongoPacienteRepository extends MongoRepository<Paciente>
        implements PacienteRepository {

    public static final String COLECAO = "pacientes";

    public MongoPacienteRepository(MongoDatabase database) {
        super(database, COLECAO, new PacienteMapper());
        colecao.createIndex(Indexes.ascending("cpf"), new IndexOptions().unique(true));
    }

    @Override
    protected String idDe(Paciente entidade) {
        return entidade.getId();
    }

    @Override
    public Optional<Paciente> buscarPorCpf(String cpf) {
        if (cpf == null) {
            return Optional.empty();
        }
        return primeiro(Filters.eq("cpf", cpf.replaceAll("\\D", "")));
    }
}
