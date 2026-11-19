package ua.lpnu.kzp.delivery;

import java.util.List;

/**
 * Накопичує чотири показники варіанта за коректними посилками:
 * кількість записів, середню вагу, найбільший термін доставлення та кількість
 * експрес-доставлень.
 */
public final class DeliverySummary {
    private WeightDelivery totals = WeightDelivery.EMPTY;
    private int expressCount;
    private Parcel longestDelivery;

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
     * @param parcel коректна посилка
     */
    public void add(Parcel parcel) {
        // Перший запис задає початковий максимум; за рівного терміну лишається перша посилка.
        if (longestDelivery == null || parcel.getDeliveryDays() > longestDelivery.getDeliveryDays()) {
            longestDelivery = parcel;
        }
        totals = totals.plus(parcel);
        if (parcel.isExpress()) {
            expressCount++;
        }
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
        return expressCount;
    }
}
