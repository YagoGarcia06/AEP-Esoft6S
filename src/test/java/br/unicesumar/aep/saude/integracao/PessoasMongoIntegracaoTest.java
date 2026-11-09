package br.unicesumar.aep.saude.integracao;

import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.RegistroClinico;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.repository.mongo.MongoPacienteRepository;
import br.unicesumar.aep.saude.repository.mongo.MongoProfissionalRepository;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Coleções "pacientes" e "profissionais" no MongoDB real. */
class PessoasMongoIntegracaoTest extends MongoIntegracaoBase {

    private MongoPacienteRepository pacientes;
    private MongoProfissionalRepository profissionais;

    @Override
    protected void prepararRepositorios(MongoDatabase database) {
        pacientes = new MongoPacienteRepository(database);
        profissionais = new MongoProfissionalRepository(database);
    }

    @Test
    @DisplayName("faz o CRUD completo de pacientes")
    void deveFazerCrudDePaciente() {
        Paciente maria = pacientes.salvar(Fixtures.maria());
        pacientes.salvar(Fixtures.joao());

        assertEquals(2, pacientes.contar());
        assertEquals("Maria Souza", pacientes.buscarPorId(maria.getId()).orElseThrow().getNome());
        assertEquals(maria.getId(), pacientes.buscarPorCpf("529.982.247-25").orElseThrow().getId());

        maria.setNome("Maria Souza Lima");
        pacientes.salvar(maria);
        assertEquals("Maria Souza Lima", pacientes.buscarPorId(maria.getId()).orElseThrow().getNome());
        assertEquals(2, pacientes.contar());

        assertTrue(pacientes.remover(maria.getId()));
        assertFalse(pacientes.remover(maria.getId()));
        assertTrue(pacientes.buscarPorId(maria.getId()).isEmpty());
        assertEquals(1, pacientes.listarTodos().size());
    }

    @Test
    @DisplayName("grava o histórico clínico como lista de subdocumentos")
    void deveGravarHistoricoComoSubdocumentos() {
        Paciente maria = Fixtures.maria();
        maria.adicionarRegistro(new RegistroClinico(LocalDate.of(2026, 3, 9), TipoAtendimento.CONSULTA,
                "prof-1", "Consulta de rotina", List.of("Losartana 50mg", "Retorno em 30 dias")));
        pacientes.salvar(maria);

        Document bruto = conexao().getDatabase().getCollection("pacientes")
                .find(new Document("_id", maria.getId())).first();
        List<Document> historico = bruto.getList("historico", Document.class);
        assertEquals(1, historico.size());
        assertEquals(List.of("Losartana 50mg", "Retorno em 30 dias"),
                historico.get(0).getList("prescricoes", String.class));
        assertEquals("Maringá", bruto.get("endereco", Document.class).getString("cidade"));

        Paciente lida = pacientes.buscarPorId(maria.getId()).orElseThrow();
        assertEquals("Consulta de rotina", lida.getHistorico().get(0).getDescricao());
    }

    @Test
    @DisplayName("o índice único impede dois pacientes com o mesmo CPF")
    void indiceUnicoDeCpf() {
        pacientes.salvar(Fixtures.maria());

        MongoWriteException erro = assertThrows(MongoWriteException.class,
                () -> pacientes.salvar(Fixtures.maria()));
        assertEquals(11000, erro.getCode());
    }

    @Test
    @DisplayName("grava profissionais com especialidades e janelas de atendimento embutidas")
    void deveGravarProfissional() {
        Profissional carlos = profissionais.salvar(Fixtures.carlos());
        profissionais.salvar(Fixtures.ana());

        Profissional lido = profissionais.buscarPorId(carlos.getId()).orElseThrow();
        assertEquals(List.of("Cardiologia"), lido.getEspecialidades());
        assertEquals(DayOfWeek.MONDAY, lido.getDisponibilidades().get(0).getDiaSemana());
        assertEquals("CRM-PR 12345", lido.getRegistroProfissional());

        assertEquals(1, profissionais.buscarPorEspecialidade("cardiologia").size());
        assertEquals(1, profissionais.buscarPorEspecialidade("CLÍNICA GERAL").size());
        assertTrue(profissionais.buscarPorEspecialidade("Cardio").isEmpty());
    }
}
