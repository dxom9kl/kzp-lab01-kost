package ua.lpnu.kzp.delivery;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Посилка – спільний незмінний тип звичайних та експрес-доставлень.
 *
 * <p>Базовий клас зберігає спільні поля й перевіряє їхні інваріанти: ідентифікатор і місто не
 * порожні, вага – додатне скінченне число, термін доставлення не від'ємний. Підтипи визначають вид
 * доставлення, розрахунковий термін і вартість. Фабрика {@link #fromCsv(String)} створює потрібний
 * підтип із рядка вхідного файла.
 */
public abstract class Parcel {
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

    /* Демонстраційний тариф, а не ціни реального перевізника. */
    private static final double BASE_COST = 40.0;
    private static final double COST_PER_KG = 10.0;

    private final String id;
    private final double weightKg;
    private final String city;
    private final int deliveryDays;

    /**
     * Створює спільну частину коректної посилки.
     *
     * @param id ідентифікатор посилки, непорожній
     * @param weightKg вага в кілограмах, додатне скінченне число
     * @param city місто доставлення, непорожнє
     * @param deliveryDays плановий термін у днях; нуль означає доставлення в день відправлення
     * @throws NullPointerException якщо {@code id} або {@code city} дорівнює {@code null}
     * @throws IllegalArgumentException якщо значення порушує інваріант посилки
     */
    protected Parcel(String id, double weightKg, String city, int deliveryDays) {
        // Значення перевіряються до створення об'єкта, тому некоректна посилка не з'являється навіть частково.
        this(validId(id), validWeightKg(weightKg), validCity(city), validDeliveryDays(deliveryDays), true);
    }

    /* Присвоює вже перевірені значення; параметр validated лише відрізняє цей конструктор. */
    private Parcel(String id, double weightKg, String city, int deliveryDays, boolean validated) {
        this.id = id;
        this.weightKg = weightKg;
        this.city = city;
        this.deliveryDays = deliveryDays;
    }

    /**
     * Створює посилку з рядка вхідного файла.
     *
     * <p>Формат рядка: {@code id;weightKg;city;deliveryDays;express}. Спершу перевіряється
     * наявність усіх полів, потім їхні значення в порядку формату, тож повідомлення винятку
     * називає першу знайдену проблему.
     *
     * @param line рядок вхідного файла
     * @return посилка підтипу, який задає поле {@code express}
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
        // Єдине місце вибору підтипу: далі програма працює лише зі спільним типом Parcel.
        return express
                ? new ExpressParcel(fields[ID], weightKg, fields[CITY], deliveryDays)
                : new RegularParcel(fields[ID], weightKg, fields[CITY], deliveryDays);
    }

    /**
     * Повертає ідентифікатор посилки.
     *
     * @return непорожній ідентифікатор
     */
    public final String getId() {
        return id;
    }

    /**
     * Повертає вагу посилки.
     *
     * @return вага в кілограмах
     */
    public final double getWeightKg() {
        return weightKg;
    }

    /**
     * Повертає місто доставлення.
     *
     * @return непорожня назва міста
     */
    public final String getCity() {
        return city;
    }

    /**
     * Повертає плановий термін доставлення з вхідних даних.
     *
     * @return термін у днях
     */
    public final int getDeliveryDays() {
        return deliveryDays;
    }

    /**
     * Повідомляє, чи є доставлення експресним.
     *
     * @return {@code true} для експрес-доставлення
     */
    public final boolean isExpress() {
        // Кожна константа enum існує в одному екземплярі, тому порівняння через == коректне.
        return getKind() == DeliveryKind.EXPRESS;
    }

    /**
     * Повертає вид доставлення, який відповідає підтипу посилки.
     *
     * @return вид доставлення
     */
    public abstract DeliveryKind getKind();

    /**
     * Обчислює розрахунковий термін доставлення.
     *
     * @return термін у днях
     */
    public abstract int calculatedDays();

    /**
     * Обчислює вартість доставлення за демонстраційним тарифом.
     *
     * @return вартість у гривнях
     */
    public abstract double calculatedCost();

    /**
     * Обчислює базову вартість доставлення за вагою посилки.
     *
     * @return базова вартість у гривнях
     */
    protected final double baseCost() {
        return BASE_COST + weightKg * COST_PER_KG;
    }

    /**
     * Повертає читабельне подання посилки; це не формат вхідного файла.
     *
     * @return рядок вигляду {@code UA-1001: 2.50 кг, Львів, 1 дн., експрес}
     */
    @Override
    public final String toString() {
        // Locale.ROOT робить крапку десятковим роздільником на будь-якій ОС.
        return String.format(Locale.ROOT, "%s: %.2f кг, %s, %d дн., %s",
                id, weightKg, city, deliveryDays, getKind().label());
    }

    private static String validId(String id) {
        Objects.requireNonNull(id, "ідентифікатор посилки не може бути null");
        requireText(id, EMPTY_ID);
        return id;
    }

    private static double validWeightKg(double weightKg) {
        checkWeightKg(weightKg, Double.toString(weightKg));
        return weightKg;
    }

    private static String validCity(String city) {
        Objects.requireNonNull(city, "місто не може бути null");
        requireText(city, EMPTY_CITY);
        return city;
    }

    private static int validDeliveryDays(int deliveryDays) {
        checkDeliveryDays(deliveryDays);
        return deliveryDays;
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
