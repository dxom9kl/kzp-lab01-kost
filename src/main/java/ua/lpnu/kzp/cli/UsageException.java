package ua.lpnu.kzp.cli;

/** Аргументи командного рядка задано неправильно. */
public final class UsageException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Створює виняток із поясненням для користувача.
     *
     * @param message що саме не так з аргументами
     */
    public UsageException(String message) {
        super(message);
    }
}
