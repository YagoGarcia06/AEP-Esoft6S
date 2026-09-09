package br.unicesumar.aep.saude.infra;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * Conexão única com o MongoDB.
 *
 * <p>A URI e o nome do banco podem ser definidos por variável de ambiente
 * ({@code MONGODB_URI} e {@code MONGODB_DATABASE}) ou por propriedade de sistema
 * ({@code -Dmongodb.uri} e {@code -Dmongodb.database}). Sem configuração, usa uma
 * instância local. Para o MongoDB Atlas basta informar a URI
 * {@code mongodb+srv://...} — nada mais muda no código.</p>
 */
public class MongoConnection implements AutoCloseable {

    public static final String URI_PADRAO = "mongodb://localhost:27017";
    public static final String BANCO_PADRAO = "poc_saude";

    private final MongoClient client;
    private final MongoDatabase database;
    private final String descricao;

    public MongoConnection() {
        this(uriConfigurada(), bancoConfigurado());
    }

    public MongoConnection(String uri, String banco) {
        this.client = MongoClients.create(uri);
        this.database = client.getDatabase(banco);
        this.descricao = "MongoDB " + ocultarCredenciais(uri) + " / " + banco;
    }

    /** Executa um ping para falhar cedo caso o servidor não esteja acessível. */
    public void verificarConexao() {
        database.runCommand(new Document("ping", 1));
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public void close() {
        client.close();
    }

    public static String uriConfigurada() {
        return configuracao("MONGODB_URI", "mongodb.uri", URI_PADRAO);
    }

    public static String bancoConfigurado() {
        return configuracao("MONGODB_DATABASE", "mongodb.database", BANCO_PADRAO);
    }

    private static String configuracao(String variavelAmbiente, String propriedade, String padrao) {
        String valor = System.getenv(variavelAmbiente);
        if (valor == null || valor.isBlank()) {
            valor = System.getProperty(propriedade);
        }
        return (valor == null || valor.isBlank()) ? padrao : valor.trim();
    }

    /** Evita expor usuário e senha da URI do Atlas na interface. */
    public static String ocultarCredenciais(String uri) {
        int arroba = uri.indexOf('@');
        int inicio = uri.indexOf("://");
        if (arroba < 0 || inicio < 0) {
            return uri;
        }
        return uri.substring(0, inicio + 3) + "***@" + uri.substring(arroba + 1);
    }
}
