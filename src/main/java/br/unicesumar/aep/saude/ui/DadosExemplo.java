package br.unicesumar.aep.saude.ui;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Disponibilidade;
import br.unicesumar.aep.saude.model.Endereco;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.service.AgendamentoService;
import br.unicesumar.aep.saude.service.PacienteService;
import br.unicesumar.aep.saude.service.ProfissionalService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Carga inicial usada apenas na demonstração, para que a tela já abra com
 * dados suficientes para exercitar todas as regras.
 */
public final class DadosExemplo {

    private DadosExemplo() {
    }

    public static void carregar(PacienteService pacienteService,
                                ProfissionalService profissionalService,
                                AgendamentoService agendamentoService) {

        Paciente maria = pacienteService.cadastrar(new Paciente(
                "Maria Souza", "529.982.247-25", LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua das Flores, 100", "Maringá", "PR")));

        Paciente joao = pacienteService.cadastrar(new Paciente(
                "João Pereira", "168.995.350-09", LocalDate.of(1978, 9, 3),
                new Contato("joao@email.com", "44 90000-1111"),
                new Endereco("Av. Brasil, 2200", "Maringá", "PR")));

        Profissional carlos = new Profissional(
                "Dr. Carlos Lima", "111.444.777-35", "CRM-PR 12345",
                new Contato("carlos@clinica.com", "44 91111-1111"));
        carlos.adicionarEspecialidade("Cardiologia");
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));
        profissionalService.cadastrar(carlos);

        Profissional ana = new Profissional(
                "Dra. Ana Prado", "615.406.180-73", "CRM-PR 54321",
                new Contato("ana@clinica.com", "44 92222-2222"));
        ana.adicionarEspecialidade("Clínica Geral");
        ana.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        profissionalService.cadastrar(ana);

        LocalDateTime segunda9h = proximo(DayOfWeek.MONDAY, LocalTime.of(9, 0));
        LocalDateTime terca10h = proximo(DayOfWeek.TUESDAY, LocalTime.of(10, 0));

        Agendamento consulta = agendamentoService.agendar(
                maria.getId(), carlos.getId(), segunda9h, TipoAtendimento.CONSULTA);
        agendamentoService.realizar(consulta.getId(),
                "Consulta de rotina em cardiologia. Paciente estável.",
                List.of("Losartana 50mg - 1x ao dia"));

        agendamentoService.agendar(
                joao.getId(), ana.getId(), terca10h, TipoAtendimento.EXAME);
        agendamentoService.agendar(
                maria.getId(), carlos.getId(), segunda9h.plusMinutes(30), TipoAtendimento.CONSULTA);
    }

    private static LocalDateTime proximo(DayOfWeek dia, LocalTime hora) {
        return LocalDate.now().with(TemporalAdjusters.next(dia)).atTime(hora);
    }
}
