package br.unicesumar.aep.saude.service;

import br.unicesumar.aep.saude.exception.RecursoNaoEncontradoException;
import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Disponibilidade;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.repository.memory.InMemoryProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfissionalServiceTest {

    private InMemoryProfissionalRepository repository;
    private ProfissionalService service;

    @BeforeEach
    void preparar() {
        repository = new InMemoryProfissionalRepository();
        service = new ProfissionalService(repository);
    }

    private Profissional carlos() {
        return new Profissional("Dr. Carlos Lima", "111.444.777-35", "CRM-PR 12345",
                new Contato("carlos@clinica.com", "44 91111-1111"));
    }

    private Profissional ana() {
        return new Profissional("Dra. Ana Prado", "529.982.247-25", "CRM-PR 54321",
                new Contato("ana@clinica.com", "44 92222-2222"));
    }

    @Test
    @DisplayName("exige repositório na construção")
    void deveExigirRepositorio() {
        assertThrows(NullPointerException.class, () -> new ProfissionalService(null));
    }

    @Test
    @DisplayName("cadastra um novo profissional")
    void deveCadastrar() {
        Profissional salvo = service.cadastrar(carlos());
        assertEquals(1, repository.contar());
        assertEquals("CRM-PR 12345", salvo.getRegistroProfissional());
        assertThrows(NullPointerException.class, () -> service.cadastrar(null));
    }

    @Test
    @DisplayName("bloqueia registro profissional duplicado")
    void deveBloquearRegistroDuplicado() {
        service.cadastrar(carlos());
        RegraNegocioException erro =
                assertThrows(RegraNegocioException.class, () -> service.cadastrar(carlos()));
        assertTrue(erro.getMessage().contains("CRM-PR 12345"));
        assertEquals(1, repository.contar());
    }

    @Test
    @DisplayName("busca por id e falha quando não existe")
    void deveBuscarPorId() {
        Profissional salvo = service.cadastrar(carlos());
        assertEquals(salvo.getId(), service.buscarPorId(salvo.getId()).getId());
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorId("inexistente"));
    }

    @Test
    @DisplayName("lista profissionais em ordem alfabética")
    void deveListarOrdenado() {
        service.cadastrar(carlos());
        service.cadastrar(ana());

        List<Profissional> lista = service.listar();
        assertEquals(2, lista.size());
        assertEquals("Dr. Carlos Lima", lista.get(0).getNome());
        assertEquals("Dra. Ana Prado", lista.get(1).getNome());
    }

    @Test
    @DisplayName("adiciona especialidade e permite buscar por ela")
    void deveAdicionarEspecialidade() {
        Profissional salvo = service.cadastrar(carlos());
        service.adicionarEspecialidade(salvo.getId(), "Cardiologia");

        assertTrue(service.buscarPorId(salvo.getId()).possuiEspecialidade("cardiologia"));
        assertEquals(1, service.buscarPorEspecialidade("Cardiologia").size());
        assertTrue(service.buscarPorEspecialidade("Pediatria").isEmpty());
    }

    @Test
    @DisplayName("adiciona janela de disponibilidade")
    void deveAdicionarDisponibilidade() {
        Profissional salvo = service.cadastrar(carlos());
        service.adicionarDisponibilidade(salvo.getId(),
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertEquals(1, service.buscarPorId(salvo.getId()).getDisponibilidades().size());
        assertThrows(RecursoNaoEncontradoException.class, () -> service.adicionarDisponibilidade(
                "inexistente", new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0))));
    }

    @Test
    @DisplayName("remove um profissional existente")
    void deveRemover() {
        Profissional salvo = service.cadastrar(carlos());
        assertTrue(service.remover(salvo.getId()));
        assertFalse(service.remover(salvo.getId()));
    }
}
