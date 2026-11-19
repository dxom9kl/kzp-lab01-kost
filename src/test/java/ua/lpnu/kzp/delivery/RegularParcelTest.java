package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegularParcelTest {
    private static final double DELTA = 0.0001;

    /* Запис із файла не має резерву, тому розрахунковий термін дорівнює плановому. */
    @Test
    void parcelWithoutReserveKeepsPlannedDays() {
        RegularParcel parcel = new RegularParcel("UA-1", 2.0, "Львів", 3);

        assertEquals(0, parcel.getReserveDays());
        assertEquals(3, parcel.calculatedDays());
        assertEquals(DeliveryKind.REGULAR, parcel.getKind());
        assertFalse(parcel.isExpress());
    }

    /* Резерв днів додається до планового терміну, а сам плановий термін не змінюється. */
    @Test
    void addsReserveToPlannedDays() {
        // Arrange і Act
        RegularParcel parcel = new RegularParcel("UA-1", 2.0, "Львів", 3, 2);

        // Assert
        assertEquals(2, parcel.getReserveDays());
        assertEquals(3, parcel.getDeliveryDays());
        assertEquals(5, parcel.calculatedDays());
    }

    /* Вартість дорівнює базовому тарифу: 40 грн + 2 кг × 10 грн. */
    @Test
    void costEqualsBaseTariff() {
        assertEquals(60.0, new RegularParcel("UA-1", 2.0, "Львів", 3).calculatedCost(), DELTA);
    }

    /* Найбільший резерв, за якого розрахунковий термін ще вміщується в int. */
    @Test
    void acceptsLargestReserve() {
        RegularParcel parcel = new RegularParcel("UA-1", 1.0, "Київ", Integer.MAX_VALUE - 5, 5);

        assertEquals(Integer.MAX_VALUE, parcel.calculatedDays());
    }

    /* Від'ємний резерв не має предметного змісту. */
    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void rejectsNegativeReserve(int reserveDays) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new RegularParcel("UA-1", 1.0, "Київ", 3, reserveDays));

        assertEquals("від'ємний резерв днів: %d".formatted(reserveDays), exception.getMessage());
    }

    /* Резерв, який переповнив би розрахунковий термін, відхиляється. */
    @Test
    void rejectsReserveThatOverflowsDays() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new RegularParcel("UA-1", 1.0, "Київ", Integer.MAX_VALUE, 1));

        assertEquals("резерв днів завеликий: 1", exception.getMessage());
    }
}
