package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class DeliverySummaryTest {
    private static final double DELTA = 0.0001;

    private static Parcel parcel(String id, String weightKg, String city, String days, String express) {
        return Parcel.fromCsv(String.join(";", id, weightKg, city, days, express));
    }

    /* Порожній набір не ділить на нуль і не має посилки з найбільшим терміном. */
    @Test
    void emptySummaryHasZeroMetrics() {
        DeliverySummary summary = new DeliverySummary();

        assertTrue(summary.isEmpty());
        assertEquals(0, summary.count());
        assertEquals(0.0, summary.averageWeightKg(), DELTA);
        assertEquals(0, summary.maxDeliveryDays());
        assertEquals("", summary.longestDeliveryId());
        assertEquals(0, summary.expressCount());
    }

    /* Середнє обчислюється в double і не втрачає дробової частини. */
    @Test
    void averageWeightKeepsFraction() {
        DeliverySummary summary = new DeliverySummary();
        summary.add(parcel("UA-1", "1.00", "Київ", "1", "false"));
        summary.add(parcel("UA-2", "2.00", "Київ", "1", "false"));
        summary.add(parcel("UA-3", "2.50", "Київ", "1", "false"));

        assertFalse(summary.isEmpty());
        assertEquals(3, summary.count());
        assertEquals(5.5 / 3, summary.averageWeightKg(), DELTA);
    }

    /* Найбільший термін запам'ятовується разом з ідентифікатором і містом посилки. */
    @Test
    void remembersLongestDelivery() {
        DeliverySummary summary = new DeliverySummary();
        summary.add(parcel("UA-1", "1.00", "Київ", "2", "false"));
        summary.add(parcel("UA-2", "1.00", "Ужгород", "5", "false"));
        summary.add(parcel("UA-3", "1.00", "Львів", "3", "false"));

        assertEquals(5, summary.maxDeliveryDays());
        assertEquals("UA-2", summary.longestDeliveryId());
        assertEquals("Ужгород", summary.longestDeliveryCity());
    }

    /* За однакового терміну лишається перша посилка; нульовий термін теж є максимумом. */
    @Test
    void keepsFirstParcelOnEqualDays() {
        DeliverySummary summary = new DeliverySummary();
        summary.add(parcel("UA-1", "1.00", "Київ", "0", "false"));
        assertEquals("UA-1", summary.longestDeliveryId());

        summary.add(parcel("UA-2", "1.00", "Київ", "4", "false"));
        summary.add(parcel("UA-3", "1.00", "Львів", "4", "false"));

        assertEquals(4, summary.maxDeliveryDays());
        assertEquals("UA-2", summary.longestDeliveryId());
    }

    /* Рахуються лише записи з express=true. */
    @Test
    void countsExpressDeliveries() {
        DeliverySummary summary = new DeliverySummary();
        summary.add(parcel("UA-1", "1.00", "Київ", "1", "true"));
        summary.add(parcel("UA-2", "1.00", "Київ", "1", "false"));
        summary.add(parcel("UA-3", "1.00", "Київ", "1", "true"));

        assertEquals(2, summary.expressCount());
    }

    /* Показники обчислюються зі списку об'єктів за тими самими формулами. */
    @Test
    void calculatesMetricsFromList() {
        // Arrange
        List<Parcel> parcels = List.of(
                parcel("UA-1", "1.00", "Київ", "2", "true"),
                parcel("UA-2", "2.00", "Ужгород", "5", "false"),
                parcel("UA-3", "3.00", "Львів", "5", "true"));

        // Act
        DeliverySummary summary = DeliverySummary.of(parcels);

        // Assert
        assertEquals(3, summary.count());
        assertEquals(2.0, summary.averageWeightKg(), DELTA);
        assertEquals(5, summary.maxDeliveryDays());
        assertEquals("UA-2", summary.longestDeliveryId());
        assertEquals("Ужгород", summary.longestDeliveryCity());
        assertEquals(2, summary.expressCount());
    }

    /* Порожній список дає порожні показники й не називає посилку з найбільшим терміном. */
    @Test
    void emptyListGivesEmptySummary() {
        DeliverySummary summary = DeliverySummary.of(List.of());

        assertTrue(summary.isEmpty());
        assertEquals("", summary.longestDeliveryId());
        assertEquals("", summary.longestDeliveryCity());
    }

    /* Середня вага звіту округлюється так само, як у попередніх роботах, а не як у summarizingDouble. */
    @Test
    void averageKeepsArithmeticOfPreviousWorks() {
        // Arrange
        List<Parcel> parcels = List.of(
                new RegularParcel("UA-1", 3.546, "Київ", 1),
                new RegularParcel("UA-2", 1.279, "Львів", 1),
                new RegularParcel("UA-3", 4.662, "Одеса", 1),
                new RegularParcel("UA-4", 0.813, "Суми", 1));

        // Act
        double average = DeliverySummary.of(parcels).averageWeightKg();

        // Assert
        assertEquals("2.57", String.format(Locale.ROOT, "%.2f", average));
        assertEquals("2.58", String.format(Locale.ROOT, "%.2f", ParcelQueries.weightStatistics(parcels).getAverage()));
    }

    /* Кількість за видами визначає фактичний підтип кожної посилки. */
    @Test
    void countsParcelsByKind() {
        DeliverySummary summary = DeliverySummary.of(List.of(
                new RegularParcel("UA-1", 1.0, "Київ", 1),
                new ExpressParcel("UA-2", 1.0, "Львів", 1),
                new ExpressParcel("UA-3", 1.0, "Одеса", 1)));

        assertEquals(1, summary.countOf(DeliveryKind.REGULAR));
        assertEquals(2, summary.countOf(DeliveryKind.EXPRESS));
        assertEquals(2, summary.expressCount());
    }

    /* Без посилок кількість кожного виду й загальна вартість дорівнюють нулю. */
    @Test
    void emptySummaryHasNoKindsAndCost() {
        DeliverySummary summary = new DeliverySummary();

        assertEquals(0, summary.countOf(DeliveryKind.REGULAR));
        assertEquals(0, summary.countOf(DeliveryKind.EXPRESS));
        assertEquals(0.0, summary.totalCost(), DELTA);
    }

    /* Найбільший термін обчислює фактичний підтип: 2 + 3 дні проти 6 − 4 днів. */
    @Test
    void usesCalculatedDaysOfActualSubtype() {
        DeliverySummary summary = DeliverySummary.of(List.of(
                new RegularParcel("UA-1", 1.0, "Київ", 2, 3),
                new ExpressParcel("UA-2", 1.0, "Львів", 6, 4)));

        assertEquals(5, summary.maxDeliveryDays());
        assertEquals("UA-1", summary.longestDeliveryId());
        assertEquals("Київ", summary.longestDeliveryCity());
    }

    /* Загальна вартість складається з вартостей різних підтипів: 60 грн + 140 грн. */
    @Test
    void sumsCostOfDifferentSubtypes() {
        DeliverySummary summary = DeliverySummary.of(List.of(
                new RegularParcel("UA-1", 2.0, "Київ", 1),
                new ExpressParcel("UA-2", 2.0, "Львів", 3, 2)));

        assertEquals(200.0, summary.totalCost(), DELTA);
    }
}
