package ua.lpnu.kzp.cli;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** Розбирає аргументи командного рядка та формує текст довідки. */
public final class ArgumentsParser {
    private static final String USAGE = """
            Використання: java -jar lab01.jar [--input <файл>] [--output <файл>]
                          java -jar lab01.jar --help | --version

            Програма читає записи про доставлення посилок (UTF-8, поля через «;»):
              id;weightKg;city;deliveryDays;express
            перевіряє їх, обчислює показники та записує звіт у консоль і файл.

            Опції:
              --input <файл>   вхідний CSV-файл (за замовчуванням data/input.csv)
              --output <файл>  файл звіту (за замовчуванням out/report.txt)
              --help           показати цю довідку
              --version        показати версію та номер збірки
            """;

    /* Забороняє створення екземплярів службового класу. */
    private ArgumentsParser() {
    }

    /**
     * Розбирає аргументи командного рядка.
     *
     * <p>{@code --help} і {@code --version} використовуються окремо від інших
     * аргументів; {@code --input} і {@code --output} можна задавати в будь-якому порядку.
     *
     * @param args аргументи командного рядка
     * @return режим роботи та шляхи до файлів
     * @throws UsageException якщо аргументи задано неправильно
     */
    public static Arguments parse(String[] args) throws UsageException {
        if (args.length == 1 && "--help".equals(args[0])) {
            return new Arguments(Arguments.Mode.HELP, Arguments.DEFAULT_INPUT, Arguments.DEFAULT_OUTPUT);
        }
        if (args.length == 1 && "--version".equals(args[0])) {
            return new Arguments(Arguments.Mode.VERSION, Arguments.DEFAULT_INPUT, Arguments.DEFAULT_OUTPUT);
        }
        Path input = Arguments.DEFAULT_INPUT;
        Path output = Arguments.DEFAULT_OUTPUT;
        // Кожна опція обробки займає два аргументи: назву та шлях.
        for (int index = 0; index < args.length; index += 2) {
            switch (args[index]) {
                case "--input" -> input = pathAfter(args, index);
                case "--output" -> output = pathAfter(args, index);
                case "--help", "--version" -> throw new UsageException(
                        "Опцію %s не можна поєднувати з іншими аргументами".formatted(args[index]));
                default -> throw new UsageException("Невідомий аргумент: %s".formatted(args[index]));
            }
        }
        return new Arguments(Arguments.Mode.PROCESS, input, output);
    }

    /**
     * Повертає текст довідки.
     *
     * @return довідка, кожен рядок якої завершено роздільником поточної ОС
     */
    public static String usage() {
        StringBuilder text = new StringBuilder();
        USAGE.lines().forEach(line -> text.append(line).append(System.lineSeparator()));
        return text.toString();
    }

    private static Path pathAfter(String[] args, int optionIndex) throws UsageException {
        String option = args[optionIndex];
        // Наступний аргумент, що починається з --, є іншою опцією, а не шляхом.
        if (optionIndex + 1 >= args.length || args[optionIndex + 1].isBlank()
                || args[optionIndex + 1].startsWith("--")) {
            throw new UsageException("Опція %s потребує шляху до файла".formatted(option));
        }
        try {
            return Path.of(args[optionIndex + 1]);
        } catch (InvalidPathException exception) {
            throw new UsageException("Неправильний шлях у %s: %s".formatted(option, args[optionIndex + 1]));
        }
    }
}
