package ua.lpnu.kzp.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Ізолює кросплатформне читання та запис текстових файлів у UTF-8. */
public final class FileReport {
    /* Позначка порядку байтів, яку додають, зокрема, Excel і Блокнот у режимі «UTF-8 з BOM». */
    private static final char BYTE_ORDER_MARK = '﻿';

    /* Забороняє створення екземплярів службового класу. */
    private FileReport() {
    }

    /**
     * Читає рядки файла у фіксованому кодуванні UTF-8.
     *
     * <p>Позначка порядку байтів (BOM) на початку файла не потрапляє в перший рядок.
     *
     * @param input шлях до вхідного файла
     * @return рядки файла без символів завершення рядка
     * @throws IOException якщо файл неможливо прочитати або він не в UTF-8
     */
    public static List<String> readLines(Path input) throws IOException {
        // Явне кодування не залежить від налаштувань операційної системи.
        List<String> lines = new ArrayList<>(Files.readAllLines(input, StandardCharsets.UTF_8));
        // Java не прибирає BOM під час читання, тож він опинився б в ідентифікаторі першої посилки.
        if (!lines.isEmpty() && !lines.get(0).isEmpty() && lines.get(0).charAt(0) == BYTE_ORDER_MARK) {
            lines.set(0, lines.get(0).substring(1));
        }
        return lines;
    }

    /**
     * Створює каталог призначення та записує звіт у UTF-8.
     *
     * @param output шлях до файла звіту
     * @param report готовий текст звіту
     * @throws IOException якщо каталог або файл неможливо створити
     */
    public static void writeReport(Path output, String report) throws IOException {
        // Після клонування репозиторію каталогу out ще немає.
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }
}
