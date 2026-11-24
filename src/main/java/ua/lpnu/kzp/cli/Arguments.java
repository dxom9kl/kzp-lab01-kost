package ua.lpnu.kzp.cli;

import java.nio.file.Path;

/**
 * Розібрані аргументи командного рядка.
 *
 * @param mode режим роботи програми
 * @param input шлях до вхідного CSV-файла
 * @param output шлях до файла звіту
 * @param csv шлях до CSV-файла для експорту посилок або {@code null}, якщо експорт не потрібен
 */
public record Arguments(Mode mode, Path input, Path output, Path csv) {
    /** Вхідний файл за замовчуванням. */
    public static final Path DEFAULT_INPUT = Path.of("data", "input.csv");
    /** Файл звіту за замовчуванням. */
    public static final Path DEFAULT_OUTPUT = Path.of("out", "report.txt");

    /**
     * Створює аргументи без CSV-експорту.
     *
     * @param mode режим роботи програми
     * @param input шлях до вхідного CSV-файла
     * @param output шлях до файла звіту
     */
    public Arguments(Mode mode, Path input, Path output) {
        this(mode, input, output, null);
    }

    /** Режим роботи, який обирають аргументи командного рядка. */
    public enum Mode {
        /** Обробити вхідний файл і сформувати звіт. */
        PROCESS,
        /** Показати довідку. */
        HELP,
        /** Показати версію та номер збірки. */
        VERSION
    }
}
