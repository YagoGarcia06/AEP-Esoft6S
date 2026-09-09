package br.unicesumar.aep.saude.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Paciente atendido pela unidade de saúde. É a entidade raiz da coleção
 * "pacientes" e carrega o histórico clínico como lista de subdocumentos.
 */
public class Paciente extends Pessoa {

    private final LocalDate dataNascimento;
    private final List<RegistroClinico> historico = new ArrayList<>();
    private Endereco endereco;

    public Paciente(String nome, String cpf, LocalDate dataNascimento,
                    Contato contato, Endereco endereco) {
        this(null, nome, cpf, dataNascimento, contato, endereco);
    }

    public Paciente(String id, String nome, String cpf, LocalDate dataNascimento,
                    Contato contato, Endereco endereco) {
        super(id, nome, cpf, contato);
        Objects.requireNonNull(dataNascimento, "Data de nascimento é obrigatória.");
        if (dataNascimento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data de nascimento não pode estar no futuro.");
        }
        this.dataNascimento = dataNascimento;
        setEndereco(endereco);
    }

    @Override
    public String tipo() {
        return "Paciente";
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = Objects.requireNonNull(endereco, "Endereço é obrigatório.");
    }

    public int idadeEm(LocalDate referencia) {
        Objects.requireNonNull(referencia, "Data de referência é obrigatória.");
        if (referencia.isBefore(dataNascimento)) {
            throw new IllegalArgumentException("Data de referência anterior ao nascimento.");
        }
        return Period.between(dataNascimento, referencia).getYears();
    }

    public int idade() {
        return idadeEm(LocalDate.now());
    }

    public void adicionarRegistro(RegistroClinico registro) {
        Objects.requireNonNull(registro, "Registro clínico é obrigatório.");
        historico.add(registro);
    }

    /** Cópia somente leitura: o histórico só muda por adicionarRegistro. */
    public List<RegistroClinico> getHistorico() {
        return Collections.unmodifiableList(historico);
    }

    public boolean possuiHistorico() {
        return !historico.isEmpty();
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public Endereco getEndereco() {
        return endereco;
    }
}
