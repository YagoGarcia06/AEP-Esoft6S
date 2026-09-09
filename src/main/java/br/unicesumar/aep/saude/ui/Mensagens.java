package br.unicesumar.aep.saude.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Diálogos padronizados da interface. Traduz exceções do domínio em mensagens
 * legíveis, sem espalhar try/catch visual pelas abas.
 */
public final class Mensagens {

    private Mensagens() {
    }

    public static void erro(Component pai, Exception e) {
        JOptionPane.showMessageDialog(pai, e.getMessage(),
                "Não foi possível concluir", JOptionPane.WARNING_MESSAGE);
    }

    public static void sucesso(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Pronto", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void aviso(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean confirmar(Component pai, String mensagem) {
        return JOptionPane.showConfirmDialog(pai, mensagem, "Confirmação",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
