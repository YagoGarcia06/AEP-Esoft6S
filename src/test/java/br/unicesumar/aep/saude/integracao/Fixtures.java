package br.unicesumar.aep.saude.integracao;

import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Disponibilidade;
import br.unicesumar.aep.saude.model.Endereco;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/** Pessoas reutilizadas pelos testes de integração (CPFs válidos). */
final class Fixtures {

    private Fixtures() {
    }

    static Paciente maria() {
        return new Paciente("Maria Souza", "529.982.247-25", LocalDate.of(1990, 4, 12),
                new Contato("maria@email.com", "44 90000-0000"),
                new Endereco("Rua A", "Maringá", "PR"));
    }

    static Paciente joao() {
        return new Paciente("João Pereira", "168.995.350-09", LocalDate.of(1978, 9, 3),
                new Contato("joao@email.com", "44 93333-3333"),
                new Endereco("Rua B", "Maringá", "PR"));
    }

    /** Cardiologista que atende às segundas, das 8h às 12h. */
    static Profissional carlos() {
        Profissional carlos = new Profissional("Dr. Carlos Lima", "111.444.777-35", "CRM-PR 12345",
                new Contato("carlos@clinica.com", "44 91111-1111"));
        carlos.adicionarEspecialidade("Cardiologia");
        carlos.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        return carlos;
    }

    /** Clínica geral que atende às segundas, das 8h às 12h. */
    static Profissional ana() {
        Profissional ana = new Profissional("Dra. Ana Prado", "615.406.180-73", "CRM-PR 54321",
                new Contato("ana@clinica.com", "44 92222-2222"));
        ana.adicionarEspecialidade("Clínica Geral");
        ana.adicionarDisponibilidade(
                new Disponibilidade(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        return ana;
    }
}
