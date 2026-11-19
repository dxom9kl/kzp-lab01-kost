package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ParcelTest {
    private static final double DELTA = 0.0001;

    /* Parcel став абстрактним, тому об'єкт створює підтип, який відповідає ознаці express. */
    private static Parcel parcel(String id, double weightKg, String city, int deliveryDays, boolean express) {
        return express
                ? new ExpressParcel(id, weightKg, city, deliveryDays)
                : new RegularParcel(id, weightKg, city, deliveryDays);
    }

    /* Коректний конструктор зберігає всі поля. */
    @Test
    void constructorKeepsAllFields() {
        // Arrange і Act
        Parcel parcel = parcel("UA-1001", 2.5, "Львів", 1, true);

        // Assert
        assertEquals("UA-1001", parcel.getId());
        assertEquals(2.5, parcel.getWeightKg(), DELTA);
        assertEquals("Львів", parcel.getCity());
        assertEquals(1, parcel.getDeliveryDays());
        assertTrue(parcel.isExpress());
    }

    /* Найменші й найбільші допустимі значення не відхиляються; нуль днів – доставлення в той самий день. */
    @Test
    void constructorAcceptsBoundaryValues() {
        Parcel lightest = parcel("1", Double.MIN_VALUE, "Л", 0, false);
        Parcel heaviest = parcel("UA-2", Double.MAX_VALUE, "Київ", Integer.MAX_VALUE, false);

        assertEquals(Double.MIN_VALUE, lightest.getWeightKg());
        assertEquals(0, lightest.getDeliveryDays());
        assertFalse(lightest.isExpress());
        assertEquals(Double.MAX_VALUE, heaviest.getWeightKg());
        assertEquals(Integer.MAX_VALUE, heaviest.getDeliveryDays());
    }

    /* Відсутній ідентифікатор відрізняється від порожнього власною причиною. */
    @Test
    void constructorRejectsNullId() {
        // Act
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> parcel(null, 1.0, "Київ", 1, false));

        // Assert
        assertEquals("ідентифікатор посилки не може бути null", exception.getMessage());
    }

    /* Відсутнє місто відхиляється так само, як і відсутній ідентифікатор. */
    @Test
    void constructorRejectsNullCity() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> parcel("UA-1", 1.0, null, 1, false));

        assertEquals("місто не може бути null", exception.getMessage());
    }

    /* Ідентифікатор із самих пробільних символів не називає посилку. */
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t"})
    void constructorRejectsBlankId(String id) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parcel(id, 1.0, "Київ", 1, false));

        assertEquals("порожній ідентифікатор посилки", exception.getMessage());
    }

    /* Місто з самих пробільних символів не задає адреси доставлення. */
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t"})
    void constructorRejectsBlankCity(String city) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parcel("UA-1", 1.0, city, 1, false));

        assertEquals("порожнє місто", exception.getMessage());
    }

    /* Нульова, від'ємна й нескінченна вага та NaN зіпсували б середнє, тому об'єкт не створюється. */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidWeights")
    void constructorRejectsInvalidWeight(double weightKg, String reason) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parcel("UA-1", weightKg, "Київ", 1, false));

        assertEquals(reason, exception.getMessage());
    }

    static Stream<Arguments> invalidWeights() {
        return Stream.of(
                arguments(0.0, "вага має бути додатною: 0.0"),
                arguments(-0.0, "вага має бути додатною: -0.0"),
                arguments(-0.01, "вага має бути додатною: -0.01"),
                arguments(Double.NaN, "вага не є числом: \"NaN\""),
                arguments(Double.POSITIVE_INFINITY, "вага завелика"),
                arguments(Double.NEGATIVE_INFINITY, "вага завелика"));
    }

    /* Від'ємний термін відхиляється, найменше значення int теж. */
    @ParameterizedTest
    @ValueSource(ints = {-1, -30, Integer.MIN_VALUE})
    void constructorRejectsNegativeDeliveryDays(int deliveryDays) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parcel("UA-1", 1.0, "Київ", deliveryDays, false));

        assertEquals("від'ємний термін доставлення: %d".formatted(deliveryDays), exception.getMessage());
    }

    /* toString показує всі поля, а вагу – з двома знаками після крапки. */
    @Test
    void toStringDescribesParcel() {
        assertEquals("UA-1001: 2.50 кг, Львів, 1 дн., експрес",
                parcel("UA-1001", 2.5, "Львів", 1, true).toString());
        assertEquals("UA-1002: 0.75 кг, Кам'янець-Подільський, 0 дн., звичайне",
                parcel("UA-1002", 0.75, "Кам'янець-Подільський", 0, false).toString());
    }

    /* Десятковий роздільник у toString не залежить від локалі за замовчуванням. */
    @Test
    void toStringUsesDotRegardlessOfDefaultLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("uk-UA"));

            String text = parcel("UA-1", 2.5, "Львів", 1, true).toString();

            assertTrue(text.contains("2.50 кг"), text);
        } finally {
            Locale.setDefault(saved);
        }
    }

    /* Числа в причинах відхилення записуються цифрами 0–9 за будь-якої локалі за замовчуванням. */
    @Test
    void messagesUseAsciiDigitsRegardlessOfDefaultLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-SA-u-nu-arab"));

            IllegalArgumentException fields =
                    assertThrows(IllegalArgumentException.class, () -> Parcel.fromCsv("UA-1;1.00;Київ;2"));
            IllegalArgumentException days = assertThrows(IllegalArgumentException.class,
                    () -> parcel("UA-1", 1.0, "Київ", -1, false));

            assertEquals("очікується 5 полів, отримано 4", fields.getMessage());
            assertEquals("від'ємний термін доставлення: -1", days.getMessage());
        } finally {
            Locale.setDefault(saved);
        }
    }

    /* Відсутній рядок – помилка програміста, а не хибний запис файла. */
    @Test
    void fromCsvRejectsNullLine() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> Parcel.fromCsv(null));

        assertEquals("рядок не може бути null", exception.getMessage());
    }

    /* Значення читаються з файла без змін: фабрика не обрізає пробіли в текстових полях. */
    @Test
    void fromCsvKeepsTextFieldsAsWritten() {
        Parcel parcel = Parcel.fromCsv(" UA-1 ;1.00; Київ ;2;false");

        assertEquals(" UA-1 ", parcel.getId());
        assertEquals(" Київ ", parcel.getCity());
    }

    /* Порушення інваріанта показує число так, як воно записане у файлі. */
    @ParameterizedTest
    @ValueSource(strings = {"0", "0.0", "0.00", "-0", "-3", "-3.00"})
    void fromCsvShowsWeightAsWritten(String weightKg) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Parcel.fromCsv("UA-1;" + weightKg + ";Київ;2;false"));

        assertEquals("вага має бути додатною: " + weightKg, exception.getMessage());
    }

    /* Якщо в рядку кілька помилок, причиною лишається перша за порядком перевірок ЛР № 1. */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("linesWithSeveralProblems")
    void fromCsvReportsFirstProblem(String line, String reason) {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> Parcel.fromCsv(line));

        assertEquals(reason, exception.getMessage());
    }

    static Stream<Arguments> linesWithSeveralProblems() {
        return Stream.of(
                arguments(";;Київ;2;false", "порожній ідентифікатор посилки"),
                arguments("UA-1;;;2;false", "порожня вага"),
                arguments("UA-1;1.00;;;false", "порожнє місто"),
                arguments("UA-1;1.00;Київ;;", "порожній термін доставлення"),
                arguments(";важка;Київ;два;так", "порожній ідентифікатор посилки"),
                arguments("UA-1;важка;;два;так", "порожнє місто"),
                arguments("UA-1;важка;Київ;;так", "порожній термін доставлення"),
                arguments("UA-1;важка;Київ;два;", "порожня ознака експрес-доставлення"),
                arguments("UA-1;важка;Київ;два;так", "вага не є числом: \"важка\""),
                arguments("UA-1;0;Київ;два;так", "вага має бути додатною: 0"),
                arguments("UA-1;1.00;Київ;два;так", "термін доставлення не є цілим числом: \"два\""),
                arguments("UA-1;1.00;Київ;-1;так", "від'ємний термін доставлення: -1"));
    }

    /* Число має цифри по обидва боки крапки, а цифрами вважаються лише 0–9. */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("unsupportedNumberFormats")
    void fromCsvRejectsUnsupportedNumberFormat(String line, String reason) {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> Parcel.fromCsv(line));

        assertEquals(reason, exception.getMessage());
    }

    static Stream<Arguments> unsupportedNumberFormats() {
        return Stream.of(
                arguments("UA-1;5.;Київ;2;false", "вага не є числом: \"5.\""),
                arguments("UA-1;.5;Київ;2;false", "вага не є числом: \".5\""),
                arguments("UA-1;٣;Київ;2;false", "вага не є числом: \"٣\""),
                arguments("UA-1;1.00;Київ;٣;false", "термін доставлення не є цілим числом: \"٣\""));
    }

    /* Переповнення int зберігає первинний виняток як причину. */
    @Test
    void fromCsvKeepsCauseOfDaysOverflow() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Parcel.fromCsv("UA-1;1.00;Київ;99999999999;false"));

        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }
}
