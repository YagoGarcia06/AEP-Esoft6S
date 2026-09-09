package br.unicesumar.aep.saude.mapper;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import org.bson.Document;

import java.time.LocalDateTime;

/**
 * Mapeia a coleção {@code agendamentos}, que se relaciona com {@code pacientes}
 * e {@code profissionais} por referência de id.
 *
 * <p>Além de {@code dataHora}, o documento guarda o campo derivado {@code data}
 * (apenas o dia). Ele existe para que a consulta de agenda por profissional e
 * dia seja uma igualdade indexável, em vez de uma varredura por intervalo.</p>
 */
public class AgendamentoMapper implements DocumentMapper<Agendamento> {

    @Override
    public Document paraDocumento(Agendamento agendamento) {
        return new Document("_id", agendamento.getId())
                .append("pacienteId", agendamento.getPacienteId())
                .append("profissionalId", agendamento.getProfissionalId())
                .append("dataHora", agendamento.getDataHora().toString())
                .append("data", agendamento.getData().toString())
                .append("duracaoMinutos", agendamento.getDuracaoMinutos())
                .append("tipo", agendamento.getTipo().name())
                .append("status", agendamento.getStatus().name());
    }

    @Override
    public Agendamento paraEntidade(Document documento) {
        return new Agendamento(
                documento.getString("_id"),
                documento.getString("pacienteId"),
                documento.getString("profissionalId"),
                LocalDateTime.parse(documento.getString("dataHora")),
                documento.getInteger("duracaoMinutos"),
                TipoAtendimento.valueOf(documento.getString("tipo")),
                StatusAgendamento.valueOf(documento.getString("status")));
    }
}
