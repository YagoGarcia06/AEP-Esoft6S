package br.unicesumar.aep.saude.integracao;

import br.unicesumar.aep.saude.infra.MongoConnection;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Base dos testes de integração contra um MongoDB real.
 *
 * <p>Usa {@code MONGODB_URI_TESTES} (variável de ambiente) ou
 * {@code -Dmongodb.uri.testes}; sem configuração, tenta {@code localhost:27017}.
 * Se o servidor não responder em poucos segundos, a classe inteira é marcada
 * como ignorada — assim {@code mvn verify} continua funcionando em máquinas sem
 * MongoDB, enquanto o CI sobe um banco e executa tudo.</p>
 *
 * <p>Cada classe usa um banco descartável com nome aleatório, apagado antes de
 * cada teste e ao final, para nunca tocar no banco da aplicação.</p>
 */
@Tag("integracao")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class MongoIntegracaoBase {

    private MongoConnection conexao;
    private String nomeBanco;

    @BeforeAll
    void conectar() {
        nomeBanco = "poc_saude_teste_" + UUID.randomUUID().toString().substring(0, 8);
        conexao = new MongoConnection(uriDeTeste(), nomeBanco);
        boolean disponivel;
        try {
            conexao.verificarConexao();
            disponivel = true;
        } catch (RuntimeException e) {
            conexao.close();
            disponivel = false;
        }
        assumeTrue(disponivel, "MongoDB indisponível em " + uriDeTeste()
                + " — testes de integração ignorados.");
    }

    @BeforeEach
    void limparBanco() {
        conexao.getDatabase().drop();
        prepararRepositorios(conexao.getDatabase());
    }

    @AfterAll
    void desconectar() {
        if (conexao != null) {
            try {
                conexao.getDatabase().drop();
            } catch (RuntimeException ignorada) {
                // banco indisponível: nada a limpar
            }
            conexao.close();
        }
    }

    /** Cria os repositórios a cada teste, depois que o banco foi zerado (recria os índices). */
    protected abstract void prepararRepositorios(MongoDatabase database);

    protected MongoConnection conexao() {
        return conexao;
    }

    static String uriDeTeste() {
        String uri = System.getenv("MONGODB_URI_TESTES");
        if (uri == null || uri.isBlank()) {
            uri = System.getProperty("mongodb.uri.testes", MongoConnection.URI_PADRAO);
        }
        String opcao = "serverSelectionTimeoutMS=2000";
        if (uri.contains("?")) {
            return uri + "&" + opcao;
        }
        return (uri.endsWith("/") ? uri : uri + "/") + "?" + opcao;
    }
}
