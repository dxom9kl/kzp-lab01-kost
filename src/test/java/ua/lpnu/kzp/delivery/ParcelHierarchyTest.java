package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ParcelHierarchyTest {
    private static final double DELTA = 0.0001;

    /* Створює посилку одного з підтипів зі спільних полів. */
    @FunctionalInterface
    interface ParcelFactory {
        Parcel create(String id, double weightKg, String city, int deliveryDays);
    }

    static Stream<Arguments> subtypes() {
        ParcelFactory regular = RegularParcel::new;
        ParcelFactory express = ExpressParcel::new;
        return Stream.of(arguments("RegularParcel", regular), arguments("ExpressParcel", express));
    }

    /* Поле express вхідного рядка визначає підтип посилки. */
    @Test
    void fromCsvCreatesSubtypeByExpressFlag() {
        assertInstanceOf(RegularParcel.class, Parcel.fromCsv("UA-1;1.00;Київ;2;false"));
        assertInstanceOf(ExpressParcel.class, Parcel.fromCsv("UA-2;1.00;Львів;2;true"));
    }

    /* Вхідний файл не задає резерву чи прискорення, тому термін посилки дорівнює плановому. */
    @Test
    void parcelsFromFileKeepPlannedDays() {
        assertEquals(2, Parcel.fromCsv("UA-1;1.00;Київ;2;false").calculatedDays());
        assertEquals(6, Parcel.fromCsv("UA-2;1.00;Львів;6;true").calculatedDays());
    }

    /* Спільні поля читаються через базовий тип однаково для кожного підтипу. */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("subtypes")
    void baseTypeKeepsSharedFields(String name, ParcelFactory factory) {
        Parcel parcel = factory.create("UA-1", 2.5, "Львів", 3);

        assertEquals("UA-1", parcel.getId());
        assertEquals(2.5, parcel.getWeightKg(), DELTA);
        assertEquals("Львів", parcel.getCity());
        assertEquals(3, parcel.getDeliveryDays());
    }

    /* Спільні інваріанти записано в базовому типі, і вони діють для кожного підтипу. */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("subtypes")
    void baseTypeRejectsInvalidSharedState(String name, ParcelFactory factory) {
        assertThrows(NullPointerException.class, () -> factory.create(null, 1.0, "Київ", 1));
        assertThrows(IllegalArgumentException.class, () -> factory.create(" ", 1.0, "Київ", 1));
        assertThrows(IllegalArgumentException.class, () -> factory.create("UA-1", 0.0, "Київ", 1));
        assertThrows(IllegalArgumentException.class, () -> factory.create("UA-1", Double.NaN, "Київ", 1));
        assertThrows(NullPointerException.class, () -> factory.create("UA-1", 1.0, null, 1));
        assertThrows(IllegalArgumentException.class, () -> factory.create("UA-1", 1.0, "", 1));
        assertThrows(IllegalArgumentException.class, () -> factory.create("UA-1", 1.0, "Київ", -1));
    }

    /* Відсутнє значення повідомляється раніше за хибні значення інших полів. */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("subtypes")
    void nullIsReportedBeforeInvalidValues(String name, ParcelFactory factory) {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> factory.create(" ", -1.0, null, -1));

        assertEquals("місто не може бути null", exception.getMessage());
    }

    /* Однаковий виклик через спільний тип дає різну вартість для різних підтипів. */
    @Test
    void subtypesCalculateCostDifferently() {
        Parcel regular = new RegularParcel("UA-1", 2.0, "Львів", 3);
        Parcel express = new ExpressParcel("UA-2", 2.0, "Львів", 3);

        assertEquals(60.0, regular.calculatedCost(), DELTA);
        assertEquals(90.0, express.calculatedCost(), DELTA);
    }

    /* Власні поля підтипів змінюють розрахунковий термін у різні боки. */
    @Test
    void subtypesCalculateDaysDifferently() {
        Parcel regular = new RegularParcel("UA-1", 2.0, "Львів", 3, 2);
        Parcel express = new ExpressParcel("UA-2", 2.0, "Львів", 3, 2);

        assertEquals(5, regular.calculatedDays());
        assertEquals(1, express.calculatedDays());
    }

    /* Колекція спільного типу викликає реалізацію фактичного підтипу кожного елемента. */
    @Test
    void commonTypeCallsImplementationOfActualSubtype() {
        // Arrange
        List<Parcel> parcels = List.of(
                new RegularParcel("UA-1", 2.0, "Львів", 3),
                new ExpressParcel("UA-2", 2.0, "Київ", 3),
                new RegularParcel("UA-3", 1.0, "Одеса", 1));

        // Act
        double totalCost = 0.0;
        for (Parcel parcel : parcels) {
            totalCost += parcel.calculatedCost();
        }

        // Assert: 60 грн + 90 грн + 50 грн.
        assertEquals(200.0, totalCost, DELTA);
    }

    /* Кожен підтип повертає свій вид доставлення, а toString бере підпис із enum. */
    @Test
    void subtypesProvideOwnKind() {
        Parcel regular = new RegularParcel("UA-1", 1.0, "Київ", 1);
        Parcel express = new ExpressParcel("UA-2", 1.0, "Київ", 1);

        assertEquals(DeliveryKind.REGULAR, regular.getKind());
        assertEquals(DeliveryKind.EXPRESS, express.getKind());
        assertEquals("UA-1: 1.00 кг, Київ, 1 дн., звичайне", regular.toString());
        assertEquals("UA-2: 1.00 кг, Київ, 1 дн., експрес", express.toString());
    }
}
