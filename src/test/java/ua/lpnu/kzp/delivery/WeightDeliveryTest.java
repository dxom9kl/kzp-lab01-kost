package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WeightDeliveryTest {
    private static final double DELTA = 0.0001;

    private static Parcel parcel(double weightKg, int deliveryDays) {
        return new Parcel("UA-1", weightKg, "Київ", deliveryDays, false);
    }

    /* Методи доступу повертають значення компонентів. */
    @Test
    void exposesComponents() {
        WeightDelivery totals = new WeightDelivery(3, 7.5, 4);

        assertEquals(3, totals.count());
        assertEquals(7.5, totals.totalWeightKg(), DELTA);
        assertEquals(4, totals.maxDeliveryDays());
    }

    /* Record порівнюється за значеннями компонентів, а не за посиланням. */
    @Test
    void usesValueEquality() {
        WeightDelivery first = new WeightDelivery(3, 7.5, 4);
        WeightDelivery second = new WeightDelivery(3, 7.5, 4);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new WeightDelivery(3, 7.5, 5));
    }

    /* Компілятор створює toString з назвами й значеннями компонентів. */
    @Test
    void generatesToString() {
        assertEquals("WeightDelivery[count=3, totalWeightKg=7.5, maxDeliveryDays=4]",
                new WeightDelivery(3, 7.5, 4).toString());
    }

    /* Порожній підсумок не ділить на нуль. */
    @Test
    void emptyTotalsHaveZeroAverage() {
        assertEquals(new WeightDelivery(0, 0.0, 0), WeightDelivery.EMPTY);
        assertEquals(0.0, WeightDelivery.EMPTY.averageWeightKg(), DELTA);
    }

    /* Додавання посилки створює новий підсумок і не змінює попередній. */
    @Test
    void plusReturnsNewTotals() {
        // Arrange
        WeightDelivery before = new WeightDelivery(1, 2.0, 5);

        // Act
        WeightDelivery after = before.plus(parcel(3.0, 2));

        // Assert
        assertEquals(new WeightDelivery(2, 5.0, 5), after);
        assertEquals(new WeightDelivery(1, 2.0, 5), before);
    }

    /* Більший термін замінює попередній максимум. */
    @Test
    void plusRaisesMaxDeliveryDays() {
        assertEquals(7, WeightDelivery.EMPTY.plus(parcel(1.0, 2)).plus(parcel(1.0, 7)).maxDeliveryDays());
    }

    /* Середнє обчислюється в double і не втрачає дробової частини. */
    @Test
    void averageWeightKeepsFraction() {
        assertEquals(2.5, new WeightDelivery(2, 5.0, 1).averageWeightKg(), DELTA);
    }

    /* Сума двох найбільших ваг переповнює double, але не перериває обробку файла. */
    @Test
    void keepsOverflowedTotalAsInfinity() {
        WeightDelivery totals = WeightDelivery.EMPTY
                .plus(parcel(Double.MAX_VALUE, 1))
                .plus(parcel(Double.MAX_VALUE, 1));

        assertEquals(Double.POSITIVE_INFINITY, totals.totalWeightKg());
    }

    /* Від'ємні значення та NaN не є допустимим підсумком. */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidTotals")
    void rejectsInvalidTotals(int count, double totalWeightKg, int maxDeliveryDays, String reason) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new WeightDelivery(count, totalWeightKg, maxDeliveryDays));

        assertEquals(reason, exception.getMessage());
    }

    static Stream<Arguments> invalidTotals() {
        return Stream.of(
                arguments(-1, 1.0, 1, "кількість посилок не може бути від'ємною: -1"),
                arguments(1, -0.5, 1, "сумарна вага не може бути від'ємною або NaN: -0.5"),
                arguments(1, Double.NaN, 1, "сумарна вага не може бути від'ємною або NaN: NaN"),
                arguments(1, Double.NEGATIVE_INFINITY, 1,
                        "сумарна вага не може бути від'ємною або NaN: -Infinity"),
                arguments(1, 1.0, -1, "найбільший термін не може бути від'ємним: -1"));
    }
}
