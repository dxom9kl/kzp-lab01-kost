package ua.lpnu.kzp.delivery;

/**
 * Розбирає та перевіряє один рядок вхідного файла із записом про посилку.
 *
 * <p>Формат рядка: {@code id;weightKg;city;deliveryDays;express}. Коректний
 * запис повертається масивом полів, індекси яких задають константи класу.
 */
public final class ParcelParser {
    /** Роздільник полів у рядку. */
    public static final String SEPARATOR = ";";
    /** Кількість полів у записі. */
    public static final int FIELD_COUNT = 5;
    /** Індекс ідентифікатора посилки. */
    public static final int ID = 0;
    /** Індекс ваги посилки в кілограмах. */
    public static final int WEIGHT_KG = 1;
    /** Індекс міста доставлення. */
    public static final int CITY = 2;
    /** Індекс терміну доставлення в днях. */
    public static final int DELIVERY_DAYS = 3;
    /** Індекс ознаки експрес-доставлення. */
    public static final int EXPRESS = 4;

    /* Забороняє створення екземплярів службового класу. */
    private ParcelParser() {
    }

    /**
     * Розбиває рядок на поля та перевіряє кожне з них.
     *
     * <p>Перевірки виконуються в порядку полів, тож причиною пропуску стає перша
     * знайдена проблема.
     *
     * @param line рядок вхідного файла
     * @return поля коректного запису в порядку формату
     * @throws InvalidRecordException якщо рядок не є коректним записом
     */
    public static String[] parse(String line) throws InvalidRecordException {
        if (line.isBlank()) {
            throw new InvalidRecordException("порожній рядок");
        }
        // Другий аргумент -1 зберігає порожні поля наприкінці рядка.
        String[] fields = line.split(SEPARATOR, -1);
        if (fields.length != FIELD_COUNT) {
            throw new InvalidRecordException(
                    "очікується %d полів, отримано %d".formatted(FIELD_COUNT, fields.length));
        }
        requireText(fields[ID], "порожній ідентифікатор посилки");
        requireText(fields[WEIGHT_KG], "порожня вага");
        requireText(fields[CITY], "порожнє місто");
        requireText(fields[DELIVERY_DAYS], "порожній термін доставлення");
        requireText(fields[EXPRESS], "порожня ознака експрес-доставлення");
        checkWeightKg(fields[WEIGHT_KG]);
        checkDeliveryDays(fields[DELIVERY_DAYS]);
        checkExpress(fields[EXPRESS]);
        return fields;
    }

    private static void requireText(String value, String reason) throws InvalidRecordException {
        if (value.isBlank()) {
            throw new InvalidRecordException(reason);
        }
    }

    private static void checkWeightKg(String text) throws InvalidRecordException {
        double weightKg;
        try {
            weightKg = Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            throw new InvalidRecordException("вага не є числом: \"%s\"".formatted(text));
        }
        // Посилка без ваги або з від'ємною вагою не має фізичного змісту.
        if (weightKg <= 0) {
            throw new InvalidRecordException("вага має бути додатною: %s".formatted(text));
        }
    }

    private static void checkDeliveryDays(String text) throws InvalidRecordException {
        int deliveryDays;
        try {
            deliveryDays = Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new InvalidRecordException(
                    "термін доставлення не є цілим числом: \"%s\"".formatted(text));
        }
        // Нуль днів допустимий: це доставлення в день відправлення.
        if (deliveryDays < 0) {
            throw new InvalidRecordException("від'ємний термін доставлення: %d".formatted(deliveryDays));
        }
    }

    private static void checkExpress(String text) throws InvalidRecordException {
        // Boolean.parseBoolean мовчки повертає false для будь-якого тексту, тому значення перевіряємо явно.
        if (!"true".equals(text) && !"false".equals(text)) {
            throw new InvalidRecordException(
                    "ознака експрес-доставлення має бути true або false: \"%s\"".formatted(text));
        }
    }
}
