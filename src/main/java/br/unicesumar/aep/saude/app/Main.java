package br.unicesumar.aep.saude.app;

import br.unicesumar.aep.saude.infra.ContextoAplicacao;
import br.unicesumar.aep.saude.infra.MongoConnection;
import br.unicesumar.aep.saude.ui.JanelaPrincipal;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ponto de entrada da PoC.
 *
 * <p>Sem argumentos, conecta ao MongoDB e abre a interface gráfica. Se o banco
 * não estiver acessível, oferece continuar em memória para que a demonstração
 * não fique bloqueada. Argumentos aceitos:</p>
 *
 * <ul>
 *   <li>{@code --memoria} — força o modo em memória, sem tentar conectar;</li>
 *   <li>{@code --console} — executa a demonstração em modo texto.</li>
 * </ul>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (temArgumento(args, "--console")) {
            DemoConsole.executar();
            return;
        }

        boolean forcarMemoria = temArgumento(args, "--memoria");
        SwingUtilities.invokeLater(() -> {
            aplicarAparencia();
            ContextoAplicacao contexto = forcarMemoria
                    ? ContextoAplicacao.emMemoria()
                    : abrirContextoMongo();
            if (contexto == null) {
                return;
            }
            abrirJanela(contexto);
        });
    }

    private static ContextoAplicacao abrirContextoMongo() {
        MongoConnection conexao = null;
        try {
            conexao = new MongoConnection();
            conexao.verificarConexao();
            return ContextoAplicacao.comMongo(conexao);
        } catch (RuntimeException e) {
            fechar(conexao);
            return contextoAlternativo(e);
        }
    }

    private static ContextoAplicacao contextoAlternativo(RuntimeException erro) {
        String mensagem = "Não foi possível conectar ao MongoDB em "
                + MongoConnection.ocultarCredenciais(MongoConnection.uriConfigurada()) + ".\n\n"
                + erro.getMessage() + "\n\n"
                + "Deseja continuar em memória? Os dados não serão persistidos.";

        int resposta = JOptionPane.showConfirmDialog(null, mensagem,
                "MongoDB indisponível", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return resposta == JOptionPane.YES_OPTION ? ContextoAplicacao.emMemoria() : null;
    }

    private static void abrirJanela(ContextoAplicacao contexto) {
        JFrame janela = new JanelaPrincipal(contexto);
        janela.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                contexto.close();
            }
        });
        janela.setVisible(true);
    }

    private static void fechar(MongoConnection conexao) {
        if (conexao != null) {
            try {
                conexao.close();
            } catch (RuntimeException ignorada) {
                // A conexão já falhou; nada mais a fazer aqui.
            }
        }
    }

    private static boolean temArgumento(String[] args, String procurado) {
        for (String argumento : args) {
            if (procurado.equals(argumento)) {
                return true;
            }
        }
        return false;
    }

    private static void aplicarAparencia() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedOperationException
                 | javax.swing.UnsupportedLookAndFeelException e) {
            // Mantém a aparência padrão do Java se nenhuma outra estiver disponível.
        }
    }
}
