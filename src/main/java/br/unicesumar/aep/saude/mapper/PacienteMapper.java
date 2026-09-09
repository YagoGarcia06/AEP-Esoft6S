package br.unicesumar.aep.saude.mapper;

import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.RegistroClinico;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import org.bson.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapeia a coleção {@code pacientes}, que contém os objetos complexos da PoC:
 * {@code contato} e {@code endereco} aninhados e {@code historico} como lista
 * de subdocumentos.
 */
public class PacienteMapper implements DocumentMapper<Paciente> {

    @Override
    public Document paraDocumento(Paciente paciente) {
        List<Document> historico = new ArrayList<>();
        for (RegistroClinico registro : paciente.getHistorico()) {
            historico.add(registroParaDocumento(registro));
        }

        return new Document("_id", paciente.getId())
                .append("nome", paciente.getNome())
                .append("cpf", paciente.getCpf())
                .append("dataNascimento", paciente.getDataNascimento().toString())
                .append("contato", ContatoMapper.paraDocumento(paciente.getContato()))
                .append("endereco", ContatoMapper.paraDocumento(paciente.getEndereco()))
                .append("historico", historico);
    }

    @Override
    public Paciente paraEntidade(Document documento) {
        Paciente paciente = new Paciente(
                documento.getString("_id"),
                documento.getString("nome"),
                documento.getString("cpf"),
                LocalDate.parse(documento.getString("dataNascimento")),
                ContatoMapper.paraContato(documento.get("contato", Document.class)),
                ContatoMapper.paraEndereco(documento.get("endereco", Document.class)));

        List<Document> historico = documento.getList("historico", Document.class);
        if (historico != null) {
            for (Document registro : historico) {
                paciente.adicionarRegistro(documentoParaRegistro(registro));
            }
        }
        return paciente;
    }

    static Document registroParaDocumento(RegistroClinico registro) {
        return new Document("data", registro.getData().toString())
                .append("tipo", registro.getTipo().name())
                .append("profissionalId", registro.getProfissionalId())
                .append("descricao", registro.getDescricao())
                .append("prescricoes", new ArrayList<>(registro.getPrescricoes()));
    }

    static RegistroClinico documentoParaRegistro(Document documento) {
        return new RegistroClinico(
                LocalDate.parse(documento.getString("data")),
                TipoAtendimento.valueOf(documento.getString("tipo")),
                documento.getString("profissionalId"),
                documento.getString("descricao"),
                documento.getList("prescricoes", String.class));
    }
}
