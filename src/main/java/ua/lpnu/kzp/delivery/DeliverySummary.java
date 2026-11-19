package ua.lpnu.kzp.delivery;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Накопичує показники варіанта за коректними посилками: кількість записів, середню вагу,
 * найбільший термін доставлення, кількість експрес-доставлень і загальну вартість.
 *
 * <p>Клас працює лише зі спільним типом {@link Parcel}, тому новий підтип посилки не потребує його змін.
 */
public final class DeliverySummary {
    private final Map<DeliveryKind, Integer> countByKind = new EnumMap<>(DeliveryKind.class);
    private WeightDelivery totals = WeightDelivery.EMPTY;
    private double totalCost;
    private Parcel longestDelivery;

    /** Створює порожній набір показників. */
    public DeliverySummary() {
        // Початковий стан задано в оголошеннях полів.
    }

    /**
     * Обчислює показники за списком посилок.
     *
     * @param parcels коректні посилки в порядку вхідного файла
     * @return показники всіх посилок списку
     */
    public static DeliverySummary of(List<Parcel> parcels) {
        DeliverySummary summary = new DeliverySummary();
        parcels.forEach(summary::add);
        return summary;
    }

    /**
     * Додає посилку до показників.
     *
     * @param parcel коректна посилка будь-якого підтипу
     */
    public void add(Parcel parcel) {
        // Перший запис задає початковий максимум; за рівного терміну лишається перша посилка.
        if (longestDelivery == null || parcel.calculatedDays() > totals.maxDeliveryDays()) {
            longestDelivery = parcel;
        }
        totals = totals.plus(parcel);
        // Вид і вартість повертає фактичний підтип, тому розгалуження за типом посилки не потрібне.
        countByKind.merge(parcel.getKind(), 1, Integer::sum);
        totalCost += parcel.calculatedCost();
    }

    /**
     * Повертає кількість коректних записів.
     *
     * @return кількість доданих записів
     */
    public int count() {
        return totals.count();
    }

    /**
     * Перевіряє, чи додано хоча б один запис.
     *
     * @return {@code true}, якщо коректних записів немає
     */
    public boolean isEmpty() {
        return totals.count() == 0;
    }

    /**
     * Обчислює середню вагу посилки.
     *
     * @return середня вага в кілограмах або {@code 0.0}, якщо записів немає
     */
    public double averageWeightKg() {
        return totals.averageWeightKg();
    }

    /**
     * Повертає найбільший термін доставлення.
     *
     * @return термін у днях або {@code 0}, якщо записів немає
     */
    public int maxDeliveryDays() {
        return totals.maxDeliveryDays();
    }

    /**
     * Повертає ідентифікатор посилки з найбільшим терміном доставлення.
     *
     * @return ідентифікатор або порожній рядок, якщо записів немає
     */
    public String longestDeliveryId() {
        return longestDelivery == null ? "" : longestDelivery.getId();
    }

    /**
     * Повертає місто посилки з найбільшим терміном доставлення.
     *
     * @return назва міста або порожній рядок, якщо записів немає
     */
    public String longestDeliveryCity() {
        return longestDelivery == null ? "" : longestDelivery.getCity();
    }

    /**
     * Повертає кількість експрес-доставлень.
     *
     * @return кількість записів з ознакою {@code express=true}
     */
    public int expressCount() {
        return countOf(DeliveryKind.EXPRESS);
    }

    /**
     * Повертає кількість посилок заданого виду доставлення.
     *
     * @param kind вид доставлення
     * @return кількість посилок цього виду
     */
    public int countOf(DeliveryKind kind) {
        return countByKind.getOrDefault(kind, 0);
    }

    /**
     * Повертає загальну вартість доставлення всіх посилок.
     *
     * @return вартість у гривнях або {@code 0.0}, якщо записів немає
     */
    public double totalCost() {
        return totalCost;
    }
}
