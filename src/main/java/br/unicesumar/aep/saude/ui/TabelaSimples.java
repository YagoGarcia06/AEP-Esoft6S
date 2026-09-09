package br.unicesumar.aep.saude.ui;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabela somente leitura que guarda, em paralelo às linhas, o id da entidade
 * exibida — assim a interface sabe sobre qual registro agir.
 */
public class TabelaSimples extends JTable {

    private static final long serialVersionUID = 1L;

    private final transient List<String> ids = new ArrayList<>();
    private final DefaultTableModel modelo;

    public TabelaSimples(String... colunas) {
        modelo = new DefaultTableModel(colunas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
        setModel(modelo);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setRowHeight(22);
        getTableHeader().setReorderingAllowed(false);
    }

    /** Define a largura preferida de cada coluna, na ordem em que foram declaradas. */
    public void definirLarguras(int... larguras) {
        for (int i = 0; i < larguras.length && i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }
    }

    public void limpar() {
        modelo.setRowCount(0);
        ids.clear();
    }

    public void adicionarLinha(String id, Object... valores) {
        modelo.addRow(valores);
        ids.add(id);
    }

    /** Id da linha selecionada, ou {@code null} quando nada está selecionado. */
    public String idSelecionado() {
        int linha = getSelectedRow();
        if (linha < 0) {
            return null;
        }
        return ids.get(convertRowIndexToModel(linha));
    }
}
