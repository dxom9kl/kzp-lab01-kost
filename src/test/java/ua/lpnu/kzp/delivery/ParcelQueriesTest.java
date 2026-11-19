package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class ParcelQueriesTest {
    private static final double DELTA = 0.0001;

    /* Набір містить межу фільтра, однакову вагу, дубльований номер і зайві елементи для top-5. */
    private static final List<Parcel> PARCELS = List.of(
            new RegularParcel("UA-10", 5.00, "Львів", 3),
            new ExpressParcel("UA-11", 7.50, "Київ", 2),
            new RegularParcel("UA-12", 7.50, "Львів", 4),
            new ExpressParcel("UA-13", 2.25, "Одеса", 1),
            new ExpressParcel("UA-14", 9.00, "Київ", 6),
            new RegularParcel("UA-15", 1.00, "Львів", 0),
            new RegularParcel("UA-11", 3.00, "Суми", 5));

    private static List<String> ids(List<Parcel> parcels) {
        return parcels.stream().map(Parcel::getId).toList();
    }

    /* Фільтр залишає посилки після контрольного дня в початковому порядку. */
    @Test
    void inTransitKeepsParcelsAfterControlDay() {
        assertEquals(List.of("UA-10", "UA-12", "UA-14", "UA-11"), ids(ParcelQueries.inTransit(PARCELS)));
    }

    /* Посилка на межі контрольного дня вже доставлена, а наступний день означає «у дорозі». */
    @Test
    void inTransitChecksControlDayBoundary() {
        Parcel onBoundary = new RegularParcel("UA-1", 1.0, "Київ", DeliveryStatus.CONTROL_DAY);
        Parcel afterBoundary = new RegularParcel("UA-2", 1.0, "Київ", DeliveryStatus.CONTROL_DAY + 1);

        assertEquals(List.of(afterBoundary), ParcelQueries.inTransit(List.of(onBoundary, afterBoundary)));
    }

    /* Перетворення повертає міста всіх посилок разом із повторами. */
    @Test
    void citiesKeepOrderAndDuplicates() {
        assertEquals(List.of("Львів", "Київ", "Львів", "Одеса", "Київ", "Львів", "Суми"),
                ParcelQueries.cities(PARCELS));
    }

    /* Групування рахує посилки в кожному місті й упорядковує міста за назвою. */
    @Test
    void countByCityGroupsParcels() {
        // Act
        Map<String, Long> result = ParcelQueries.countByCity(PARCELS);

        // Assert
        assertEquals(Map.of("Київ", 2L, "Львів", 3L, "Одеса", 1L, "Суми", 1L), result);
        assertEquals(List.of("Київ", "Львів", "Одеса", "Суми"), List.copyOf(result.keySet()));
    }

    /* Статистика містить кількість, суму, середнє, мінімум і максимум ваги. */
    @Test
    void weightStatisticsSummarizesWeights() {
        DoubleSummaryStatistics statistics = ParcelQueries.weightStatistics(PARCELS);

        assertEquals(7, statistics.getCount());
        assertEquals(35.25, statistics.getSum(), DELTA);
        assertEquals(35.25 / 7, statistics.getAverage(), DELTA);
        assertEquals(1.00, statistics.getMin(), DELTA);
        assertEquals(9.00, statistics.getMax(), DELTA);
    }

    /* Top-5 упорядковано за вагою, однакову вагу розрізняє номер, а зайві посилки відкинуто. */
    @Test
    void topByWeightSortsByWeightThenId() {
        List<Parcel> top = ParcelQueries.topByWeight(PARCELS, ParcelQueries.TOP_SIZE);

        assertEquals(List.of("UA-14", "UA-11", "UA-12", "UA-10", "UA-11"), ids(top));
        assertEquals(5, top.size());
    }

    /* Нульова межа дає порожній результат, а завелика повертає всі посилки. */
    @Test
    void topByWeightHandlesLimitBounds() {
        assertTrue(ParcelQueries.topByWeight(PARCELS, 0).isEmpty());
        assertEquals(PARCELS.size(), ParcelQueries.topByWeight(PARCELS, PARCELS.size() + 10).size());
    }

    /* Від'ємна межа є помилкою вхідних даних. */
    @Test
    void topByWeightRejectsNegativeLimit() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ParcelQueries.topByWeight(PARCELS, -1));

        assertEquals("кількість посилок не може бути від'ємною: -1", exception.getMessage());
    }

    /* За дубльованого номера пошук повертає перший запис у початковому порядку. */
    @Test
    void findByIdReturnsFirstOfDuplicates() {
        Optional<Parcel> found = ParcelQueries.findById(PARCELS, "UA-11");

        assertTrue(found.isPresent());
        assertEquals("Київ", found.get().getCity());
    }

    /* Відсутній номер дає порожній результат, а не null. */
    @Test
    void findByIdReportsMissingParcel() {
        assertEquals(Optional.empty(), ParcelQueries.findById(PARCELS, "UA-99"));
    }

    /* Відсутній номер пошуку відхиляється. */
    @Test
    void findByIdRejectsNullId() {
        assertThrows(NullPointerException.class, () -> ParcelQueries.findById(PARCELS, null));
    }

    /* Порожній вхід дає порожні результати всіх запитів. */
    @Test
    void emptyInputGivesEmptyResults() {
        List<Parcel> empty = List.of();

        assertTrue(ParcelQueries.inTransit(empty).isEmpty());
        assertTrue(ParcelQueries.cities(empty).isEmpty());
        assertTrue(ParcelQueries.countByCity(empty).isEmpty());
        assertTrue(ParcelQueries.topByWeight(empty, ParcelQueries.TOP_SIZE).isEmpty());
        assertEquals(Optional.empty(), ParcelQueries.findById(empty, "UA-10"));
    }

    /* Статистика порожнього набору має нульові кількість і суму та спеціальні межі. */
    @Test
    void emptyStatisticsHaveSpecialBounds() {
        DoubleSummaryStatistics statistics = ParcelQueries.weightStatistics(List.of());

        assertEquals(0, statistics.getCount());
        assertEquals(0.0, statistics.getSum(), DELTA);
        assertEquals(0.0, statistics.getAverage(), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, statistics.getMin());
        assertEquals(Double.NEGATIVE_INFINITY, statistics.getMax());
    }

    /* Запити не змінюють склад і порядок вхідної колекції. */
    @Test
    void queriesDoNotChangeInput() {
        // Arrange
        List<Parcel> input = new ArrayList<>(PARCELS);

        // Act
        ParcelQueries.inTransit(input);
        ParcelQueries.cities(input);
        ParcelQueries.countByCity(input);
        ParcelQueries.weightStatistics(input);
        ParcelQueries.topByWeight(input, ParcelQueries.TOP_SIZE);
        ParcelQueries.findById(input, "UA-11");
        ParcelQueries.totals(input);
        ParcelQueries.longestDelivery(input);
        ParcelQueries.countByKind(input);
        ParcelQueries.totalCost(input);

        // Assert
        assertEquals(PARCELS, input);
    }

    /* Підсумок збігається з послідовним додаванням посилок. */
    @Test
    void totalsMatchSequentialAddition() {
        WeightDelivery expected = WeightDelivery.EMPTY;
        for (Parcel parcel : PARCELS) {
            expected = expected.plus(parcel);
        }

        assertEquals(expected, ParcelQueries.totals(PARCELS));
        assertEquals(WeightDelivery.EMPTY, ParcelQueries.totals(List.of()));
    }

    /* Серед посилок з однаковим найбільшим терміном лишається перша. */
    @Test
    void longestDeliveryKeepsFirstOfEqualDays() {
        List<Parcel> parcels = List.of(
                new RegularParcel("UA-1", 1.0, "Київ", 4),
                new ExpressParcel("UA-2", 1.0, "Львів", 6),
                new RegularParcel("UA-3", 1.0, "Одеса", 6));

        assertEquals("UA-2", ParcelQueries.longestDelivery(parcels).orElseThrow().getId());
        assertEquals(Optional.empty(), ParcelQueries.longestDelivery(List.of()));
    }

    /* Кількість за видами визначають фактичні підтипи посилок. */
    @Test
    void countByKindCountsSubtypes() {
        assertEquals(Map.of(DeliveryKind.REGULAR, 4L, DeliveryKind.EXPRESS, 3L), ParcelQueries.countByKind(PARCELS));
    }

    /* Загальна вартість складається з вартостей різних підтипів. */
    @Test
    void totalCostSumsSubtypeCosts() {
        assertEquals(786.25, ParcelQueries.totalCost(PARCELS), DELTA);
    }

    /* Потокові запити дають ті самі результати, що й циклова реалізація. */
    @Test
    void streamQueriesMatchLoopReference() {
        // Arrange: циклові еталони тих самих запитів.
        List<Parcel> loopInTransit = new ArrayList<>();
        List<String> loopCities = new ArrayList<>();
        Map<String, Long> loopCounts = new TreeMap<>();
        double loopSum = 0.0;
        double loopMin = Double.POSITIVE_INFINITY;
        double loopMax = Double.NEGATIVE_INFINITY;
        Parcel loopFound = null;
        for (Parcel parcel : PARCELS) {
            if (parcel.calculatedDays() > DeliveryStatus.CONTROL_DAY) {
                loopInTransit.add(parcel);
            }
            loopCities.add(parcel.getCity());
            loopCounts.merge(parcel.getCity(), 1L, Long::sum);
            loopSum += parcel.getWeightKg();
            loopMin = Math.min(loopMin, parcel.getWeightKg());
            loopMax = Math.max(loopMax, parcel.getWeightKg());
            if (loopFound == null && "UA-11".equals(parcel.getId())) {
                loopFound = parcel;
            }
        }
        List<Parcel> loopTop = new ArrayList<>(PARCELS);
        loopTop.sort((first, second) -> {
            int byWeight = Double.compare(second.getWeightKg(), first.getWeightKg());
            return byWeight != 0 ? byWeight : first.getId().compareTo(second.getId());
        });

        // Act
        DoubleSummaryStatistics statistics = ParcelQueries.weightStatistics(PARCELS);

        // Assert
        assertEquals(loopInTransit, ParcelQueries.inTransit(PARCELS));
        assertEquals(loopCities, ParcelQueries.cities(PARCELS));
        assertEquals(loopCounts, ParcelQueries.countByCity(PARCELS));
        assertEquals(loopSum, statistics.getSum(), DELTA);
        assertEquals(loopMin, statistics.getMin(), DELTA);
        assertEquals(loopMax, statistics.getMax(), DELTA);
        assertEquals(loopTop.subList(0, ParcelQueries.TOP_SIZE),
                ParcelQueries.topByWeight(PARCELS, ParcelQueries.TOP_SIZE));
        assertSame(loopFound, ParcelQueries.findById(PARCELS, "UA-11").orElseThrow());
    }

    /* Відсутній список відхиляється з поясненням. */
    @Test
    void queriesRejectNullList() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> ParcelQueries.inTransit(null));

        assertEquals("список посилок не може бути null", exception.getMessage());
    }
}
