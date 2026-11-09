package br.unicesumar.aep.saude.repository;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Endereco;
import br.unicesumar.aep.saude.model.ItemAgendaDia;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.ResumoAtendimentos;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.repository.memory.InMemoryAgendamentoRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryPacienteRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryProfissionalRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryRelatorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRelatorioRepositoryTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 3, 9);

    private InMemoryPacienteRepository pacientes;
    private InMemoryProfissionalRepository profissionais;
    private InMemoryAgendamentoRepository agendamentos;
    private InMemoryRelatorioRepository relatorios;
    private Paciente maria;
    private Profissional carlos;
    private Profissional ana;

    @BeforeEach
    void preparar() {
        pacientes = new InMemoryPacienteRepository();
        profissionais = new InMemoryProfissionalRepository();
        agendamentos = new InMemoryAgendamentoRepository();
        relatorios = new InMemoryRelatorioRepository(agendamentos, pacientes, profissionais);

        maria = pacientes.salvar(new Paciente("Maria Souza", "529.982.247-25",
                LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR")));
        carlos = profissionais.salvar(new Profissional("Dr. Carlos Lima", "111.444.777-35",
                "CRM-PR 12345", new Contato("carlos@clinica.com", "44 91111-1111")));
        ana = profissionais.salvar(new Profissional("Dra. Ana Prado", "615.406.180-73",
                "CRM-PR 54321", new Contato("ana@clinica.com", "44 92222-2222")));
    }

    private void agendar(Profissional profissional, LocalDateTime dataHora,
                         TipoAtendimento tipo, StatusAgendamento status) {
        agendamentos.salvar(new Agendamento(null, maria.getId(), profissional.getId(), dataHora,
                tipo.getDuracaoPadraoMinutos(), tipo, status));
    }

    @Test
    @DisplayName("monta a agenda do dia em ordem de horário e com os nomes")
    void deveMontarAgendaDoDia() {
        agendar(carlos, SEGUNDA.atTime(10, 0), TipoAtendimento.CONSULTA, StatusAgendamento.CONFIRMADO);
        agendar(ana, SEGUNDA.atTime(8, 30), TipoAtendimento.EXAME, StatusAgendamento.AGENDADO);
        agendar(carlos, SEGUNDA.plusDays(1).atTime(9, 0), TipoAtendimento.CONSULTA, StatusAgendamento.AGENDADO);

        List<ItemAgendaDia> agenda = relatorios.agendaDoDia(SEGUNDA);

        assertEquals(2, agenda.size());
        assertEquals(SEGUNDA.atTime(8, 30), agenda.get(0).getDataHora());
        assertEquals("Dra. Ana Prado", agenda.get(0).getProfissionalNome());
        assertEquals("Maria Souza", agenda.get(1).getPacienteNome());
        assertEquals(StatusAgendamento.CONFIRMADO, agenda.get(1).getStatus());
        assertTrue(relatorios.agendaDoDia(SEGUNDA.minusDays(1)).isEmpty());
    }

    @Test
    @DisplayName("mostra '(removido)' quando o profissional não existe mais")
    void deveMarcarProfissionalRemovido() {
        agendar(carlos, SEGUNDA.atTime(9, 0), TipoAtendimento.CONSULTA, StatusAgendamento.REALIZADO);
        profissionais.remover(carlos.getId());

        assertEquals(ItemAgendaDia.REMOVIDO, relatorios.agendaDoDia(SEGUNDA).get(0).getProfissionalNome());
        assertEquals(ItemAgendaDia.REMOVIDO,
                relatorios.resumoPorProfissional(SEGUNDA, SEGUNDA).get(0).getGrupo());
    }

    @Test
    @DisplayName("resume por profissional contando cada status, do maior para o menor")
    void deveResumirPorProfissional() {
        agendar(carlos, SEGUNDA.atTime(8, 0), TipoAtendimento.CONSULTA, StatusAgendamento.REALIZADO);
        agendar(carlos, SEGUNDA.atTime(9, 0), TipoAtendimento.CONSULTA, StatusAgendamento.NAO_COMPARECEU);
        agendar(carlos, SEGUNDA.atTime(10, 0), TipoAtendimento.EXAME, StatusAgendamento.CANCELADO);
        agendar(carlos, SEGUNDA.plusDays(2).atTime(10, 0), TipoAtendimento.CONSULTA, StatusAgendamento.CONFIRMADO);
        agendar(ana, SEGUNDA.atTime(8, 0), TipoAtendimento.EXAME, StatusAgendamento.REALIZADO);
        agendar(ana, SEGUNDA.plusDays(30).atTime(8, 0), TipoAtendimento.EXAME, StatusAgendamento.REALIZADO);

        List<ResumoAtendimentos> resumo = relatorios.resumoPorProfissional(SEGUNDA, SEGUNDA.plusDays(6));

        assertEquals(2, resumo.size());
        ResumoAtendimentos doCarlos = resumo.get(0);
        assertEquals("Dr. Carlos Lima", doCarlos.getGrupo());
        assertEquals(4, doCarlos.getTotal());
        assertEquals(1, doCarlos.getRealizados());
        assertEquals(1, doCarlos.getFaltas());
        assertEquals(1, doCarlos.getCancelados());
        assertEquals(1, doCarlos.getPendentes());
        assertEquals(50.0, doCarlos.getPercentualFaltas(), 0.001);
        assertEquals("Dra. Ana Prado", resumo.get(1).getGrupo());
        assertEquals(1, resumo.get(1).getTotal());
    }

    @Test
    @DisplayName("resume por tipo, incluindo as duas pontas do período")
    void deveResumirPorTipo() {
        agendar(carlos, SEGUNDA.atTime(8, 0), TipoAtendimento.CONSULTA, StatusAgendamento.REALIZADO);
        agendar(carlos, SEGUNDA.plusDays(6).atTime(8, 0), TipoAtendimento.CONSULTA, StatusAgendamento.AGENDADO);
        agendar(ana, SEGUNDA.plusDays(3).atTime(8, 0), TipoAtendimento.EXAME, StatusAgendamento.NAO_COMPARECEU);
        agendar(ana, SEGUNDA.plusDays(7).atTime(8, 0), TipoAtendimento.EXAME, StatusAgendamento.AGENDADO);

        List<ResumoAtendimentos> resumo = relatorios.resumoPorTipo(SEGUNDA, SEGUNDA.plusDays(6));

        assertEquals(2, resumo.size());
        assertEquals("Consulta", resumo.get(0).getGrupo());
        assertEquals(2, resumo.get(0).getTotal());
        assertEquals("Exame", resumo.get(1).getGrupo());
        assertEquals(1, resumo.get(1).getFaltas());
        assertEquals(100.0, resumo.get(1).getPercentualFaltas(), 0.001);
    }

    @Test
    @DisplayName("período sem atendimentos devolve lista vazia")
    void periodoVazio() {
        assertTrue(relatorios.resumoPorTipo(SEGUNDA, SEGUNDA.plusDays(6)).isEmpty());
    }

    @Test
    @DisplayName("exige os três repositórios")
    void deveExigirDependencias() {
        assertThrows(NullPointerException.class,
                () -> new InMemoryRelatorioRepository(null, pacientes, profissionais));
        assertThrows(NullPointerException.class,
                () -> new InMemoryRelatorioRepository(agendamentos, null, profissionais));
        assertThrows(NullPointerException.class,
                () -> new InMemoryRelatorioRepository(agendamentos, pacientes, null));
    }
}
