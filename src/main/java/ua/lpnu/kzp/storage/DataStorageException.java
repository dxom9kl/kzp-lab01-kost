package ua.lpnu.kzp.storage;

/**
 * Повідомляє, що дані не вдалося записати, прочитати, розібрати або отримати рефлексією.
 *
 * <p>Це checked-виняток: компілятор змушує викликача обробити помилку або передати її далі. Повідомлення
 * пояснює операцію й дані (шлях, номер запису, поле), а конструктор із причиною зберігає початкову
 * технічну помилку – {@link java.io.IOException}, {@link NumberFormatException}, помилку доступу до поля, –
 * яка лишається доступною через {@link #getCause()}.
 */
public final class DataStorageException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Створює виняток без першопричини, коли помилку виявила сама програма.
     *
     * @param message що саме не вдалося і з якими даними
     */
    public DataStorageException(String message) {
        super(message);
    }

    /**
     * Створює виняток, що загортає початкову помилку.
     *
     * @param message що саме не вдалося і з якими даними
     * @param cause початкова помилка, яку повертає {@link #getCause()}
     */
    public DataStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
