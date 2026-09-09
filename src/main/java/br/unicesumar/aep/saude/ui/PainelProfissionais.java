package br.unicesumar.aep.saude.ui;

import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Disponibilidade;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.service.ProfissionalService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.DayOfWeek;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aba de cadastro de profissionais e de suas janelas de atendimento.
 */
public class PainelProfissionais extends JPanel implements PainelAtualizavel {

    private static final long serialVersionUID = 1L;

    private final transient ProfissionalService profissionalService;

    private final JTextField campoNome = new JTextField(18);
    private final JTextField campoCpf = new JTextField(18);
    private final JTextField campoRegistro = new JTextField(18);
    private final JTextField campoEmail = new JTextField(18);
    private final JTextField campoTelefone = new JTextField(18);
    private final JTextField campoEspecialidade = new JTextField(18);

    private final JComboBox<DayOfWeek> comboDia = new JComboBox<>(DayOfWeek.values());
    private final JTextField campoHoraInicio = new JTextField("08:00", 6);
    private final JTextField campoHoraFim = new JTextField("12:00", 6);

    private final TabelaSimples tabela =
            new TabelaSimples("Nome", "Registro", "Especialidades", "Disponibilidades");

    public PainelProfissionais(ProfissionalService profissionalService) {
        this.profissionalService = Objects.requireNonNull(profissionalService);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(construirFormulario(), BorderLayout.WEST);
        add(construirTabela(), BorderLayout.CENTER);
        atualizar();
    }

    private JPanel construirFormulario() {
        PainelFormulario cadastro = new PainelFormulario("Novo profissional");
        cadastro.adicionar("Nome:", campoNome);
        cadastro.adicionar("CPF:", campoCpf);
        cadastro.adicionar("Registro (CRM/CRO):", campoRegistro);
        cadastro.adicionar("E-mail:", campoEmail);
        cadastro.adicionar("Telefone:", campoTelefone);
        cadastro.adicionar("Especialidade:", campoEspecialidade);

        JButton cadastrar = new JButton("Cadastrar profissional");
        cadastrar.addActionListener(e -> cadastrar());
        cadastro.adicionarLinhaInteira(cadastrar);

        PainelFormulario janela = new PainelFormulario("Disponibilidade do selecionado");
        comboDia.setRenderer(new RenderizadorDia());
        janela.adicionar("Dia:", comboDia);
        janela.adicionar("Início (HH:mm):", campoHoraInicio);
        janela.adicionar("Fim (HH:mm):", campoHoraFim);

        JButton adicionar = new JButton("Adicionar disponibilidade");
        adicionar.addActionListener(e -> adicionarDisponibilidade());
        janela.adicionarLinhaInteira(adicionar);

        JPanel formularios = new JPanel(new BorderLayout(0, 10));
        formularios.add(cadastro, BorderLayout.NORTH);
        formularios.add(janela, BorderLayout.CENTER);

        JPanel container = new JPanel(new BorderLayout());
        container.add(formularios, BorderLayout.NORTH);
        return container;
    }

    private JPanel construirTabela() {
        JPanel painel = new JPanel(new BorderLayout(6, 6));
        painel.setBorder(BorderFactory.createTitledBorder("Profissionais cadastrados"));
        tabela.definirLarguras(160, 110, 130, 220);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);

        JButton remover = new JButton("Remover selecionado");
        remover.addActionListener(e -> remover());

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(remover);
        painel.add(rodape, BorderLayout.SOUTH);
        return painel;
    }

    private void cadastrar() {
        try {
            Profissional profissional = new Profissional(
                    Formatos.exigirPreenchido(campoNome.getText(), "Nome"),
                    Formatos.exigirPreenchido(campoCpf.getText(), "CPF"),
                    Formatos.exigirPreenchido(campoRegistro.getText(), "Registro"),
                    new Contato(campoEmail.getText().trim(), campoTelefone.getText().trim()));

            if (!campoEspecialidade.getText().isBlank()) {
                profissional.adicionarEspecialidade(campoEspecialidade.getText());
            }

            profissionalService.cadastrar(profissional);
            limparFormulario();
            atualizar();
            Mensagens.sucesso(this, "Profissional cadastrado com sucesso.");
        } catch (RuntimeException e) {
            Mensagens.erro(this, e);
        }
    }

    private void adicionarDisponibilidade() {
        String id = tabela.idSelecionado();
        if (id == null) {
            Mensagens.aviso(this, "Selecione um profissional na tabela.");
            return;
        }
        try {
            Disponibilidade disponibilidade = new Disponibilidade(
                    (DayOfWeek) comboDia.getSelectedItem(),
                    Formatos.lerHora(campoHoraInicio.getText(), "Início"),
                    Formatos.lerHora(campoHoraFim.getText(), "Fim"));

            profissionalService.adicionarDisponibilidade(id, disponibilidade);
            atualizar();
        } catch (RuntimeException e) {
            Mensagens.erro(this, e);
        }
    }

    private void remover() {
        String id = tabela.idSelecionado();
        if (id == null) {
            Mensagens.aviso(this, "Selecione um profissional na tabela.");
            return;
        }
        if (Mensagens.confirmar(this, "Remover o profissional selecionado?")) {
            profissionalService.remover(id);
            atualizar();
        }
    }

    private void limparFormulario() {
        campoNome.setText("");
        campoCpf.setText("");
        campoRegistro.setText("");
        campoEmail.setText("");
        campoTelefone.setText("");
        campoEspecialidade.setText("");
    }

    @Override
    public void atualizar() {
        tabela.limpar();
        for (Profissional profissional : profissionalService.listar()) {
            String especialidades = String.join(", ", profissional.getEspecialidades());
            String disponibilidades = profissional.getDisponibilidades().stream()
                    .map(d -> Formatos.nomeDoDia(d.getDiaSemana()) + " "
                            + d.getHoraInicio() + "-" + d.getHoraFim())
                    .collect(Collectors.joining("; "));

            tabela.adicionarLinha(profissional.getId(),
                    profissional.getNome(),
                    profissional.getRegistroProfissional(),
                    especialidades,
                    disponibilidades);
        }
    }

    /** Exibe os dias da semana em português na caixa de seleção. */
    private static class RenderizadorDia extends javax.swing.DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> lista,
                Object valor, int indice, boolean selecionado, boolean comFoco) {
            super.getListCellRendererComponent(lista, valor, indice, selecionado, comFoco);
            if (valor instanceof DayOfWeek) {
                setText(Formatos.nomeDoDia((DayOfWeek) valor));
            }
            return this;
        }
    }
}
