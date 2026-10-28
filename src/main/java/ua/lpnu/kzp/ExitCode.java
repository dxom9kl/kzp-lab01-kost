package ua.lpnu.kzp;

/** Коди завершення програми. */
public final class ExitCode {
    /** Обробку завершено успішно. */
    public static final int OK = 0;
    /** Файл неможливо прочитати чи записати або в ньому немає коректних записів. */
    public static final int DATA_ERROR = 1;
    /** Аргументи командного рядка задано неправильно. */
    public static final int USAGE_ERROR = 2;

    /* Забороняє створення екземплярів класу констант. */
    private ExitCode() {
    }
}
