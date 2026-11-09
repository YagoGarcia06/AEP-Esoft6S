package br.unicesumar.aep.saude.mapper;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.AlteracaoAgendamento;
import br.unicesumar.aep.saude.model.StatusAgendamento;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapeia a coleção {@code agendamentos}, que se relaciona com {@code pacientes}
 * e {@code profissionais} por referência de id.
 *
 * <p>Além de {@code dataHora}, o documento guarda o campo derivado {@code data}
 * (apenas o dia). Ele existe para que a consulta de agenda por profissional e
 * dia seja uma igualdade indexável, em vez de uma varredura por intervalo.</p>
 *
 * <p>As mudanças de status ficam embutidas na lista {@code alteracoes}: elas só
 * fazem sentido dentro do agendamento e são sempre lidas junto com ele.
 * Documentos gravados antes dessa lista existir são lidos com histórico vazio.</p>
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
                .append("status", agendamento.getStatus().name())
                .append("alteracoes", alteracoesParaDocumentos(agendamento.getAlteracoes()));
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
                StatusAgendamento.valueOf(documento.getString("status")),
                alteracoesParaEntidades(documento.getList("alteracoes", Document.class)));
    }

    private List<Document> alteracoesParaDocumentos(List<AlteracaoAgendamento> alteracoes) {
        List<Document> documentos = new ArrayList<>();
        for (AlteracaoAgendamento alteracao : alteracoes) {
            Document documento = new Document("momento", alteracao.getMomento().toString())
                    .append("statusAnterior", alteracao.getStatusAnterior().name())
                    .append("statusNovo", alteracao.getStatusNovo().name());
            if (alteracao.getObservacao() != null) {
                documento.append("observacao", alteracao.getObservacao());
            }
            documentos.add(documento);
        }
        return documentos;
    }

    private List<AlteracaoAgendamento> alteracoesParaEntidades(List<Document> documentos) {
        List<AlteracaoAgendamento> alteracoes = new ArrayList<>();
        if (documentos == null) {
            return alteracoes;
        }
        for (Document documento : documentos) {
            alteracoes.add(new AlteracaoAgendamento(
                    LocalDateTime.parse(documento.getString("momento")),
                    StatusAgendamento.valueOf(documento.getString("statusAnterior")),
                    StatusAgendamento.valueOf(documento.getString("statusNovo")),
                    documento.getString("observacao")));
        }
        return alteracoes;
    }
}
