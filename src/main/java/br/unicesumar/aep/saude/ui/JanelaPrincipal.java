package br.unicesumar.aep.saude.ui;

import br.unicesumar.aep.saude.infra.ContextoAplicacao;
import br.unicesumar.aep.saude.service.AgendamentoService;
import br.unicesumar.aep.saude.service.PacienteService;
import br.unicesumar.aep.saude.service.ProfissionalService;
import br.unicesumar.aep.saude.service.RelatorioService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Janela principal da demonstração. Monta os repositórios e serviços uma única
 * vez e distribui as dependências entre as abas.
 */
public class JanelaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient PacienteService pacienteService;
    private final transient ProfissionalService profissionalService;
    private final transient AgendamentoService agendamentoService;
    private final transient RelatorioService relatorioService;

    private final JTabbedPane abas = new JTabbedPane();
    private final String descricaoArmazenamento;

    public JanelaPrincipal(ContextoAplicacao contexto) {
        this.pacienteService = contexto.getPacienteService();
        this.profissionalService = contexto.getProfissionalService();
        this.agendamentoService = contexto.getAgendamentoService();
        this.relatorioService = contexto.getRelatorioService();
        this.descricaoArmazenamento = contexto.getDescricaoArmazenamento();

        setTitle("PoC — Agendamento em Saúde | AEP 2026.2 — 6S | ODS 3");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(1020, 640));
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);

        abas.addTab("Pacientes", new PainelPacientes(pacienteService));
        abas.addTab("Profissionais", new PainelProfissionais(profissionalService));
        abas.addTab("Agendamentos",
                new PainelAgendamentos(agendamentoService, pacienteService, profissionalService));
        abas.addTab("Histórico", new PainelHistorico(pacienteService, profissionalService));
        abas.addTab("Relatórios", new PainelRelatorios(relatorioService));

        abas.addChangeListener(e -> atualizarAbaSelecionada());

        add(construirCabecalho(), BorderLayout.NORTH);
        add(abas, BorderLayout.CENTER);
    }

    private JPanel construirCabecalho() {
        JLabel titulo = new JLabel("Agendamento de consultas e exames com histórico do paciente");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 14f));

        JButton exemplo = new JButton("Carregar dados de exemplo");
        exemplo.addActionListener(e -> carregarDadosExemplo());

        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));

        JLabel armazenamento = new JLabel("Armazenamento: " + descricaoArmazenamento);
        armazenamento.setFont(armazenamento.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel titulos = new JPanel(new BorderLayout());
        titulos.add(titulo, BorderLayout.NORTH);
        titulos.add(armazenamento, BorderLayout.SOUTH);
        painel.add(titulos, BorderLayout.WEST);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acoes.add(exemplo);
        painel.add(acoes, BorderLayout.EAST);
        return painel;
    }

    private void carregarDadosExemplo() {
        try {
            DadosExemplo.carregar(pacienteService, profissionalService, agendamentoService);
            atualizarTodas();
            Mensagens.sucesso(this, "Dados de exemplo carregados.");
        } catch (RuntimeException e) {
            Mensagens.erro(this, e);
        }
    }

    private void atualizarAbaSelecionada() {
        Component selecionada = abas.getSelectedComponent();
        if (selecionada instanceof PainelAtualizavel) {
            ((PainelAtualizavel) selecionada).atualizar();
        }
    }

    private void atualizarTodas() {
        for (int i = 0; i < abas.getTabCount(); i++) {
            if (abas.getComponentAt(i) instanceof PainelAtualizavel) {
                ((PainelAtualizavel) abas.getComponentAt(i)).atualizar();
            }
        }
    }
}
