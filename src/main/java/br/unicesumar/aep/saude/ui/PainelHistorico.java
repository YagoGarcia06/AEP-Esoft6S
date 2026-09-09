package br.unicesumar.aep.saude.ui;

import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.RegistroClinico;
import br.unicesumar.aep.saude.service.PacienteService;
import br.unicesumar.aep.saude.service.ProfissionalService;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Aba que mostra o histórico clínico consolidado de um paciente — o problema
 * central que a PoC se propõe a resolver.
 */
public class PainelHistorico extends JPanel implements PainelAtualizavel {

    private static final long serialVersionUID = 1L;

    private final transient PacienteService pacienteService;
    private final transient ProfissionalService profissionalService;

    private final JComboBox<ItemPessoa> comboPaciente = new JComboBox<>();
    private final TabelaSimples tabela =
            new TabelaSimples("Data", "Tipo", "Profissional", "Descrição", "Prescrições");

    private boolean recarregando;

    public PainelHistorico(PacienteService pacienteService, ProfissionalService profissionalService) {
        this.pacienteService = Objects.requireNonNull(pacienteService);
        this.profissionalService = Objects.requireNonNull(profissionalService);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topo.add(new JLabel("Paciente:"));
        topo.add(comboPaciente);
        comboPaciente.addActionListener(e -> {
            if (!recarregando) {
                recarregarTabela();
            }
        });

        JPanel corpo = new JPanel(new BorderLayout());
        corpo.setBorder(BorderFactory.createTitledBorder("Histórico clínico"));
        tabela.definirLarguras(90, 90, 140, 330, 200);
        corpo.add(new JScrollPane(tabela), BorderLayout.CENTER);

        add(topo, BorderLayout.NORTH);
        add(corpo, BorderLayout.CENTER);
        atualizar();
    }

    @Override
    public void atualizar() {
        recarregando = true;
        ItemPessoa anterior = (ItemPessoa) comboPaciente.getSelectedItem();
        comboPaciente.removeAllItems();
        for (Paciente paciente : pacienteService.listar()) {
            comboPaciente.addItem(new ItemPessoa(paciente));
        }
        if (anterior != null) {
            for (int i = 0; i < comboPaciente.getItemCount(); i++) {
                if (comboPaciente.getItemAt(i).getId().equals(anterior.getId())) {
                    comboPaciente.setSelectedIndex(i);
                    break;
                }
            }
        }
        recarregando = false;
        recarregarTabela();
    }

    private void recarregarTabela() {
        tabela.limpar();
        ItemPessoa selecionado = (ItemPessoa) comboPaciente.getSelectedItem();
        if (selecionado == null) {
            return;
        }

        Map<String, String> nomes = new HashMap<>();
        for (Profissional profissional : profissionalService.listar()) {
            nomes.put(profissional.getId(), profissional.getNome());
        }

        for (RegistroClinico registro : pacienteService.historico(selecionado.getId())) {
            tabela.adicionarLinha(null,
                    registro.getData().format(Formatos.DATA),
                    registro.getTipo().getDescricao(),
                    nomes.getOrDefault(registro.getProfissionalId(), "(removido)"),
                    registro.getDescricao(),
                    String.join("; ", registro.getPrescricoes()));
        }
    }
}
