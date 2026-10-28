package ua.lpnu.kzp;

import java.io.PrintStream;
import ua.lpnu.kzp.cli.Arguments;
import ua.lpnu.kzp.cli.ArgumentsParser;
import ua.lpnu.kzp.cli.UsageException;

/**
 * Точка входу консольної програми «Доставлення посилок».
 *
 * <p>Main лише розбирає аргументи за допомогою {@link ArgumentsParser}, запускає
 * {@link DeliveryController} і повертає код завершення; сама обробка відбувається
 * в інших класах.
 */
public final class Main {
    /* Забороняє створення екземплярів класу точки входу. */
    private Main() {
    }

    /**
     * Запускає програму та завершує JVM із кодом з {@link ExitCode}.
     *
     * @param args аргументи командного рядка
     */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Виконує програму без завершення JVM, тому її можна перевіряти тестами.
     *
     * @param args аргументи командного рядка
     * @param out потік для звіту та довідки
     * @param err потік для повідомлень про помилки
     * @return код завершення з {@link ExitCode}
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        Arguments arguments;
        try {
            arguments = ArgumentsParser.parse(args);
        } catch (UsageException exception) {
            err.printf("%s%nЗапустіть програму з --help, щоб побачити список аргументів%n",
                    exception.getMessage());
            return ExitCode.USAGE_ERROR;
        }
        return switch (arguments.mode()) {
            case HELP -> {
                out.print(ArgumentsParser.usage());
                yield ExitCode.OK;
            }
            case VERSION -> {
                out.printf("%s%n", BuildInfo.version());
                yield ExitCode.OK;
            }
            case PROCESS -> new DeliveryController(arguments.input(), arguments.output()).run(out, err);
        };
    }
}
