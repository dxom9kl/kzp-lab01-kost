package ua.lpnu.kzp.cli;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.stream.Collectors;

/** Розбирає аргументи командного рядка та формує текст довідки. */
public final class ArgumentsParser {
    private static final String USAGE = """
            Використання: java -jar lab01.jar [--input <файл>] [--output <файл>] [--csv <файл>]
                          java -jar lab01.jar --help | --version

            Програма читає записи про доставлення посилок (UTF-8, поля через «;»):
              id;weightKg;city;deliveryDays;express
            перевіряє їх, обчислює показники та записує звіт у консоль і файл.

            Опції:
              --input <файл>   вхідний CSV-файл (за замовчуванням data/input.csv)
              --output <файл>  файл звіту (за замовчуванням out/report.txt)
              --csv <файл>     записати коректні посилки в CSV (UTF-8, кома, заголовок
                               місто,габарити,статус,номер,вага) і перевірити round-trip
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
     * аргументів; {@code --input}, {@code --output} і {@code --csv} можна задавати в будь-якому порядку.
     * Файл {@code --csv} не може збігатися з вхідним файлом або файлом звіту, щоб експорт їх не перезаписав.
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
        Path csv = null;
        // Кожна опція обробки займає два аргументи: назву та шлях.
        for (int index = 0; index < args.length; index += 2) {
            switch (args[index]) {
                case "--input" -> input = pathAfter(args, index);
                case "--output" -> output = pathAfter(args, index);
                case "--csv" -> csv = pathAfter(args, index);
                case "--help", "--version" -> throw new UsageException(
                        "Опцію %s не можна поєднувати з іншими аргументами".formatted(args[index]));
                default -> throw new UsageException("Невідомий аргумент: %s".formatted(args[index]));
            }
        }
        if (csv != null && (sameFile(csv, input) || sameFile(csv, output))) {
            throw new UsageException("Файл --csv має відрізнятися від --input і --output: %s".formatted(csv));
        }
        return new Arguments(Arguments.Mode.PROCESS, input, output, csv);
    }

    /**
     * Повертає текст довідки.
     *
     * @return довідка, кожен рядок якої завершено роздільником поточної ОС
     */
    public static String usage() {
        return USAGE.lines()
                .map(line -> line + System.lineSeparator())
                .collect(Collectors.joining());
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

    /* Порівнює абсолютні нормалізовані шляхи, тож out/a.csv і ./out/a.csv вважаються одним файлом. */
    private static boolean sameFile(Path first, Path second) {
        return first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
    }
}
