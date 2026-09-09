package br.unicesumar.aep.saude.ui;

import br.unicesumar.aep.saude.model.Contato;
import br.unicesumar.aep.saude.model.Endereco;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.service.PacienteService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Aba de cadastro e listagem de pacientes.
 */
public class PainelPacientes extends JPanel implements PainelAtualizavel {

    private static final long serialVersionUID = 1L;

    private final transient PacienteService pacienteService;

    private final JTextField campoNome = new JTextField(18);
    private final JTextField campoCpf = new JTextField(18);
    private final JTextField campoNascimento = new JTextField(18);
    private final JTextField campoEmail = new JTextField(18);
    private final JTextField campoTelefone = new JTextField(18);
    private final JTextField campoRua = new JTextField(18);
    private final JTextField campoCidade = new JTextField(18);
    private final JTextField campoEstado = new JTextField(18);

    private final TabelaSimples tabela =
            new TabelaSimples("Nome", "CPF", "Idade", "Cidade", "Atendimentos");

    public PainelPacientes(PacienteService pacienteService) {
        this.pacienteService = Objects.requireNonNull(pacienteService);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(construirFormulario(), BorderLayout.WEST);
        add(construirTabela(), BorderLayout.CENTER);
        atualizar();
    }

    private JPanel construirFormulario() {
        PainelFormulario formulario = new PainelFormulario("Novo paciente");
        formulario.adicionar("Nome:", campoNome);
        formulario.adicionar("CPF:", campoCpf);
        formulario.adicionar("Nascimento (dd/MM/aaaa):", campoNascimento);
        formulario.adicionar("E-mail:", campoEmail);
        formulario.adicionar("Telefone:", campoTelefone);
        formulario.adicionar("Rua:", campoRua);
        formulario.adicionar("Cidade:", campoCidade);
        formulario.adicionar("UF:", campoEstado);

        JButton cadastrar = new JButton("Cadastrar paciente");
        cadastrar.addActionListener(e -> cadastrar());

        JButton limpar = new JButton("Limpar");
        limpar.addActionListener(e -> limparFormulario());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        botoes.add(cadastrar);
        botoes.add(limpar);
        formulario.adicionarLinhaInteira(botoes);

        JPanel container = new JPanel(new BorderLayout());
        container.add(formulario, BorderLayout.NORTH);
        return container;
    }

    private JPanel construirTabela() {
        JPanel painel = new JPanel(new BorderLayout(6, 6));
        painel.setBorder(BorderFactory.createTitledBorder("Pacientes cadastrados"));
        tabela.definirLarguras(180, 130, 60, 120, 100);
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
            Paciente paciente = new Paciente(
                    Formatos.exigirPreenchido(campoNome.getText(), "Nome"),
                    Formatos.exigirPreenchido(campoCpf.getText(), "CPF"),
                    Formatos.lerData(campoNascimento.getText(), "Nascimento"),
                    new Contato(campoEmail.getText().trim(), campoTelefone.getText().trim()),
                    new Endereco(campoRua.getText().trim(), campoCidade.getText().trim(),
                            campoEstado.getText().trim()));

            pacienteService.cadastrar(paciente);
            limparFormulario();
            atualizar();
            Mensagens.sucesso(this, "Paciente cadastrado com sucesso.");
        } catch (RuntimeException e) {
            Mensagens.erro(this, e);
        }
    }

    private void remover() {
        String id = tabela.idSelecionado();
        if (id == null) {
            Mensagens.aviso(this, "Selecione um paciente na tabela.");
            return;
        }
        if (Mensagens.confirmar(this, "Remover o paciente selecionado?")) {
            pacienteService.remover(id);
            atualizar();
        }
    }

    private void limparFormulario() {
        campoNome.setText("");
        campoCpf.setText("");
        campoNascimento.setText("");
        campoEmail.setText("");
        campoTelefone.setText("");
        campoRua.setText("");
        campoCidade.setText("");
        campoEstado.setText("");
    }

    @Override
    public void atualizar() {
        tabela.limpar();
        for (Paciente paciente : pacienteService.listar()) {
            tabela.adicionarLinha(paciente.getId(),
                    paciente.getNome(),
                    paciente.getCpfFormatado(),
                    paciente.idadeEm(LocalDate.now()),
                    paciente.getEndereco().getCidade(),
                    paciente.getHistorico().size());
        }
    }
}
