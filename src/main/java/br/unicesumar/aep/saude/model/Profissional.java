package br.unicesumar.aep.saude.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Profissional de saúde que atende consultas e exames. É a entidade raiz da
 * coleção "profissionais" e guarda especialidades e janelas de disponibilidade
 * como subdocumentos.
 */
public class Profissional extends Pessoa {

    private final String registroProfissional;
    private final List<String> especialidades = new ArrayList<>();
    private final List<Disponibilidade> disponibilidades = new ArrayList<>();

    public Profissional(String nome, String cpf, String registroProfissional, Contato contato) {
        this(null, nome, cpf, registroProfissional, contato);
    }

    public Profissional(String id, String nome, String cpf,
                        String registroProfissional, Contato contato) {
        super(id, nome, cpf, contato);
        if (registroProfissional == null || registroProfissional.isBlank()) {
            throw new IllegalArgumentException("Registro profissional (CRM/CRO/etc.) é obrigatório.");
        }
        this.registroProfissional = registroProfissional.trim().toUpperCase();
    }

    @Override
    public String tipo() {
        return "Profissional";
    }

    public void adicionarEspecialidade(String especialidade) {
        if (especialidade == null || especialidade.isBlank()) {
            throw new IllegalArgumentException("Especialidade é obrigatória.");
        }
        String normalizada = especialidade.trim();
        if (!possuiEspecialidade(normalizada)) {
            especialidades.add(normalizada);
        }
    }

    public boolean possuiEspecialidade(String especialidade) {
        if (especialidade == null) {
            return false;
        }
        return especialidades.stream().anyMatch(e -> e.equalsIgnoreCase(especialidade.trim()));
    }

    public void adicionarDisponibilidade(Disponibilidade disponibilidade) {
        Objects.requireNonNull(disponibilidade, "Disponibilidade é obrigatória.");
        if (!disponibilidades.contains(disponibilidade)) {
            disponibilidades.add(disponibilidade);
        }
    }

    /**
     * Verifica se o intervalo pedido cabe em alguma janela de atendimento.
     */
    public boolean atendeEm(LocalDateTime inicio, LocalDateTime fim) {
        return disponibilidades.stream().anyMatch(d -> d.cobre(inicio, fim));
    }

    public String getRegistroProfissional() {
        return registroProfissional;
    }

    public List<String> getEspecialidades() {
        return Collections.unmodifiableList(especialidades);
    }

    public List<Disponibilidade> getDisponibilidades() {
        return Collections.unmodifiableList(disponibilidades);
    }

    @Override
    public String descricaoResumida() {
        return super.descricaoResumida() + " - " + registroProfissional;
    }
}
