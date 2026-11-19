package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DeliveryKindTest {
    /* Варіант має рівно два види доставлення. */
    @Test
    void hasExactlyTwoKinds() {
        assertArrayEquals(new DeliveryKind[] {DeliveryKind.REGULAR, DeliveryKind.EXPRESS}, DeliveryKind.values());
    }

    /* Кожен вид має підпис для текстового подання посилки. */
    @Test
    void providesLabels() {
        assertEquals("звичайне", DeliveryKind.REGULAR.label());
        assertEquals("експрес", DeliveryKind.EXPRESS.label());
    }

    /* Кожна константа існує в одному екземплярі, тому значення порівнюють через ==. */
    @Test
    void valueOfReturnsSameInstance() {
        assertSame(DeliveryKind.EXPRESS, DeliveryKind.valueOf("EXPRESS"));
    }

    /* Довільний текст не стає видом доставлення. */
    @Test
    void rejectsUnknownName() {
        assertThrows(IllegalArgumentException.class, () -> DeliveryKind.valueOf("ШВИДКЕ"));
    }
}
