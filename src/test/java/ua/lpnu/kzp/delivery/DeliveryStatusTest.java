package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeliveryStatusTest {
    /* Посилка має рівно два стани з підписами. */
    @Test
    void providesStatusesWithLabels() {
        assertArrayEquals(new DeliveryStatus[] {DeliveryStatus.DELIVERED, DeliveryStatus.IN_TRANSIT},
                DeliveryStatus.values());
        assertEquals("доставлено", DeliveryStatus.DELIVERED.label());
        assertEquals("у дорозі", DeliveryStatus.IN_TRANSIT.label());
    }

    /* Контрольним є другий день після відправлення. */
    @Test
    void controlDayIsSecondDay() {
        assertEquals(2, DeliveryStatus.CONTROL_DAY);
    }

    /* Значення до, на межі та після контрольного дня. */
    @ParameterizedTest
    @CsvSource({"0, DELIVERED", "1, DELIVERED", "2, DELIVERED", "3, IN_TRANSIT", "6, IN_TRANSIT"})
    void definesStatusByControlDay(int days, DeliveryStatus status) {
        assertEquals(status, DeliveryStatus.ofDays(days));
    }

    /* Стан посилки залежить від розрахункового терміну її фактичного підтипу. */
    @Test
    void parcelStatusUsesCalculatedDays() {
        assertEquals(DeliveryStatus.IN_TRANSIT, new RegularParcel("UA-1", 1.0, "Київ", 2, 1).getStatus());
        assertEquals(DeliveryStatus.DELIVERED, new ExpressParcel("UA-2", 1.0, "Львів", 3, 1).getStatus());
    }
}
