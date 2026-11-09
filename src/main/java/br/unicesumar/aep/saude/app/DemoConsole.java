package br.unicesumar.aep.saude.app;

import br.unicesumar.aep.saude.exception.RegraNegocioException;
import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Disponibilidade;
import br.unicesumar.aep.saude.model.Endereco;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.RegistroClinico;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.repository.memory.InMemoryAgendamentoRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryPacienteRepository;
import br.unicesumar.aep.saude.repository.memory.InMemoryProfissionalRepository;
import br.unicesumar.aep.saude.service.AgendamentoService;
import br.unicesumar.aep.saude.service.PacienteService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Demonstração dos fluxos da PoC pela linha de comando, útil quando não há
 * ambiente gráfico disponível. Camada fina: toda a regra está nos serviços.
 */
public final class DemoConsole {

    private DemoConsole() {
    }

    public static void executar() {
        InMemoryPacienteRepository pacientes = new InMemoryPacienteRepository();
        InMemoryProfissionalRepository profissionais = new InMemoryProfissionalRepository();
        InMemoryAgendamentoRepository agendamentos = new InMemoryAgendamentoRepository();

        PacienteService pacienteService = new PacienteService(pacientes);
        AgendamentoService agendamentoService =
                new AgendamentoService(agendamentos, pacientes, profissionais);

        System.out.println("=== PoC Agendamento em Saude (ODS 3) ===\n");

        Paciente maria = pacienteService.cadastrar(new Paciente(
                "Maria Souza", "529.982.247-25", LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A, 100", "Maringa", "PR")));
        System.out.println("[1] Paciente cadastrado: " + maria.descricaoResumida()
                + " | idade: " + maria.idade());

        Profissional carlos = new Profissional(
                "Dr. Carlos Lima", "111.444.777-35", "CRM-PR 12345",
                new Contato("carlos@clinica.com", "44 91111-1111"));
        carlos.adicionarEspecialidade("Cardiologia");
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        profissionais.salvar(carlos);
        System.out.println("[2] Profissional cadastrado: " + carlos.descricaoResumida()
                + " | " + carlos.getDisponibilidades());

        LocalDateTime segunda9h = proximaSegundaAs(LocalTime.of(9, 0));
        Agendamento consulta = agendamentoService.agendar(
                maria.getId(), carlos.getId(), segunda9h, TipoAtendimento.CONSULTA);
        System.out.println("\n[3] Agendado: " + consulta
                + " | fim: " + consulta.getFim().toLocalTime());

        System.out.println("\n[4] Tentando agendar exame sobreposto no mesmo horario...");
        try {
            agendamentoService.agendar(maria.getId(), carlos.getId(),
                    segunda9h.plusMinutes(15), TipoAtendimento.EXAME);
        } catch (RegraNegocioException e) {
            System.out.println("    Bloqueado: " + e.getMessage());
        }

        System.out.println("\n[5] Tentando agendar fora da disponibilidade (14h)...");
        try {
            agendamentoService.agendar(maria.getId(), carlos.getId(),
                    proximaSegundaAs(LocalTime.of(14, 0)), TipoAtendimento.CONSULTA);
        } catch (RegraNegocioException e) {
            System.out.println("    Bloqueado: " + e.getMessage());
        }

        agendamentoService.confirmar(consulta.getId());
        agendamentoService.realizar(consulta.getId(),
                "Consulta de rotina em cardiologia. Paciente estavel.",
                List.of("Losartana 50mg - 1x ao dia"));
        System.out.println("\n[6] Presenca confirmada e atendimento concluido. Status: "
                + agendamentoService.buscarPorId(consulta.getId()).getStatus());

        System.out.println("\n[7] Historico clinico de " + maria.getNome() + ":");
        for (RegistroClinico registro : pacienteService.historico(maria.getId())) {
            System.out.println("    - " + registro);
            registro.getPrescricoes().forEach(p -> System.out.println("        Rx: " + p));
        }

        System.out.println("\n[8] Agenda do profissional em " + segunda9h.toLocalDate() + ":");
        agendamentoService.agendaDoProfissional(carlos.getId(), segunda9h.toLocalDate())
                .forEach(a -> System.out.println("    - " + a));
    }

    private static LocalDateTime proximaSegundaAs(LocalTime hora) {
        return LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .atTime(hora);
    }
}
