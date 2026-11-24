package ua.lpnu.kzp.delivery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import ua.lpnu.kzp.storage.CsvColumn;
import ua.lpnu.kzp.storage.CsvParser;
import ua.lpnu.kzp.storage.DataStorageException;

/**
 * Плоский незмінний запис посилки для CSV: поля й назви стовпців варіанта 15.
 *
 * <p>Запис містить лише значення, без поведінки підтипів, тому його експортує універсальний
 * {@link ua.lpnu.kzp.storage.CsvExporter}. Фабрика {@link #of(Parcel)} створює запис із посилки будь-якого
 * підтипу, а {@link #fromFields(List)} відновлює його з полів CSV. Експортер упорядковує стовпці за
 * Java-іменами полів, тому порядок у файлі такий: {@code city}, {@code dimensions}, {@code status},
 * {@code trackingNumber}, {@code weightKg}, тобто {@code місто,габарити,статус,номер,вага}. Статус записується
 * назвою константи enum, наприклад {@code IN_TRANSIT}.
 *
 * <p>Record порівнює записи за всіма полями, а {@code weightKg} – через {@link Double#compare(double, double)},
 * тому {@code equals} і {@code hashCode} узгоджені.
 *
 * @param trackingNumber номер посилки – ключ round-trip, непорожній
 * @param weightKg вага в кілограмах, скінченне число, більше нуля
 * @param dimensions габарити {@code ДxШxВ} у сантиметрах, наприклад {@code 30x20x15}, або порожній рядок,
 *        якщо габарити не вказано
 * @param city місто доставлення, непорожнє
 * @param status стан посилки
 */
public record ParcelRecord(
        @CsvColumn("номер") String trackingNumber,
        @CsvColumn("вага") double weightKg,
        @CsvColumn("габарити") String dimensions,
        @CsvColumn("місто") String city,
        @CsvColumn("статус") DeliveryStatus status) {

    /** Значення габаритів, якщо їх не вказано: вхідний файл ЛР № 1 габаритів не містить. */
    public static final String NO_DIMENSIONS = "";

    /* Індекси полів CSV у порядку Java-імен полів, яким CsvExporter упорядковує стовпці. */
    private static final int FIELD_COUNT = 5;
    private static final int CITY = 0;
    private static final int DIMENSIONS = 1;
    private static final int STATUS = 2;
    private static final int TRACKING_NUMBER = 3;
    private static final int WEIGHT_KG = 4;

    /* Довжина, ширина й висота – додатні цілі числа без нуля на початку, розділені латинською x. */
    private static final Pattern DIMENSIONS_FORMAT = Pattern.compile("[1-9]\\d*x[1-9]\\d*x[1-9]\\d*");

    /**
     * Перевіряє інваріанти запису, тому хибний запис неможливо створити.
     *
     * @throws NullPointerException якщо номер, габарити, місто або статус дорівнює {@code null}
     * @throws IllegalArgumentException якщо значення порушує інваріант запису
     */
    public ParcelRecord {
        Objects.requireNonNull(trackingNumber, "номер посилки не може бути null");
        Objects.requireNonNull(dimensions, "габарити не можуть бути null");
        Objects.requireNonNull(city, "місто не може бути null");
        Objects.requireNonNull(status, "статус не може бути null");
        if (trackingNumber.isBlank()) {
            throw new IllegalArgumentException("порожній номер посилки");
        }
        if (!Double.isFinite(weightKg)) {
            throw new IllegalArgumentException("вага має бути скінченним числом: %s".formatted(weightKg));
        }
        if (weightKg <= 0) {
            throw new IllegalArgumentException("вага має бути додатною: %s".formatted(weightKg));
        }
        if (!dimensions.isEmpty() && !DIMENSIONS_FORMAT.matcher(dimensions).matches()) {
            throw new IllegalArgumentException(
                    "габарити мають бути у форматі ДxШxВ у сантиметрах, наприклад 30x20x15: \"%s\"".formatted(dimensions));
        }
        if (city.isBlank()) {
            throw new IllegalArgumentException("порожнє місто");
        }
    }

    /**
     * Створює запис для експорту з посилки будь-якого підтипу.
     *
     * <p>Статус визначає {@link Parcel#getStatus()} за розрахунковим терміном фактичного підтипу. Вхідний файл
     * не містить габаритів, тому вони порожні.
     *
     * @param parcel коректна посилка
     * @return запис із номером, вагою, містом і статусом посилки
     * @throws NullPointerException якщо {@code parcel} дорівнює {@code null}
     */
    public static ParcelRecord of(Parcel parcel) {
        Objects.requireNonNull(parcel, "посилка не може бути null");
        return new ParcelRecord(parcel.getId(), parcel.getWeightKg(), NO_DIMENSIONS, parcel.getCity(),
                parcel.getStatus());
    }

    /**
     * Розбирає один CSV-запис у порядку стовпців експорту.
     *
     * @param line текст одного CSV-запису без заголовка
     * @return відновлений запис
     * @throws DataStorageException якщо текст не є одним CSV-записом або поля не утворюють коректного запису
     * @throws NullPointerException якщо {@code line} дорівнює {@code null}
     */
    public static ParcelRecord fromCsv(String line) throws DataStorageException {
        return fromFields(CsvParser.parse(line));
    }

    /**
     * Створює запис із уже розібраних полів CSV.
     *
     * <p>Спершу перевіряється кількість полів, потім формат ваги та статусу, а далі інваріанти запису.
     *
     * @param fields поля в порядку {@code місто,габарити,статус,номер,вага}
     * @return відновлений запис
     * @throws DataStorageException якщо кількість полів не п'ять, вага не є числом, статус невідомий або значення
     *         порушує інваріант; для трьох останніх випадків першопричину повертає {@code getCause()}
     * @throws NullPointerException якщо {@code fields} дорівнює {@code null}
     */
    public static ParcelRecord fromFields(List<String> fields) throws DataStorageException {
        Objects.requireNonNull(fields, "поля CSV не можуть бути null");
        if (fields.size() != FIELD_COUNT) {
            throw new DataStorageException(String.format(Locale.ROOT,
                    "очікується %d полів, отримано %d", FIELD_COUNT, fields.size()));
        }
        double weightKg = parseWeightKg(fields.get(WEIGHT_KG));
        DeliveryStatus status = parseStatus(fields.get(STATUS));
        try {
            return new ParcelRecord(fields.get(TRACKING_NUMBER), weightKg, fields.get(DIMENSIONS),
                    fields.get(CITY), status);
        } catch (IllegalArgumentException exception) {
            throw new DataStorageException("некоректний запис посилки: %s".formatted(exception.getMessage()),
                    exception);
        }
    }

    private static double parseWeightKg(String text) throws DataStorageException {
        try {
            // BigDecimal приймає лише десятковий запис, зокрема 1.0E7 з Double.toString, а Double.parseDouble
            // мовчки приймає ще NaN, Infinity, пробіли навколо числа, 2.5d і 0x1p3.
            return new BigDecimal(text).doubleValue();
        } catch (NumberFormatException exception) {
            throw new DataStorageException("вага не є числом: \"%s\"".formatted(text), exception);
        }
    }

    private static DeliveryStatus parseStatus(String text) throws DataStorageException {
        try {
            return DeliveryStatus.valueOf(text);
        } catch (IllegalArgumentException exception) {
            throw new DataStorageException("невідомий статус посилки: \"%s\"".formatted(text), exception);
        }
    }
}
