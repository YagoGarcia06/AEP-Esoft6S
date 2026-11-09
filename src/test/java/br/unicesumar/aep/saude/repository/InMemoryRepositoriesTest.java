package br.unicesumar.aep.saude.repository;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.FiltroAgenda;
import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Disponibilidade;
import br.unicesumar.aep.saude.model.Endereco;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.repository.memory.InMemoryAgendamentoRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryPacienteRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRepositoriesTest {

    private InMemoryPacienteRepository pacientes;
    private InMemoryProfissionalRepository profissionais;
    private InMemoryAgendamentoRepository agendamentos;

    @BeforeEach
    void preparar() {
        pacientes = new InMemoryPacienteRepository();
        profissionais = new InMemoryProfissionalRepository();
        agendamentos = new InMemoryAgendamentoRepository();
    }

    private Paciente maria() {
        return new Paciente("Maria Souza", "529.982.247-25", LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR"));
    }

    private Profissional carlos() {
        Profissional carlos = new Profissional("Dr. Carlos Lima", "111.444.777-35", "CRM-PR 12345",
                new Contato("carlos@clinica.com", "44 91111-1111"));
        carlos.adicionarEspecialidade("Cardiologia");
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        return carlos;
    }

    @Test
    @DisplayName("executa o CRUD básico de pacientes")
    void deveExecutarCrudDePacientes() {
        Paciente maria = pacientes.salvar(maria());

        assertEquals(1, pacientes.contar());
        assertTrue(pacientes.buscarPorId(maria.getId()).isPresent());
        assertEquals(1, pacientes.listarTodos().size());
        assertTrue(pacientes.remover(maria.getId()));
        assertFalse(pacientes.remover(maria.getId()));
        assertEquals(0, pacientes.contar());
    }

    @Test
    @DisplayName("trata id nulo e entidade nula com segurança")
    void deveTratarEntradasNulas() {
        assertTrue(pacientes.buscarPorId(null).isEmpty());
        assertFalse(pacientes.remover(null));
        assertThrows(NullPointerException.class, () -> pacientes.salvar(null));
    }

    @Test
    @DisplayName("sobrescreve a entidade quando o id já existe")
    void deveSobrescreverPorId() {
        Paciente maria = pacientes.salvar(maria());
        maria.setNome("Maria Souza Silva");
        pacientes.salvar(maria);

        assertEquals(1, pacientes.contar());
        assertEquals("Maria Souza Silva", pacientes.buscarPorId(maria.getId()).orElseThrow().getNome());
    }

    @Test
    @DisplayName("busca paciente por CPF com ou sem máscara")
    void deveBuscarPorCpf() {
        Paciente maria = pacientes.salvar(maria());

        assertTrue(pacientes.buscarPorCpf("529.982.247-25").isPresent());
        assertEquals(maria.getId(), pacientes.buscarPorCpf("52998224725").orElseThrow().getId());
        assertTrue(pacientes.buscarPorCpf("111.444.777-35").isEmpty());
        assertTrue(pacientes.buscarPorCpf(null).isEmpty());
    }

    @Test
    @DisplayName("busca profissionais por especialidade")
    void deveBuscarPorEspecialidade() {
        profissionais.salvar(carlos());

        assertEquals(1, profissionais.buscarPorEspecialidade("cardiologia").size());
        assertTrue(profissionais.buscarPorEspecialidade("Pediatria").isEmpty());
    }

    @Test
    @DisplayName("limpa todos os registros")
    void deveLimpar() {
        pacientes.salvar(maria());
        pacientes.limpar();
        assertEquals(0, pacientes.contar());
    }

    @Test
    @DisplayName("filtra a agenda por profissional e data, em ordem cronológica")
    void deveBuscarAgendaDoProfissional() {
        agendamentos.salvar(new Agendamento("pac-1", "prof-1",
                LocalDateTime.of(2026, 3, 9, 10, 0), TipoAtendimento.CONSULTA));
        agendamentos.salvar(new Agendamento("pac-2", "prof-1",
                LocalDateTime.of(2026, 3, 9, 8, 0), TipoAtendimento.EXAME));
        agendamentos.salvar(new Agendamento("pac-3", "prof-1",
                LocalDateTime.of(2026, 3, 10, 8, 0), TipoAtendimento.CONSULTA));
        agendamentos.salvar(new Agendamento("pac-4", "prof-2",
                LocalDateTime.of(2026, 3, 9, 8, 0), TipoAtendimento.CONSULTA));

        List<Agendamento> agenda =
                agendamentos.buscarPorProfissionalEData("prof-1", LocalDate.of(2026, 3, 9));

        assertEquals(2, agenda.size());
        assertEquals(LocalTime.of(8, 0), agenda.get(0).getDataHora().toLocalTime());
        assertEquals(LocalTime.of(10, 0), agenda.get(1).getDataHora().toLocalTime());
        assertTrue(agendamentos.buscarPorProfissionalEData(null, LocalDate.of(2026, 3, 9)).isEmpty());
        assertTrue(agendamentos.buscarPorProfissionalEData("prof-1", null).isEmpty());
    }

    @Test
    @DisplayName("filtra a agenda por paciente, em ordem cronológica")
    void deveBuscarAgendaDoPaciente() {
        agendamentos.salvar(new Agendamento("pac-1", "prof-1",
                LocalDateTime.of(2026, 3, 16, 9, 0), TipoAtendimento.CONSULTA));
        agendamentos.salvar(new Agendamento("pac-1", "prof-2",
                LocalDateTime.of(2026, 3, 9, 9, 0), TipoAtendimento.EXAME));
        agendamentos.salvar(new Agendamento("pac-2", "prof-1",
                LocalDateTime.of(2026, 3, 9, 11, 0), TipoAtendimento.CONSULTA));

        List<Agendamento> agenda = agendamentos.buscarPorPaciente("pac-1");

        assertEquals(2, agenda.size());
        assertEquals(LocalDate.of(2026, 3, 9), agenda.get(0).getData());
        assertTrue(agendamentos.buscarPorPaciente(null).isEmpty());
    }

    @Test
    @DisplayName("aplica o filtro da agenda em ordem cronológica")
    void deveBuscarComFiltro() {
        agendamentos.salvar(new Agendamento("pac-1", "prof-1",
                LocalDateTime.of(2026, 3, 16, 9, 0), TipoAtendimento.CONSULTA));
        agendamentos.salvar(new Agendamento("pac-1", "prof-1",
                LocalDateTime.of(2026, 3, 9, 9, 0), TipoAtendimento.CONSULTA));
        agendamentos.salvar(new Agendamento("pac-2", "prof-2",
                LocalDateTime.of(2026, 3, 9, 11, 0), TipoAtendimento.EXAME));

        List<Agendamento> doProf1 = agendamentos.buscarComFiltro(new FiltroAgenda(null, null, "prof-1", null));

        assertEquals(2, doProf1.size());
        assertEquals(LocalDate.of(2026, 3, 9), doProf1.get(0).getData());
        assertEquals(3, agendamentos.buscarComFiltro(FiltroAgenda.TODOS).size());
        assertThrows(NullPointerException.class, () -> agendamentos.buscarComFiltro(null));
    }
}
