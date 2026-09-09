package br.unicesumar.aep.saude.mapper;

import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Endereco;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.RegistroClinico;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacienteMapperTest {

    private PacienteMapper mapper;
    private Paciente maria;

    @BeforeEach
    void preparar() {
        mapper = new PacienteMapper();
        maria = new Paciente("pac-1", "Maria Souza", "529.982.247-25",
                LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A, 100", "Maringá", "PR"));
    }

    @Test
    @DisplayName("grava contato e endereço como documentos aninhados")
    void deveAninharObjetosDeValor() {
        Document documento = mapper.paraDocumento(maria);

        assertEquals("pac-1", documento.getString("_id"));
        assertEquals("52998224725", documento.getString("cpf"));
        assertEquals("1990-04-12", documento.getString("dataNascimento"));
        assertEquals("maria@email.com",
                documento.get("contato", Document.class).getString("email"));
        assertEquals("PR", documento.get("endereco", Document.class).getString("estado"));
        assertTrue(documento.getList("historico", Document.class).isEmpty());
    }

    @Test
    @DisplayName("grava o histórico como lista de subdocumentos")
    void deveGravarHistoricoComoLista() {
        maria.adicionarRegistro(new RegistroClinico(LocalDate.of(2026, 3, 9),
                TipoAtendimento.CONSULTA, "prof-1", "Consulta de rotina",
                List.of("Losartana 50mg")));

        List<Document> historico = mapper.paraDocumento(maria).getList("historico", Document.class);

        assertEquals(1, historico.size());
        assertEquals("2026-03-09", historico.get(0).getString("data"));
        assertEquals("CONSULTA", historico.get(0).getString("tipo"));
        assertEquals(List.of("Losartana 50mg"),
                historico.get(0).getList("prescricoes", String.class));
    }

    @Test
    @DisplayName("recupera o paciente completo na ida e volta")
    void devePreservarNaIdaEVolta() {
        maria.adicionarRegistro(new RegistroClinico(LocalDate.of(2026, 1, 10),
                TipoAtendimento.EXAME, "prof-2", "Hemograma completo", null));
        maria.adicionarRegistro(new RegistroClinico(LocalDate.of(2026, 3, 9),
                TipoAtendimento.CONSULTA, "prof-1", "Consulta de rotina",
                List.of("Losartana 50mg")));

        Paciente recuperada = mapper.paraEntidade(mapper.paraDocumento(maria));

        assertEquals(maria.getId(), recuperada.getId());
        assertEquals(maria.getNome(), recuperada.getNome());
        assertEquals(maria.getCpf(), recuperada.getCpf());
        assertEquals(maria.getDataNascimento(), recuperada.getDataNascimento());
        assertEquals(maria.getContato(), recuperada.getContato());
        assertEquals(maria.getEndereco(), recuperada.getEndereco());
        assertEquals(2, recuperada.getHistorico().size());
        assertEquals("Hemograma completo", recuperada.getHistorico().get(0).getDescricao());
        assertEquals(TipoAtendimento.CONSULTA, recuperada.getHistorico().get(1).getTipo());
        assertEquals(List.of("Losartana 50mg"), recuperada.getHistorico().get(1).getPrescricoes());
        assertTrue(recuperada.getHistorico().get(0).getPrescricoes().isEmpty());
    }

    @Test
    @DisplayName("aceita documento sem o campo histórico")
    void deveAceitarDocumentoSemHistorico() {
        Document documento = mapper.paraDocumento(maria);
        documento.remove("historico");

        Paciente recuperada = mapper.paraEntidade(documento);

        assertEquals("Maria Souza", recuperada.getNome());
        assertTrue(recuperada.getHistorico().isEmpty());
    }
}
