package br.unicesumar.aep.saude.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Conversões entre o texto digitado na interface e os tipos de data e hora do domínio.
 */
public final class Formatos {

    public static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatos() {
    }

    public static LocalDate lerData(String texto, String campo) {
        try {
            return LocalDate.parse(exigirPreenchido(texto, campo), DATA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("O campo " + campo + " deve estar no formato dd/MM/aaaa.");
        }
    }

    public static LocalTime lerHora(String texto, String campo) {
        try {
            return LocalTime.parse(exigirPreenchido(texto, campo), HORA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("O campo " + campo + " deve estar no formato HH:mm.");
        }
    }

    public static String exigirPreenchido(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O campo " + campo + " é obrigatório.");
        }
        return texto.trim();
    }

    public static String nomeDoDia(DayOfWeek dia) {
        return dia.getDisplayName(TextStyle.FULL, PT_BR);
    }
}
