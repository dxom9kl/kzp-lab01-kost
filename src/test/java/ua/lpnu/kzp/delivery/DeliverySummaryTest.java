package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeliverySummaryTest {
    private static final double DELTA = 0.0001;

    private static String[] parcel(String id, String weightKg, String city, String days, String express) {
        return new String[] {id, weightKg, city, days, express};
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
}
