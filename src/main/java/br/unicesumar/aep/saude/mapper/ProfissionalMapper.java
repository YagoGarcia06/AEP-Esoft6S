package br.unicesumar.aep.saude.mapper;

import br.unicesumar.aep.saude.model.Disponibilidade;
import br.unicesumar.aep.saude.model.Profissional;
import org.bson.Document;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapeia a coleção {@code profissionais}, com {@code especialidades} como lista
 * simples e {@code disponibilidades} como lista de subdocumentos.
 */
public class ProfissionalMapper implements DocumentMapper<Profissional> {

    @Override
    public Document paraDocumento(Profissional profissional) {
        List<Document> disponibilidades = new ArrayList<>();
        for (Disponibilidade disponibilidade : profissional.getDisponibilidades()) {
            disponibilidades.add(new Document("diaSemana", disponibilidade.getDiaSemana().name())
                    .append("horaInicio", disponibilidade.getHoraInicio().toString())
                    .append("horaFim", disponibilidade.getHoraFim().toString()));
        }

        return new Document("_id", profissional.getId())
                .append("nome", profissional.getNome())
                .append("cpf", profissional.getCpf())
                .append("registroProfissional", profissional.getRegistroProfissional())
                .append("contato", ContatoMapper.paraDocumento(profissional.getContato()))
                .append("especialidades", new ArrayList<>(profissional.getEspecialidades()))
                .append("disponibilidades", disponibilidades);
    }

    @Override
    public Profissional paraEntidade(Document documento) {
        Profissional profissional = new Profissional(
                documento.getString("_id"),
                documento.getString("nome"),
                documento.getString("cpf"),
                documento.getString("registroProfissional"),
                ContatoMapper.paraContato(documento.get("contato", Document.class)));

        List<String> especialidades = documento.getList("especialidades", String.class);
        if (especialidades != null) {
            especialidades.forEach(profissional::adicionarEspecialidade);
        }

        List<Document> disponibilidades = documento.getList("disponibilidades", Document.class);
        if (disponibilidades != null) {
            for (Document disponibilidade : disponibilidades) {
                profissional.adicionarDisponibilidade(new Disponibilidade(
                        DayOfWeek.valueOf(disponibilidade.getString("diaSemana")),
                        LocalTime.parse(disponibilidade.getString("horaInicio")),
                        LocalTime.parse(disponibilidade.getString("horaFim"))));
            }
        }
        return profissional;
    }
}
