package ua.lpnu.kzp.delivery;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Посилка – незмінний запис про одне доставлення.
 *
 * <p>Конструктор перевіряє всі інваріанти, тому коректність об'єкта не залежить від способу
 * його створення: ідентифікатор і місто не порожні, вага – додатне скінченне число, термін
 * доставлення не від'ємний. Фабрика {@link #fromCsv(String)} створює посилку з рядка вхідного
 * файла.
 */
public final class Parcel {
    private static final String SEPARATOR = ";";
    private static final int FIELD_COUNT = 5;
    private static final int ID = 0;
    private static final int WEIGHT_KG = 1;
    private static final int CITY = 2;
    private static final int DELIVERY_DAYS = 3;
    private static final int EXPRESS = 4;

    private static final String EMPTY_ID = "порожній ідентифікатор посилки";
    private static final String EMPTY_CITY = "порожнє місто";
    private static final String WEIGHT_NOT_NUMBER = "вага не є числом: \"%s\"";

    /* Десяткове число з крапкою: без знака «+», експоненти, суфіксів Java і пробілів. */
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
    /* Ціле число без знака «+» і пробілів. */
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    private final String id;
    private final double weightKg;
    private final String city;
    private final int deliveryDays;
    private final boolean express;

    /**
     * Створює коректну посилку.
     *
     * @param id ідентифікатор посилки, непорожній
     * @param weightKg вага в кілограмах, додатне скінченне число
     * @param city місто доставлення, непорожнє
     * @param deliveryDays термін доставлення в днях; нуль означає доставлення в день відправлення
     * @param express {@code true}, якщо це експрес-доставлення
     * @throws NullPointerException якщо {@code id} або {@code city} дорівнює {@code null}
     * @throws IllegalArgumentException якщо значення порушує інваріант посилки
     */
    public Parcel(String id, double weightKg, String city, int deliveryDays, boolean express) {
        // Перевірки виконуються до присвоєння, тому некоректний об'єкт не створюється.
        Objects.requireNonNull(id, "ідентифікатор посилки не може бути null");
        Objects.requireNonNull(city, "місто не може бути null");
        requireText(id, EMPTY_ID);
        checkWeightKg(weightKg, Double.toString(weightKg));
        requireText(city, EMPTY_CITY);
        checkDeliveryDays(deliveryDays);
        this.id = id;
        this.weightKg = weightKg;
        this.city = city;
        this.deliveryDays = deliveryDays;
        this.express = express;
    }

    /**
     * Створює посилку з рядка вхідного файла.
     *
     * <p>Формат рядка: {@code id;weightKg;city;deliveryDays;express}. Спершу перевіряється
     * наявність усіх полів, потім їхні значення в порядку формату, тож повідомлення винятку
     * називає першу знайдену проблему.
     *
     * @param line рядок вхідного файла
     * @return посилка з полями рядка
     * @throws NullPointerException якщо {@code line} дорівнює {@code null}
     * @throws IllegalArgumentException якщо рядок не є коректним записом
     */
    public static Parcel fromCsv(String line) {
        Objects.requireNonNull(line, "рядок не може бути null");
        if (line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }
        // Другий аргумент -1 зберігає порожні поля наприкінці рядка.
        String[] fields = line.split(SEPARATOR, -1);
        if (fields.length != FIELD_COUNT) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "очікується %d полів, отримано %d", FIELD_COUNT, fields.length));
        }
        // Порядок перевірок збережено з ЛР № 1, щоб причина пропуску рядка не змінилася.
        requireText(fields[ID], EMPTY_ID);
        requireText(fields[WEIGHT_KG], "порожня вага");
        requireText(fields[CITY], EMPTY_CITY);
        requireText(fields[DELIVERY_DAYS], "порожній термін доставлення");
        requireText(fields[EXPRESS], "порожня ознака експрес-доставлення");
        double weightKg = parseWeightKg(fields[WEIGHT_KG]);
        int deliveryDays = parseDeliveryDays(fields[DELIVERY_DAYS]);
        boolean express = parseExpress(fields[EXPRESS]);
        return new Parcel(fields[ID], weightKg, fields[CITY], deliveryDays, express);
    }

    /**
     * Повертає ідентифікатор посилки.
     *
     * @return непорожній ідентифікатор
     */
    public String getId() {
        return id;
    }

    /**
     * Повертає вагу посилки.
     *
     * @return вага в кілограмах
     */
    public double getWeightKg() {
        return weightKg;
    }

    /**
     * Повертає місто доставлення.
     *
     * @return непорожня назва міста
     */
    public String getCity() {
        return city;
    }

    /**
     * Повертає термін доставлення.
     *
     * @return термін у днях
     */
    public int getDeliveryDays() {
        return deliveryDays;
    }

    /**
     * Повідомляє, чи є доставлення експресним.
     *
     * @return {@code true} для експрес-доставлення
     */
    public boolean isExpress() {
        return express;
    }

    /**
     * Повертає читабельне подання посилки; це не формат вхідного файла.
     *
     * @return рядок вигляду {@code UA-1001: 2.50 кг, Львів, 1 дн., експрес}
     */
    @Override
    public String toString() {
        // Locale.ROOT робить крапку десятковим роздільником на будь-якій ОС.
        return String.format(Locale.ROOT, "%s: %.2f кг, %s, %d дн., %s",
                id, weightKg, city, deliveryDays, express ? "експрес" : "звичайне");
    }

    private static void requireText(String value, String reason) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(reason);
        }
    }

    /*
     * Правило ваги записане лише тут. Фабрика викликає його раніше за конструктор, щоб причиною
     * пропуску стала перша проблема рядка, а число в повідомленні лишилося таким, як у файлі.
     */
    private static void checkWeightKg(double weightKg, String shownAs) {
        if (Double.isNaN(weightKg)) {
            throw new IllegalArgumentException(WEIGHT_NOT_NUMBER.formatted(shownAs));
        }
        // Число із сотень цифр перетворюється на Infinity.
        if (Double.isInfinite(weightKg)) {
            throw new IllegalArgumentException("вага завелика");
        }
        // Посилка без ваги або з від'ємною вагою не має фізичного змісту.
        if (weightKg <= 0) {
            throw new IllegalArgumentException("вага має бути додатною: %s".formatted(shownAs));
        }
    }

    private static void checkDeliveryDays(int deliveryDays) {
        // Нуль днів допустимий: це доставлення в день відправлення.
        if (deliveryDays < 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "від'ємний термін доставлення: %d", deliveryDays));
        }
    }

    private static double parseWeightKg(String text) {
        // Double.parseDouble приймає NaN, Infinity, 1e3, 10d і пробіли, тому формат перевіряємо до перетворення.
        if (!DECIMAL.matcher(text).matches()) {
            throw new IllegalArgumentException(WEIGHT_NOT_NUMBER.formatted(text));
        }
        double weightKg = Double.parseDouble(text);
        checkWeightKg(weightKg, text);
        return weightKg;
    }

    private static int parseDeliveryDays(String text) {
        if (!INTEGER.matcher(text).matches()) {
            throw new IllegalArgumentException(
                    "термін доставлення не є цілим числом: \"%s\"".formatted(text));
        }
        int deliveryDays;
        try {
            deliveryDays = Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            // Формат уже перевірено, тож виняток означає вихід за межі int.
            throw new IllegalArgumentException(
                    "термін доставлення завеликий: \"%s\"".formatted(text), exception);
        }
        checkDeliveryDays(deliveryDays);
        return deliveryDays;
    }

    private static boolean parseExpress(String text) {
        // Boolean.parseBoolean мовчки повертає false для будь-якого тексту, тому значення перевіряємо явно.
        if (!"true".equals(text) && !"false".equals(text)) {
            throw new IllegalArgumentException(
                    "ознака експрес-доставлення має бути true або false: \"%s\"".formatted(text));
        }
        return "true".equals(text);
    }
}
