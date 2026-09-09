package br.unicesumar.aep.saude.mapper;

import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Endereco;
import org.bson.Document;

/**
 * Conversão dos objetos de valor que viram documentos aninhados.
 */
public final class ContatoMapper {

    private ContatoMapper() {
    }

    public static Document paraDocumento(Contato contato) {
        return new Document("email", contato.getEmail())
                .append("telefone", contato.getTelefone());
    }

    public static Contato paraContato(Document documento) {
        return new Contato(documento.getString("email"), documento.getString("telefone"));
    }

    public static Document paraDocumento(Endereco endereco) {
        return new Document("rua", endereco.getRua())
                .append("cidade", endereco.getCidade())
                .append("estado", endereco.getEstado());
    }

    public static Endereco paraEndereco(Document documento) {
        return new Endereco(documento.getString("rua"),
                documento.getString("cidade"),
                documento.getString("estado"));
    }
}
