package br.unicesumar.aep.saude.infra;

import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Endereco;
import br.unicesumar.aep.saude.model.Paciente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextoAplicacaoTest {

    @Test
    @DisplayName("monta os três serviços no modo em memória")
    void deveMontarServicosEmMemoria() {
        try (ContextoAplicacao contexto = ContextoAplicacao.emMemoria()) {
            assertNotNull(contexto.getPacienteService());
            assertNotNull(contexto.getProfissionalService());
            assertNotNull(contexto.getAgendamentoService());
            assertTrue(contexto.getDescricaoArmazenamento().contains("Memória"));
        }
    }

    @Test
    @DisplayName("os serviços compartilham os mesmos repositórios")
    void deveCompartilharRepositorios() {
        try (ContextoAplicacao contexto = ContextoAplicacao.emMemoria()) {
            Paciente maria = contexto.getPacienteService().cadastrar(new Paciente(
                    "Maria Souza", "529.982.247-25", LocalDate.of(1990, 4, 12),
                    new Contato("maria@email.com", "44 90000-0000"),
                    new Endereco("Rua A", "Maringá", "PR")));

            assertEquals(1, contexto.getPacienteService().listar().size());
            assertTrue(contexto.getAgendamentoService().agendaDoPaciente(maria.getId()).isEmpty());
        }
    }

    @Test
    @DisplayName("exige uma conexão válida no modo MongoDB")
    void deveExigirConexao() {
        assertThrows(NullPointerException.class, () -> ContextoAplicacao.comMongo(null));
    }
}
