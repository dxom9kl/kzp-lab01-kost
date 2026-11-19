package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExpressParcelTest {
    private static final double DELTA = 0.0001;

    /* Запис із файла не має прискорення, тому розрахунковий термін дорівнює плановому. */
    @Test
    void parcelWithoutAccelerationKeepsPlannedDays() {
        ExpressParcel parcel = new ExpressParcel("UA-1", 2.0, "Львів", 3);

        assertEquals(0, parcel.getAcceleratedDays());
        assertEquals(3, parcel.calculatedDays());
        assertEquals(DeliveryKind.EXPRESS, parcel.getKind());
        assertTrue(parcel.isExpress());
    }

    /* Прискорення скорочує розрахунковий термін, а плановий термін не змінюється. */
    @Test
    void subtractsAccelerationFromPlannedDays() {
        // Arrange і Act
        ExpressParcel parcel = new ExpressParcel("UA-1", 2.0, "Львів", 3, 2);

        // Assert
        assertEquals(2, parcel.getAcceleratedDays());
        assertEquals(3, parcel.getDeliveryDays());
        assertEquals(1, parcel.calculatedDays());
    }

    /* Без прискорення вартість містить лише надбавку: (40 грн + 2 кг × 10 грн) × 1.5. */
    @Test
    void costContainsExpressRate() {
        assertEquals(90.0, new ExpressParcel("UA-1", 2.0, "Львів", 3).calculatedCost(), DELTA);
    }

    /* Кожен день прискорення додає 25 грн: 90 грн + 2 × 25 грн. */
    @Test
    void costGrowsWithAcceleration() {
        assertEquals(140.0, new ExpressParcel("UA-1", 2.0, "Львів", 3, 2).calculatedCost(), DELTA);
    }

    /* Найбільше прискорення дає доставлення в день відправлення. */
    @Test
    void acceptsAccelerationEqualToPlannedDays() {
        assertEquals(0, new ExpressParcel("UA-1", 1.0, "Київ", 4, 4).calculatedDays());
    }

    /* Від'ємне прискорення не має предметного змісту. */
    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void rejectsNegativeAcceleration(int acceleratedDays) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ExpressParcel("UA-1", 1.0, "Київ", 3, acceleratedDays));

        assertEquals("від'ємне прискорення доставлення: %d".formatted(acceleratedDays), exception.getMessage());
    }

    /* Прискорення не може зробити розрахунковий термін від'ємним. */
    @Test
    void rejectsAccelerationLongerThanPlannedDays() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ExpressParcel("UA-1", 1.0, "Київ", 3, 4));

        assertEquals("прискорення 4 дн. перевищує плановий термін 3 дн.", exception.getMessage());
    }
}
