package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import ua.lpnu.kzp.storage.CsvColumn;
import ua.lpnu.kzp.storage.CsvExporter;
import ua.lpnu.kzp.storage.DataStorageException;

class ParcelRecordTest {
    private static final DeliveryStatus DELIVERED = DeliveryStatus.DELIVERED;
    private static final DeliveryStatus IN_TRANSIT = DeliveryStatus.IN_TRANSIT;
    private static final ParcelRecord LVIV = new ParcelRecord("UA-5001", 2.5, "30x20x15", "Львів", DELIVERED);

    private static String dimensionsError(String dimensions) {
        return "габарити мають бути у форматі ДxШxВ у сантиметрах, наприклад 30x20x15: \"%s\"".formatted(dimensions);
    }

    /* Поля й назви стовпців відповідають варіанту 15. */
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"trackingNumber, номер", "weightKg, вага", "dimensions, габарити", "city, місто", "status, статус"})
    void fieldsCarryVariantColumnNames(String field, String column) throws NoSuchFieldException {
        assertEquals(column, ParcelRecord.class.getDeclaredField(field).getAnnotation(CsvColumn.class).value());
    }

    /* Заголовок упорядковано за Java-іменами: city, dimensions, status, trackingNumber, weightKg. */
    @Test
    void headerIsSortedByJavaFieldNames() throws DataStorageException {
        assertEquals(List.of("місто", "габарити", "статус", "номер", "вага"), CsvExporter.header(ParcelRecord.class));
    }

    /* Запис переносить значення посилки, статус обчислює фактичний підтип, а габарити порожні. */
    @Test
    void createsRecordFromParcelOfAnySubtype() {
        assertEquals(new ParcelRecord("UA-1", 2.5, "", "Львів", DELIVERED),
                ParcelRecord.of(new ExpressParcel("UA-1", 2.5, "Львів", 1)));
        // 2 дні + 1 день резерву = 3 дні, тобто після контрольного дня посилка ще в дорозі.
        assertEquals(new ParcelRecord("UA-2", 0.75, "", "Київ", IN_TRANSIT),
                ParcelRecord.of(new RegularParcel("UA-2", 0.75, "Київ", 2, 1)));
        // 4 дні − 2 дні прискорення = 2 дні, тобто посилку доставлено.
        assertEquals(DELIVERED, ParcelRecord.of(new ExpressParcel("UA-3", 1.0, "Одеса", 4, 2)).status());
    }

    /* Габарити або порожні, або три додатні цілі через латинську x. */
    @ParameterizedTest
    @ValueSource(strings = {"", "30x20x15", "1x1x1", "120x60x50"})
    void acceptsEmptyOrWellFormedDimensions(String dimensions) {
        assertEquals(dimensions, new ParcelRecord("UA-1", 1.0, dimensions, "Львів", DELIVERED).dimensions());
    }

    /* Хибні значення відхиляє конструктор, тому хибний запис неможливо створити. */
    @ParameterizedTest(name = "[{index}] {5}")
    @MethodSource
    void rejectsInvalidValues(String trackingNumber, double weightKg, String dimensions, String city,
            DeliveryStatus status, String message) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ParcelRecord(trackingNumber, weightKg, dimensions, city, status));

        assertEquals(message, exception.getMessage());
    }

    static Stream<Arguments> rejectsInvalidValues() {
        return Stream.of(
                arguments(" ", 1.0, "", "Львів", DELIVERED, "порожній номер посилки"),
                arguments("UA-1", 0.0, "", "Львів", DELIVERED, "вага має бути додатною: 0.0"),
                arguments("UA-1", -2.5, "", "Львів", DELIVERED, "вага має бути додатною: -2.5"),
                arguments("UA-1", Double.NaN, "", "Львів", DELIVERED, "вага має бути скінченним числом: NaN"),
                arguments("UA-1", Double.POSITIVE_INFINITY, "", "Львів", DELIVERED,
                        "вага має бути скінченним числом: Infinity"),
                arguments("UA-1", 1.0, "30x20", "Львів", DELIVERED, dimensionsError("30x20")),
                arguments("UA-1", 1.0, "30х20х15", "Львів", DELIVERED, dimensionsError("30х20х15")),
                arguments("UA-1", 1.0, "0x20x15", "Львів", DELIVERED, dimensionsError("0x20x15")),
                arguments("UA-1", 1.0, "30 x 20 x 15", "Львів", DELIVERED, dimensionsError("30 x 20 x 15")),
                arguments("UA-1", 1.0, "30x20x15 см", "Львів", DELIVERED, dimensionsError("30x20x15 см")),
                arguments("UA-1", 1.0, "", " ", DELIVERED, "порожнє місто"));
    }

    /* Відсутні значення відхиляються одразу. */
    @Test
    void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> new ParcelRecord(null, 1.0, "", "Львів", DELIVERED));
        assertThrows(NullPointerException.class, () -> new ParcelRecord("UA-1", 1.0, null, "Львів", DELIVERED));
        assertThrows(NullPointerException.class, () -> new ParcelRecord("UA-1", 1.0, "", null, DELIVERED));
        assertThrows(NullPointerException.class, () -> new ParcelRecord("UA-1", 1.0, "", "Львів", null));
        assertThrows(NullPointerException.class, () -> ParcelRecord.of(null));
    }

    /* Поля CSV ідуть у порядку заголовка: місто, габарити, статус, номер, вага. */
    @Test
    void restoresRecordFromFieldsInHeaderOrder() throws DataStorageException {
        assertEquals(LVIV, ParcelRecord.fromFields(List.of("Львів", "30x20x15", "DELIVERED", "UA-5001", "2.5")));
    }

    /* fromCsv розбирає рядок CSV-парсером, тому кома в лапках лишається в назві міста. */
    @Test
    void fromCsvKeepsQuotedComma() throws DataStorageException {
        assertEquals(new ParcelRecord("UA-5003", 0.75, "25x25x10", "Біла Церква, Київська обл.", DELIVERED),
                ParcelRecord.fromCsv("\"Біла Церква, Київська обл.\",25x25x10,DELIVERED,UA-5003,0.75"));
    }

    /* Неправильна кількість полів – помилка формату без першопричини; кома без лапок дає зайве поле. */
    @ParameterizedTest
    @ValueSource(strings = {
        "Львів,30x20x15,DELIVERED,UA-5001",
        "Львів,30x20x15,DELIVERED,UA-5001,2.5,зайве",
        "Біла Церква, Київська обл.,25x25x10,DELIVERED,UA-5003,0.75"})
    void wrongFieldCountIsFormatError(String line) {
        DataStorageException exception = assertThrows(DataStorageException.class, () -> ParcelRecord.fromCsv(line));

        assertTrue(exception.getMessage().startsWith("очікується 5 полів, отримано "), exception.getMessage());
        assertNull(exception.getCause());
    }

    /* Хибне число зберігає NumberFormatException як першопричину. */
    @ParameterizedTest(name = "[{index}] «{0}»")
    @ValueSource(strings = {"важка", "2,5", " 2.5", "NaN", "Infinity", "0x1p3", "2.5d", ""})
    void invalidWeightKeepsNumberFormatException(String weight) {
        List<String> fields = List.of("Львів", "", "DELIVERED", "UA-1", weight);

        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> ParcelRecord.fromFields(fields));

        assertEquals("вага не є числом: \"%s\"".formatted(weight), exception.getMessage());
        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }

    /* BigDecimal розпізнає й цифри інших писемностей, але вага в CSV записується лише цифрами 0–9. */
    @ParameterizedTest(name = "[{index}] «{0}»")
    @ValueSource(strings = {"２.５", "٢.٥"})
    void rejectsNonAsciiDigits(String weight) {
        List<String> fields = List.of("Львів", "", "DELIVERED", "UA-1", weight);

        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> ParcelRecord.fromFields(fields));

        assertEquals("вага має бути записана цифрами 0–9: \"%s\"".formatted(weight), exception.getMessage());
    }

    /* Невідомий статус зберігає IllegalArgumentException від valueOf як першопричину. */
    @ParameterizedTest(name = "[{index}] «{0}»")
    @ValueSource(strings = {"LOST", "in_transit", "у дорозі", ""})
    void unknownStatusKeepsCause(String status) {
        List<String> fields = List.of("Львів", "", status, "UA-1", "2.5");

        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> ParcelRecord.fromFields(fields));

        assertEquals("невідомий статус посилки: \"%s\"".formatted(status), exception.getMessage());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    /* Число записано правильно, але порушує інваріант: першопричиною є виняток конструктора. */
    @Test
    void invalidValueKeepsConstructorException() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> ParcelRecord.fromFields(List.of("Львів", "", "DELIVERED", "UA-1", "0")));

        assertEquals("некоректний запис посилки: вага має бути додатною: 0.0", exception.getMessage());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    /* Вагу з Double.toString, зокрема в експоненційному записі, відновлено точно. */
    @ParameterizedTest
    @ValueSource(doubles = {2.5, 0.75, 0.1, 12.4, 25.0, 1.0E7, 5.0E-4, 123456.789, Double.MIN_VALUE, Double.MAX_VALUE})
    void weightRoundTripsExactly(double weight) throws DataStorageException {
        ParcelRecord record = new ParcelRecord("UA-1", weight, "", "Львів", DELIVERED);

        assertEquals(record, ParcelRecord.fromFields(List.of("Львів", "", "DELIVERED", "UA-1", String.valueOf(weight))));
    }

    /* Рівні записи мають однаковий хеш-код, а відмінність будь-якого поля робить записи різними. */
    @Test
    void equalityUsesAllFields() {
        ParcelRecord same = new ParcelRecord("UA-5001", 2.5, "30x20x15", "Львів", DELIVERED);

        assertEquals(LVIV, same);
        assertEquals(LVIV.hashCode(), same.hashCode());
        assertNotEquals(LVIV, new ParcelRecord("UA-5002", 2.5, "30x20x15", "Львів", DELIVERED));
        assertNotEquals(LVIV, new ParcelRecord("UA-5001", 2.6, "30x20x15", "Львів", DELIVERED));
        assertNotEquals(LVIV, new ParcelRecord("UA-5001", 2.5, "30x20x16", "Львів", DELIVERED));
        assertNotEquals(LVIV, new ParcelRecord("UA-5001", 2.5, "30x20x15", "Київ", DELIVERED));
        assertNotEquals(LVIV, new ParcelRecord("UA-5001", 2.5, "30x20x15", "Львів", IN_TRANSIT));
    }
}
