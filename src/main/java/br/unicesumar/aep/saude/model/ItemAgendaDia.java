package br.unicesumar.aep.saude.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Linha da agenda do dia já com os nomes do paciente e do profissional.
 * É o resultado de uma consulta que junta as coleções "agendamentos",
 * "pacientes" e "profissionais".
 */
public final class ItemAgendaDia {

    /** Usado quando o paciente ou o profissional referenciado não existe mais. */
    public static final String REMOVIDO = "(removido)";

    private final LocalDateTime dataHora;
    private final String pacienteNome;
    private final String profissionalNome;
    private final TipoAtendimento tipo;
    private final StatusAgendamento status;

    public ItemAgendaDia(LocalDateTime dataHora, String pacienteNome, String profissionalNome,
                         TipoAtendimento tipo, StatusAgendamento status) {
        this.dataHora = Objects.requireNonNull(dataHora, "Data e hora são obrigatórias.");
        this.pacienteNome = nomeOuRemovido(pacienteNome);
        this.profissionalNome = nomeOuRemovido(profissionalNome);
        this.tipo = Objects.requireNonNull(tipo, "Tipo é obrigatório.");
        this.status = Objects.requireNonNull(status, "Status é obrigatório.");
    }

    private static String nomeOuRemovido(String nome) {
        return (nome == null || nome.isBlank()) ? REMOVIDO : nome;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getPacienteNome() {
        return pacienteNome;
    }

    public String getProfissionalNome() {
        return profissionalNome;
    }

    public TipoAtendimento getTipo() {
        return tipo;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return dataHora.toLocalTime() + " " + tipo.getDescricao() + " - " + pacienteNome
                + " com " + profissionalNome + " (" + status + ")";
    }
}
