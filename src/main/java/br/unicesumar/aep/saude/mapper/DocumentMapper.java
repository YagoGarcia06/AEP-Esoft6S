package br.unicesumar.aep.saude.mapper;

import org.bson.Document;

/**
 * Tradutor entre uma entidade do domínio e o documento armazenado no MongoDB.
 * Manter essa conversão isolada evita que o domínio conheça a API do driver.
 *
 * @param <T> tipo da entidade convertida.
 */
public interface DocumentMapper<T> {

    Document paraDocumento(T entidade);

    T paraEntidade(Document documento);
}
