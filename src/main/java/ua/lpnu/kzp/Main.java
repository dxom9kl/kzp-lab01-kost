package ua.lpnu.kzp;

/** Точка входу консольної програми «Доставлення посилок». */
public final class Main {
    /* Забороняє створення екземплярів класу точки входу. */
    private Main() {
    }

    /**
     * Запускає програму.
     *
     * @param args аргументи командного рядка
     */
    public static void main(String[] args) {
        if (args.length == 1 && "--version".equals(args[0])) {
            System.out.printf("%s%n", BuildInfo.version());
            return;
        }
        System.out.printf("Обробку записів буде додано в наступних змінах.%n");
    }
}
