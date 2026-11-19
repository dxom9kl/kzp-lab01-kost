package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ParcelEqualityTest {
    private static Parcel regular() {
        return new RegularParcel("UA-1", 2.5, "Львів", 3, 1);
    }

    private static Parcel express() {
        return new ExpressParcel("UA-1", 2.5, "Львів", 3, 1);
    }

    /* Посилки одного підтипу з однаковими полями рівні й мають однаковий хеш-код. */
    @Test
    void equalParcelsHaveSameHashCode() {
        assertEquals(regular(), regular());
        assertEquals(regular().hashCode(), regular().hashCode());
        assertEquals(express(), express());
        assertEquals(express().hashCode(), express().hashCode());
    }

    /* Рівність рефлексивна, симетрична й транзитивна. */
    @Test
    void equalityFollowsContract() {
        Parcel first = regular();
        Parcel second = regular();
        Parcel third = regular();

        assertTrue(first.equals(first));
        assertTrue(first.equals(second) && second.equals(first));
        assertTrue(first.equals(second) && second.equals(third) && first.equals(third));
    }

    /* Посилка не дорівнює null і об'єкту іншого типу. */
    @Test
    void differsFromNullAndOtherTypes() {
        assertFalse(regular().equals(null));
        assertFalse(express().equals("UA-1"));
    }

    /* Різні підтипи з однаковими спільними полями не рівні, бо по-різному обчислюють термін і вартість. */
    @Test
    void differentSubtypesAreNotEqual() {
        Parcel regular = new RegularParcel("UA-1", 2.5, "Львів", 3);
        Parcel express = new ExpressParcel("UA-1", 2.5, "Львів", 3);

        assertNotEquals(regular, express);
        assertNotEquals(express, regular);
    }

    /* Кожне поле звичайної посилки впливає на рівність. */
    @ParameterizedTest
    @MethodSource("changedRegularParcels")
    void everyRegularFieldAffectsEquality(Parcel changed) {
        assertNotEquals(regular(), changed);
    }

    static Stream<Parcel> changedRegularParcels() {
        return Stream.of(
                new RegularParcel("UA-2", 2.5, "Львів", 3, 1),
                new RegularParcel("UA-1", 2.6, "Львів", 3, 1),
                new RegularParcel("UA-1", 2.5, "Київ", 3, 1),
                new RegularParcel("UA-1", 2.5, "Львів", 4, 1),
                new RegularParcel("UA-1", 2.5, "Львів", 3, 2));
    }

    /* Кожне поле експрес-посилки впливає на рівність. */
    @ParameterizedTest
    @MethodSource("changedExpressParcels")
    void everyExpressFieldAffectsEquality(Parcel changed) {
        assertNotEquals(express(), changed);
    }

    static Stream<Parcel> changedExpressParcels() {
        return Stream.of(
                new ExpressParcel("UA-2", 2.5, "Львів", 3, 1),
                new ExpressParcel("UA-1", 2.6, "Львів", 3, 1),
                new ExpressParcel("UA-1", 2.5, "Київ", 3, 1),
                new ExpressParcel("UA-1", 2.5, "Львів", 4, 1),
                new ExpressParcel("UA-1", 2.5, "Львів", 3, 2));
    }

    /* HashSet зберігає лише один із логічно рівних об'єктів. */
    @Test
    void hashSetKeepsOneOfEqualParcels() {
        // Arrange
        List<Parcel> parcels = List.of(regular(), regular(), express(), express(),
                new RegularParcel("UA-1", 2.5, "Львів", 3, 2));

        // Act
        Set<Parcel> unique = new HashSet<>(parcels);

        // Assert
        assertEquals(3, unique.size());
        assertTrue(unique.contains(regular()));
    }

    /* HashMap знаходить значення за логічно рівним ключем. */
    @Test
    void hashMapFindsValueByEqualKey() {
        Map<Parcel, String> statusByParcel = new HashMap<>();
        statusByParcel.put(express(), "у дорозі");

        assertEquals("у дорозі", statusByParcel.get(express()));
        assertNull(statusByParcel.get(regular()));
    }
}
