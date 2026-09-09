package br.unicesumar.aep.saude.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacienteTest {

    private Paciente novaMaria() {
        return new Paciente("Maria Souza", "529.982.247-25", LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A, 100", "Maringá", "PR"));
    }

    @Test
    @DisplayName("gera id e normaliza o CPF ao criar o paciente")
    void deveCriarPacienteValido() {
        Paciente maria = novaMaria();
        assertNotNull(maria.getId());
        assertFalse(maria.getId().isBlank());
        assertEquals("52998224725", maria.getCpf());
        assertEquals("529.982.247-25", maria.getCpfFormatado());
        assertEquals("Paciente", maria.tipo());
        assertTrue(maria.descricaoResumida().contains("Maria Souza"));
    }

    @Test
    @DisplayName("preserva o id informado")
    void devePreservarIdInformado() {
        Paciente maria = new Paciente("id-fixo", "Maria Souza", "52998224725",
                LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR"));
        assertEquals("id-fixo", maria.getId());
    }

    @Test
    @DisplayName("calcula idade em uma data de referência")
    void deveCalcularIdade() {
        Paciente maria = novaMaria();
        assertEquals(35, maria.idadeEm(LocalDate.of(2026, 4, 11)));
        assertEquals(36, maria.idadeEm(LocalDate.of(2026, 4, 12)));
    }

    @Test
    @DisplayName("rejeita data de referência anterior ao nascimento")
    void deveRejeitarReferenciaInvalida() {
        Paciente maria = novaMaria();
        assertThrows(IllegalArgumentException.class, () -> maria.idadeEm(LocalDate.of(1980, 1, 1)));
        assertThrows(NullPointerException.class, () -> maria.idadeEm(null));
    }

    @Test
    @DisplayName("rejeita CPF com formato inválido")
    void deveRejeitarCpfInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Paciente(
                "Maria", "123", LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR")));
        assertThrows(IllegalArgumentException.class, () -> new Paciente(
                "Maria", "111.111.111-11", LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR")));
        assertThrows(IllegalArgumentException.class, () -> new Paciente(
                "Maria", null, LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR")));
    }

    @Test
    @DisplayName("rejeita nome em branco e data de nascimento no futuro")
    void deveRejeitarCamposInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new Paciente(
                "  ", "529.982.247-25", LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR")));
        assertThrows(IllegalArgumentException.class, () -> new Paciente(
                "Maria", "529.982.247-25", LocalDate.now().plusDays(1),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR")));
    }

    @Test
    @DisplayName("exige contato, endereço e data de nascimento")
    void deveExigirObjetosObrigatorios() {
        assertThrows(NullPointerException.class, () -> new Paciente(
                "Maria", "529.982.247-25", LocalDate.of(1990, 4, 12), null,
                new Endereco("Rua A", "Maringá", "PR")));
        assertThrows(NullPointerException.class, () -> new Paciente(
                "Maria", "529.982.247-25", LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"), null));
        assertThrows(NullPointerException.class, () -> new Paciente(
                "Maria", "529.982.247-25", null,
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR")));
    }

    @Test
    @DisplayName("acumula registros no histórico clínico")
    void deveAcumularHistorico() {
        Paciente maria = novaMaria();
        assertFalse(maria.possuiHistorico());

        maria.adicionarRegistro(new RegistroClinico(LocalDate.of(2026, 3, 9),
                TipoAtendimento.CONSULTA, "prof-1", "Consulta de rotina", List.of("Losartana 50mg")));

        assertTrue(maria.possuiHistorico());
        assertEquals(1, maria.getHistorico().size());
        assertEquals("Consulta de rotina", maria.getHistorico().get(0).getDescricao());
        assertThrows(NullPointerException.class, () -> maria.adicionarRegistro(null));
    }

    @Test
    @DisplayName("expõe o histórico como lista somente leitura")
    void deveProtegerHistorico() {
        Paciente maria = novaMaria();
        List<RegistroClinico> historico = maria.getHistorico();
        assertThrows(UnsupportedOperationException.class, () -> historico.add(
                new RegistroClinico(LocalDate.of(2026, 3, 9), TipoAtendimento.EXAME,
                        "prof-1", "Hemograma", null)));
    }

    @Test
    @DisplayName("permite atualizar contato e endereço")
    void devePermitirAtualizacoes() {
        Paciente maria = novaMaria();
        maria.setContato(new Contato("novo@email.com", "44 92222-2222"));
        maria.setEndereco(new Endereco("Rua B", "Curitiba", "PR"));
        maria.setNome("Maria S. Souza");
        assertEquals("novo@email.com", maria.getContato().getEmail());
        assertEquals("Curitiba", maria.getEndereco().getCidade());
        assertEquals("Maria S. Souza", maria.getNome());
        assertEquals(LocalDate.of(1990, 4, 12), maria.getDataNascimento());
    }

    @Test
    @DisplayName("compara pacientes por id")
    void deveCompararPorId() {
        Paciente maria = novaMaria();
        Paciente outra = novaMaria();
        assertTrue(maria.equals(maria));
        assertFalse(maria.equals(outra));
        assertFalse(maria.equals("texto"));
        assertEquals(maria.getId().hashCode(), maria.hashCode());
    }
}
