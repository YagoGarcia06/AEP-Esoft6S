package br.unicesumar.aep.saude.ui;

import br.unicesumar.aep.saude.model.ItemAgendaDia;
import br.unicesumar.aep.saude.model.ResumoAtendimentos;
import br.unicesumar.aep.saude.service.RelatorioService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Aba de relatórios. No modo MongoDB, cada consulta é um aggregation pipeline
 * executado no banco ($lookup para trazer nomes, $group para os totais).
 */
public class PainelRelatorios extends JPanel implements PainelAtualizavel {

    private static final long serialVersionUID = 1L;

    private static final String POR_PROFISSIONAL = "Por profissional";
    private static final String POR_TIPO = "Por tipo de atendimento";

    private final transient RelatorioService relatorioService;

    private final JTextField campoDia = new JTextField(12);
    private final JTextField campoInicio = new JTextField(12);
    private final JTextField campoFim = new JTextField(12);
    private final JComboBox<String> comboAgrupamento =
            new JComboBox<>(new String[] {POR_PROFISSIONAL, POR_TIPO});

    private final TabelaSimples tabelaAgenda = new TabelaSimples(
            "Horário", "Paciente", "Profissional", "Tipo", "Status");
    private final TabelaSimples tabelaResumo = new TabelaSimples(
            "Grupo", "Agendamentos", "Realizados", "Faltas", "Cancelados", "Pendentes", "% faltas");

    public PainelRelatorios(RelatorioService relatorioService) {
        this.relatorioService = Objects.requireNonNull(relatorioService);

        LocalDate hoje = LocalDate.now();
        campoDia.setText(hoje.plusDays(1).format(Formatos.DATA));
        campoInicio.setText(hoje.withDayOfMonth(1).format(Formatos.DATA));
        campoFim.setText(hoje.withDayOfMonth(hoje.lengthOfMonth()).format(Formatos.DATA));

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(construirFormularios(), BorderLayout.WEST);
        add(construirTabelas(), BorderLayout.CENTER);
    }

    private JPanel construirFormularios() {
        PainelFormulario agenda = new PainelFormulario("Agenda do dia");
        agenda.adicionar("Dia (dd/MM/aaaa):", campoDia);
        JButton gerarAgenda = new JButton("Gerar agenda");
        gerarAgenda.addActionListener(e -> gerarAgenda());
        agenda.adicionarLinhaInteira(gerarAgenda);

        PainelFormulario resumo = new PainelFormulario("Resumo do período");
        resumo.adicionar("Início (dd/MM/aaaa):", campoInicio);
        resumo.adicionar("Fim (dd/MM/aaaa):", campoFim);
        resumo.adicionar("Agrupar:", comboAgrupamento);
        JButton gerarResumo = new JButton("Gerar resumo");
        gerarResumo.addActionListener(e -> gerarResumo());
        resumo.adicionarLinhaInteira(gerarResumo);

        JPanel coluna = new JPanel(new BorderLayout(0, 10));
        coluna.add(agenda, BorderLayout.NORTH);
        coluna.add(resumo, BorderLayout.CENTER);

        JPanel container = new JPanel(new BorderLayout());
        container.add(coluna, BorderLayout.NORTH);
        return container;
    }

    private JPanel construirTabelas() {
        tabelaAgenda.definirLarguras(60, 160, 160, 80, 110);
        tabelaResumo.definirLarguras(170, 90, 80, 60, 80, 75, 70);

        JPanel painelAgenda = new JPanel(new BorderLayout());
        painelAgenda.setBorder(BorderFactory.createTitledBorder("Agenda do dia"));
        painelAgenda.add(new JScrollPane(tabelaAgenda), BorderLayout.CENTER);

        JPanel painelResumo = new JPanel(new BorderLayout());
        painelResumo.setBorder(BorderFactory.createTitledBorder("Resumo de atendimentos"));
        painelResumo.add(new JScrollPane(tabelaResumo), BorderLayout.CENTER);

        JPanel painel = new JPanel(new GridLayout(2, 1, 0, 10));
        painel.add(painelAgenda);
        painel.add(painelResumo);
        return painel;
    }

    private void gerarAgenda() {
        try {
            LocalDate dia = Formatos.lerData(campoDia.getText(), "Dia");
            List<ItemAgendaDia> itens = relatorioService.agendaDoDia(dia);
            tabelaAgenda.limpar();
            for (int i = 0; i < itens.size(); i++) {
                ItemAgendaDia item = itens.get(i);
                tabelaAgenda.adicionarLinha(String.valueOf(i),
                        item.getDataHora().format(Formatos.HORA),
                        item.getPacienteNome(),
                        item.getProfissionalNome(),
                        item.getTipo().getDescricao(),
                        item.getStatus());
            }
            if (itens.isEmpty()) {
                Mensagens.aviso(this, "Nenhum atendimento em " + dia.format(Formatos.DATA) + ".");
            }
        } catch (RuntimeException e) {
            Mensagens.erro(this, e);
        }
    }

    private void gerarResumo() {
        try {
            LocalDate inicio = Formatos.lerData(campoInicio.getText(), "Início");
            LocalDate fim = Formatos.lerData(campoFim.getText(), "Fim");
            List<ResumoAtendimentos> resumos = POR_TIPO.equals(comboAgrupamento.getSelectedItem())
                    ? relatorioService.resumoPorTipo(inicio, fim)
                    : relatorioService.resumoPorProfissional(inicio, fim);
            tabelaResumo.limpar();
            for (ResumoAtendimentos resumo : resumos) {
                tabelaResumo.adicionarLinha(resumo.getGrupo(),
                        resumo.getGrupo(),
                        resumo.getTotal(),
                        resumo.getRealizados(),
                        resumo.getFaltas(),
                        resumo.getCancelados(),
                        resumo.getPendentes(),
                        String.format(Formatos.PT_BR, "%.1f%%", resumo.getPercentualFaltas()));
            }
            if (resumos.isEmpty()) {
                Mensagens.aviso(this, "Nenhum atendimento no período.");
            }
        } catch (RuntimeException e) {
            Mensagens.erro(this, e);
        }
    }

    /** Os relatórios são gerados sob demanda; ao voltar para a aba, limpa resultados antigos. */
    @Override
    public void atualizar() {
        tabelaAgenda.limpar();
        tabelaResumo.limpar();
    }
}
