package br.unicesumar.aep.saude.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MongoConnectionTest {

    @Test
    @DisplayName("oculta usuário e senha de uma URI do Atlas")
    void deveOcultarCredenciais() {
        String mascarada = MongoConnection.ocultarCredenciais(
                "mongodb+srv://aluno:senhaSecreta@cluster0.abcde.mongodb.net/");

        assertFalse(mascarada.contains("senhaSecreta"));
        assertFalse(mascarada.contains("aluno"));
        assertEquals("mongodb+srv://***@cluster0.abcde.mongodb.net/", mascarada);
    }

    @Test
    @DisplayName("mantém a URI local intacta, por não ter credenciais")
    void deveManterUriSemCredenciais() {
        assertEquals("mongodb://localhost:27017",
                MongoConnection.ocultarCredenciais("mongodb://localhost:27017"));
    }

    @Test
    @DisplayName("usa a configuração padrão quando nada é informado")
    void deveUsarPadroes() {
        String uriAnterior = System.getProperty("mongodb.uri");
        String bancoAnterior = System.getProperty("mongodb.database");
        System.clearProperty("mongodb.uri");
        System.clearProperty("mongodb.database");
        try {
            if (System.getenv("MONGODB_URI") == null) {
                assertEquals(MongoConnection.URI_PADRAO, MongoConnection.uriConfigurada());
            }
            if (System.getenv("MONGODB_DATABASE") == null) {
                assertEquals(MongoConnection.BANCO_PADRAO, MongoConnection.bancoConfigurado());
            }
        } finally {
            restaurar("mongodb.uri", uriAnterior);
            restaurar("mongodb.database", bancoAnterior);
        }
    }

    @Test
    @DisplayName("respeita a propriedade de sistema quando definida")
    void deveRespeitarPropriedadeDeSistema() {
        String anterior = System.getProperty("mongodb.database");
        System.setProperty("mongodb.database", "poc_saude_teste");
        try {
            if (System.getenv("MONGODB_DATABASE") == null) {
                assertEquals("poc_saude_teste", MongoConnection.bancoConfigurado());
            }
        } finally {
            restaurar("mongodb.database", anterior);
        }
    }

    private void restaurar(String propriedade, String valor) {
        if (valor == null) {
            System.clearProperty(propriedade);
        } else {
            System.setProperty(propriedade, valor);
        }
    }
}
