package br.unicesumar.aep.saude.ui;

import br.unicesumar.aep.saude.model.Agendamento;
import br.unicesumar.aep.saude.model.Paciente;
import br.unicesumar.aep.saude.model.Profissional;
import br.unicesumar.aep.saude.model.TipoAtendimento;
import br.unicesumar.aep.saude.service.AgendamentoService;
import br.unicesumar.aep.saude.service.PacienteService;
import br.unicesumar.aep.saude.service.ProfissionalService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Aba principal da demonstração: agenda atendimentos e aplica, na prática,
 * as regras de data futura, disponibilidade e conflito de horário.
 */
public class PainelAgendamentos extends JPanel implements PainelAtualizavel {

    private static final long serialVersionUID = 1L;

    private final transient AgendamentoService agendamentoService;
    private final transient PacienteService pacienteService;
    private final transient ProfissionalService profissionalService;

    private final JComboBox<ItemPessoa> comboPaciente = new JComboBox<>();
    private final JComboBox<ItemPessoa> comboProfissional = new JComboBox<>();
    private final JComboBox<TipoAtendimento> comboTipo =
            new JComboBox<>(TipoAtendimento.values());
    private final JTextField campoData = new JTextField(12);
    private final JTextField campoHora = new JTextField("09:00", 12);

    private final TabelaSimples tabela = new TabelaSimples(
            "Data e hora", "Paciente", "Profissional", "Tipo", "Duração", "Status");

    public PainelAgendamentos(AgendamentoService agendamentoService,
                              PacienteService pacienteService,
                              ProfissionalService profissionalService) {
        this.agendamentoService = Objects.requireNonNull(agendamentoService);
        this.pacienteService = Objects.requireNonNull(pacienteService);
        this.profissionalService = Objects.requireNonNull(profissionalService);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        campoData.setText(LocalDate.now().plusDays(1).format(Formatos.DATA));
        add(construirFormulario(), BorderLayout.WEST);
        add(construirTabela(), BorderLayout.CENTER);
        atualizar();
    }

    private JPanel construirFormulario() {
        PainelFormulario formulario = new PainelFormulario("Novo agendamento");
        formulario.adicionar("Paciente:", comboPaciente);
        formulario.adicionar("Profissional:", comboProfissional);
        formulario.adicionar("Data (dd/MM/aaaa):", campoData);
        formulario.adicionar("Hora (HH:mm):", campoHora);
        formulario.adicionar("Tipo:", comboTipo);

        JButton agendar = new JButton("Agendar");
        agendar.addActionListener(e -> agendar());
        formulario.adicionarLinhaInteira(agendar);

        JPanel container = new JPanel(new BorderLayout());
        container.add(formulario, BorderLayout.NORTH);
        return container;
    }

    private JPanel construirTabela() {
        JPanel painel = new JPanel(new BorderLayout(6, 6));
        painel.setBorder(BorderFactory.createTitledBorder("Agenda"));
        tabela.definirLarguras(130, 140, 140, 80, 70, 90);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);

        JButton confirmar = new JButton("Confirmar");
        confirmar.addActionListener(e -> executarNoSelecionado(
                agendamentoService::confirmar, "Presença confirmada."));

        JButton remarcar = new JButton("Remarcar");
        remarcar.addActionListener(e -> remarcar());

        JButton concluir = new JButton("Concluir atendimento");
        concluir.addActionListener(e -> concluir());

        JButton falta = new JButton("Registrar falta");
        falta.addActionListener(e -> executarNoSelecionado(
                agendamentoService::registrarFalta, "Falta registrada."));

        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> executarNoSelecionado(
                agendamentoService::cancelar, "Atendimento cancelado."));

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(confirmar);
        rodape.add(remarcar);
        rodape.add(concluir);
        rodape.add(falta);
        rodape.add(cancelar);
        painel.add(rodape, BorderLayout.SOUTH);
        return painel;
    }

    private void agendar() {
        ItemPessoa paciente = (ItemPessoa) comboPaciente.getSelectedItem();
        ItemPessoa profissional = (ItemPessoa) comboProfissional.getSelectedItem();
        if (paciente == null || profissional == null) {
            Mensagens.aviso(this, "Cadastre ao menos um paciente e um profissional.");
            return;
        }
        try {
            LocalDateTime dataHora = LocalDateTime.of(
                    Formatos.lerData(campoData.getText(), "Data"),
                    Formatos.lerHora(campoHora.getText(), "Hora"));

            agendamentoService.agendar(paciente.getId(), profissional.getId(),
                    dataHora, (TipoAtendimento) comboTipo.getSelectedItem());

            atualizar();
            Mensagens.sucesso(this, "Atendimento agendado com sucesso.");
        } catch (RuntimeException e) {
            Mensagens.erro(this, e);
        }
    }

    private void concluir() {
        String id = tabela.idSelecionado();
        if (id == null) {
            Mensagens.aviso(this, "Selecione um atendimento na agenda.");
            return;
        }
        String descricao = JOptionPane.showInputDialog(this,
                "Descrição do atendimento:", "Concluir atendimento", JOptionPane.QUESTION_MESSAGE);
        if (descricao == null) {
            return;
        }
        String prescricoes = JOptionPane.showInputDialog(this,
                "Prescrições (separadas por ';'):", "Concluir atendimento", JOptionPane.QUESTION_MESSAGE);

        try {
            agendamentoService.realizar(id, descricao, separarPrescricoes(prescricoes));
            atualizar();
            Mensagens.sucesso(this, "Atendimento concluído e registrado no histórico do paciente.");
        } catch (RuntimeException e) {
            Mensagens.erro(this, e);
        }
    }

    private List<String> separarPrescricoes(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(texto.split(";"))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .toList();
    }

    /** Aplica uma ação simples de mudança de status no atendimento selecionado. */
    private void executarNoSelecionado(Consumer<String> acao, String mensagemSucesso) {
        String id = tabela.idSelecionado();
        if (id == null) {
            Mensagens.aviso(this, "Selecione um atendimento na agenda.");
            return;
        }
        try {
            acao.accept(id);
            atualizar();
            Mensagens.sucesso(this, mensagemSucesso);
        } catch (RuntimeException e) {
            Mensagens.erro(this, e);
        }
    }

    private void remarcar() {
        String id = tabela.idSelecionado();
        if (id == null) {
            Mensagens.aviso(this, "Selecione um atendimento na agenda.");
            return;
        }
        String data = JOptionPane.showInputDialog(this,
                "Nova data (dd/MM/aaaa):", "Remarcar atendimento", JOptionPane.QUESTION_MESSAGE);
        if (data == null) {
            return;
        }
        String hora = JOptionPane.showInputDialog(this,
                "Novo horário (HH:mm):", "Remarcar atendimento", JOptionPane.QUESTION_MESSAGE);
        if (hora == null) {
            return;
        }
        try {
            LocalDateTime novaDataHora = LocalDateTime.of(
                    Formatos.lerData(data, "Data"), Formatos.lerHora(hora, "Hora"));
            agendamentoService.remarcar(id, novaDataHora);
            atualizar();
            Mensagens.sucesso(this, "Atendimento remarcado. Confirme novamente com o paciente.");
        } catch (RuntimeException e) {
            Mensagens.erro(this, e);
        }
    }

    @Override
    public void atualizar() {
        recarregarCombos();
        recarregarTabela();
    }

    private void recarregarCombos() {
        ItemPessoa pacienteAtual = (ItemPessoa) comboPaciente.getSelectedItem();
        ItemPessoa profissionalAtual = (ItemPessoa) comboProfissional.getSelectedItem();

        comboPaciente.removeAllItems();
        for (Paciente paciente : pacienteService.listar()) {
            comboPaciente.addItem(new ItemPessoa(paciente));
        }
        comboProfissional.removeAllItems();
        for (Profissional profissional : profissionalService.listar()) {
            comboProfissional.addItem(new ItemPessoa(profissional));
        }

        reselecionar(comboPaciente, pacienteAtual);
        reselecionar(comboProfissional, profissionalAtual);
    }

    private void reselecionar(JComboBox<ItemPessoa> combo, ItemPessoa anterior) {
        if (anterior == null) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getId().equals(anterior.getId())) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void recarregarTabela() {
        Map<String, String> nomes = new HashMap<>();
        pacienteService.listar().forEach(p -> nomes.put(p.getId(), p.getNome()));
        profissionalService.listar().forEach(p -> nomes.put(p.getId(), p.getNome()));

        tabela.limpar();
        for (Agendamento agendamento : agendamentoService.listarTodos()) {
            tabela.adicionarLinha(agendamento.getId(),
                    agendamento.getDataHora().format(Formatos.DATA_HORA),
                    nomes.getOrDefault(agendamento.getPacienteId(), "(removido)"),
                    nomes.getOrDefault(agendamento.getProfissionalId(), "(removido)"),
                    agendamento.getTipo().getDescricao(),
                    agendamento.getDuracaoMinutos() + " min",
                    agendamento.getStatus());
        }
    }
}
