package br.unicesumar.aep.saude.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Formulário em grade que empilha pares rótulo/campo, evitando repetir a
 * configuração de layout em cada aba.
 */
public class PainelFormulario extends JPanel {

    private static final long serialVersionUID = 1L;

    private int linha;

    public PainelFormulario(String titulo) {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder(titulo));
    }

    public <T extends JComponent> T adicionar(String rotulo, T campo) {
        GridBagConstraints rotuloGbc = base();
        rotuloGbc.gridx = 0;
        rotuloGbc.gridy = linha;
        rotuloGbc.weightx = 0;
        add(new JLabel(rotulo), rotuloGbc);

        GridBagConstraints campoGbc = base();
        campoGbc.gridx = 1;
        campoGbc.gridy = linha;
        campoGbc.weightx = 1;
        add(campo, campoGbc);

        linha++;
        return campo;
    }

    public void adicionarLinhaInteira(JComponent componente) {
        GridBagConstraints gbc = base();
        gbc.gridx = 0;
        gbc.gridy = linha;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        add(componente, gbc);
        linha++;
    }

    private GridBagConstraints base() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }
}
