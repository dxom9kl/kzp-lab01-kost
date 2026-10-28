package ua.lpnu.kzp.delivery;

/** Рядок вхідного файла не є коректним записом про посилку. */
public final class InvalidRecordException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Створює виняток із причиною, яку буде показано у звіті.
     *
     * @param reason причина пропуску рядка
     */
    public InvalidRecordException(String reason) {
        super(reason);
    }
}
