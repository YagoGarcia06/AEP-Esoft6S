package br.unicesumar.aep.saude.service;

import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.repository.memory.InMemoryAgendamentoRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryPacienteRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryProfissionalRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryRelatorioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 3, 1);

    private final RelatorioService service = new RelatorioService(new InMemoryRelatorioRepository(
            new InMemoryAgendamentoRepository(), new InMemoryPacienteRepository(),
            new InMemoryProfissionalRepository()));

    @Test
    @DisplayName("consulta a agenda do dia e os resumos de um período válido")
    void deveConsultar() {
        assertTrue(service.agendaDoDia(INICIO).isEmpty());
        assertTrue(service.resumoPorProfissional(INICIO, INICIO.plusDays(30)).isEmpty());
        assertTrue(service.resumoPorTipo(INICIO, INICIO).isEmpty());
    }

    @Test
    @DisplayName("valida datas obrigatórias e ordem do período")
    void deveValidarPeriodo() {
        assertThrows(NullPointerException.class, () -> service.agendaDoDia(null));
        assertThrows(NullPointerException.class, () -> service.resumoPorTipo(null, INICIO));
        assertThrows(NullPointerException.class, () -> service.resumoPorProfissional(INICIO, null));
        assertThrows(RegraNegocioException.class,
                () -> service.resumoPorTipo(INICIO, INICIO.minusDays(1)));
    }

    @Test
    @DisplayName("limita o período a um ano")
    void deveLimitarPeriodo() {
        assertDoesNotThrow(() -> service.resumoPorTipo(INICIO,
                INICIO.plusDays(RelatorioService.PERIODO_MAXIMO_DIAS - 1)));
        assertThrows(RegraNegocioException.class, () -> service.resumoPorProfissional(INICIO,
                INICIO.plusDays(RelatorioService.PERIODO_MAXIMO_DIAS)));
    }

    @Test
    @DisplayName("exige o repositório")
    void deveExigirRepositorio() {
        assertThrows(NullPointerException.class, () -> new RelatorioService(null));
    }
}
