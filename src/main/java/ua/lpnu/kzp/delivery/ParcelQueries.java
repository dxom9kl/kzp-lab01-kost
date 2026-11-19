package ua.lpnu.kzp.delivery;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Потокові запити до колекції посилок.
 *
 * <p>Кожен метод створює власний потік, не змінює вхідний список і працює зі спільним типом
 * {@link Parcel}, тому не залежить від конкретних підтипів посилок.
 */
public final class ParcelQueries {
    /** Кількість найважчих посилок за умовою варіанта. */
    public static final int TOP_SIZE = 5;

    /* Вага за спаданням, а за однакової ваги – номер за зростанням. */
    private static final Comparator<Parcel> BY_WEIGHT_THEN_ID = Comparator
            .comparingDouble(Parcel::getWeightKg)
            .reversed()
            .thenComparing(Parcel::getId);

    /* Забороняє створення екземплярів службового класу. */
    private ParcelQueries() {
    }

    /**
     * Відбирає посилки, які на контрольний день ще в дорозі.
     *
     * @param parcels посилки
     * @return посилки зі статусом {@link DeliveryStatus#IN_TRANSIT} у початковому порядку
     */
    public static List<Parcel> inTransit(List<Parcel> parcels) {
        return streamOf(parcels)
                .filter(parcel -> parcel.getStatus() == DeliveryStatus.IN_TRANSIT)
                .toList();
    }

    /**
     * Повертає міста доставлення всіх посилок.
     *
     * @param parcels посилки
     * @return міста в початковому порядку разом із повторами
     */
    public static List<String> cities(List<Parcel> parcels) {
        return streamOf(parcels)
                .map(Parcel::getCity)
                .toList();
    }

    /**
     * Рахує посилки в кожному місті.
     *
     * @param parcels посилки
     * @return кількість посилок за містом, упорядкована за назвою міста
     */
    public static Map<String, Long> countByCity(List<Parcel> parcels) {
        return streamOf(parcels)
                .collect(Collectors.groupingBy(Parcel::getCity, TreeMap::new, Collectors.counting()));
    }

    /**
     * Обчислює зведену статистику ваги.
     *
     * @param parcels посилки
     * @return кількість, сума, середнє, мінімум і максимум ваги
     */
    public static DoubleSummaryStatistics weightStatistics(List<Parcel> parcels) {
        return streamOf(parcels)
                .collect(Collectors.summarizingDouble(Parcel::getWeightKg));
    }

    /**
     * Вибирає найважчі посилки.
     *
     * @param parcels посилки
     * @param limit найбільша кількість посилок у результаті, не від'ємна
     * @return посилки за спаданням ваги, а за однакової ваги – за зростанням номера
     * @throws IllegalArgumentException якщо {@code limit} від'ємний
     */
    public static List<Parcel> topByWeight(List<Parcel> parcels, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "кількість посилок не може бути від'ємною: %d", limit));
        }
        // Спершу впорядковується весь набір, а limit залишає лише перші елементи.
        return streamOf(parcels)
                .sorted(BY_WEIGHT_THEN_ID)
                .limit(limit)
                .toList();
    }

    /**
     * Шукає посилку за номером.
     *
     * @param parcels посилки
     * @param id номер посилки
     * @return перша посилка з таким номером або порожній результат
     */
    public static Optional<Parcel> findById(List<Parcel> parcels, String id) {
        Objects.requireNonNull(id, "номер посилки не може бути null");
        return streamOf(parcels)
                .filter(parcel -> parcel.getId().equals(id))
                .findFirst();
    }

    /**
     * Обчислює проміжний підсумок послідовним додаванням посилок.
     *
     * <p>{@link #weightStatistics(List)} використовує компенсоване підсумовування, тому його середнє
     * може округлитися інакше. Показник звіту обчислюється так само, як у попередніх роботах.
     *
     * @param parcels посилки
     * @return кількість посилок, сумарна вага й найбільший розрахунковий термін
     */
    public static WeightDelivery totals(List<Parcel> parcels) {
        return streamOf(parcels).reduce(WeightDelivery.EMPTY, WeightDelivery::plus, WeightDelivery::merge);
    }

    /**
     * Шукає посилку з найбільшим розрахунковим терміном.
     *
     * @param parcels посилки
     * @return перша з посилок із найбільшим терміном або порожній результат
     */
    public static Optional<Parcel> longestDelivery(List<Parcel> parcels) {
        // Строге порівняння залишає першу посилку серед рівних за терміном.
        return streamOf(parcels)
                .reduce((longest, next) -> next.calculatedDays() > longest.calculatedDays() ? next : longest);
    }

    /**
     * Рахує посилки кожного виду доставлення.
     *
     * @param parcels посилки
     * @return кількість посилок за видом доставлення
     */
    public static Map<DeliveryKind, Long> countByKind(List<Parcel> parcels) {
        return streamOf(parcels).collect(Collectors.groupingBy(
                Parcel::getKind, () -> new EnumMap<>(DeliveryKind.class), Collectors.counting()));
    }

    /**
     * Обчислює загальну вартість доставлення.
     *
     * @param parcels посилки
     * @return сума вартостей, які повертають фактичні підтипи посилок
     */
    public static double totalCost(List<Parcel> parcels) {
        return streamOf(parcels)
                .mapToDouble(Parcel::calculatedCost)
                .sum();
    }

    /* Кожен запит отримує новий потік, бо використаний потік не можна запустити вдруге. */
    private static Stream<Parcel> streamOf(List<Parcel> parcels) {
        return Objects.requireNonNull(parcels, "список посилок не може бути null").stream();
    }
}
