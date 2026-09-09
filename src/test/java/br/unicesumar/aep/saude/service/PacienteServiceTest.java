package br.unicesumar.aep.saude.service;

import br.unicesumar.aep.saude.exception.RecursoNaoEncontradoException;
import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Endereco;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.RegistroClinico;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.repository.memory.InMemoryPacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacienteServiceTest {

    private InMemoryPacienteRepository repository;
    private PacienteService service;

    @BeforeEach
    void preparar() {
        repository = new InMemoryPacienteRepository();
        service = new PacienteService(repository);
    }

    private Paciente maria() {
        return new Paciente("Maria Souza", "529.982.247-25", LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR"));
    }

    private Paciente joao() {
        return new Paciente("Ana Lima", "111.444.777-35", LocalDate.of(1985, 1, 20),
                new Contato("ana@email.com", "44 93333-3333"),
                new Endereco("Rua B", "Curitiba", "PR"));
    }

    @Test
    @DisplayName("exige repositório na construção")
    void deveExigirRepositorio() {
        assertThrows(NullPointerException.class, () -> new PacienteService(null));
    }

    @Test
    @DisplayName("cadastra um novo paciente")
    void deveCadastrar() {
        Paciente salvo = service.cadastrar(maria());
        assertEquals(1, repository.contar());
        assertEquals("Maria Souza", salvo.getNome());
        assertThrows(NullPointerException.class, () -> service.cadastrar(null));
    }

    @Test
    @DisplayName("bloqueia cadastro com CPF já existente")
    void deveBloquearCpfDuplicado() {
        service.cadastrar(maria());
        RegraNegocioException erro =
                assertThrows(RegraNegocioException.class, () -> service.cadastrar(maria()));
        assertTrue(erro.getMessage().contains("529.982.247-25"));
        assertEquals(1, repository.contar());
    }

    @Test
    @DisplayName("busca paciente por id e falha quando não existe")
    void deveBuscarPorId() {
        Paciente salvo = service.cadastrar(maria());
        assertEquals(salvo.getId(), service.buscarPorId(salvo.getId()).getId());
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorId("inexistente"));
    }

    @Test
    @DisplayName("lista pacientes em ordem alfabética")
    void deveListarOrdenado() {
        service.cadastrar(maria());
        service.cadastrar(joao());

        List<Paciente> lista = service.listar();
        assertEquals(2, lista.size());
        assertEquals("Ana Lima", lista.get(0).getNome());
        assertEquals("Maria Souza", lista.get(1).getNome());
    }

    @Test
    @DisplayName("atualiza o contato do paciente")
    void deveAtualizarContato() {
        Paciente salvo = service.cadastrar(maria());
        Paciente atualizado =
                service.atualizarContato(salvo.getId(), new Contato("novo@email.com", "44 94444-4444"));

        assertEquals("novo@email.com", atualizado.getContato().getEmail());
        assertEquals("novo@email.com",
                repository.buscarPorId(salvo.getId()).orElseThrow().getContato().getEmail());
    }

    @Test
    @DisplayName("remove um paciente existente")
    void deveRemover() {
        Paciente salvo = service.cadastrar(maria());
        assertTrue(service.remover(salvo.getId()));
        assertFalse(service.remover(salvo.getId()));
    }

    @Test
    @DisplayName("devolve o histórico do mais recente para o mais antigo")
    void deveOrdenarHistorico() {
        Paciente salvo = service.cadastrar(maria());
        salvo.adicionarRegistro(new RegistroClinico(LocalDate.of(2026, 1, 10),
                TipoAtendimento.CONSULTA, "prof-1", "Primeira consulta", null));
        salvo.adicionarRegistro(new RegistroClinico(LocalDate.of(2026, 3, 9),
                TipoAtendimento.EXAME, "prof-1", "Hemograma", null));

        List<RegistroClinico> historico = service.historico(salvo.getId());

        assertEquals(2, historico.size());
        assertEquals(LocalDate.of(2026, 3, 9), historico.get(0).getData());
        assertEquals(LocalDate.of(2026, 1, 10), historico.get(1).getData());
    }
}
