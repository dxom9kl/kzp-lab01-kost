package ua.lpnu.kzp.delivery;

import java.util.List;
import java.util.Objects;
import ua.lpnu.kzp.storage.Repository;

/**
 * Обчислює показники варіанта за коректними посилками: кількість записів, середню вагу,
 * найбільший термін доставлення, кількість експрес-доставлень і загальну вартість.
 *
 * <p>Клас зберігає посилки спільного типу {@link Parcel} у сховищі {@link Repository}, а показники обчислює
 * потоковими запитами {@link ParcelQueries} над його незмінним знімком, тому новий підтип посилки не
 * потребує змін цього класу.
 */
public final class DeliverySummary {
    private final Repository<Parcel> parcels = new Repository<>();

    /** Створює порожній набір показників. */
    public DeliverySummary() {
        // Початковий стан задано в оголошеннях полів.
    }

    /**
     * Створює набір показників для списку посилок.
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
        parcels.add(Objects.requireNonNull(parcel, "посилка не може бути null"));
    }

    /**
     * Повертає кількість коректних записів.
     *
     * @return кількість доданих записів
     */
    public int count() {
        return parcels.size();
    }

    /**
     * Перевіряє, чи додано хоча б один запис.
     *
     * @return {@code true}, якщо коректних записів немає
     */
    public boolean isEmpty() {
        return parcels.size() == 0;
    }

    /**
     * Обчислює середню вагу посилки.
     *
     * @return середня вага в кілограмах або {@code 0.0}, якщо записів немає
     */
    public double averageWeightKg() {
        return ParcelQueries.totals(parcels.all()).averageWeightKg();
    }

    /**
     * Повертає найбільший термін доставлення.
     *
     * @return термін у днях або {@code 0}, якщо записів немає
     */
    public int maxDeliveryDays() {
        return ParcelQueries.totals(parcels.all()).maxDeliveryDays();
    }

    /**
     * Повертає ідентифікатор посилки з найбільшим терміном доставлення.
     *
     * @return ідентифікатор або порожній рядок, якщо записів немає
     */
    public String longestDeliveryId() {
        return ParcelQueries.longestDelivery(parcels.all()).map(Parcel::getId).orElse("");
    }

    /**
     * Повертає місто посилки з найбільшим терміном доставлення.
     *
     * @return назва міста або порожній рядок, якщо записів немає
     */
    public String longestDeliveryCity() {
        return ParcelQueries.longestDelivery(parcels.all()).map(Parcel::getCity).orElse("");
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
        return ParcelQueries.countByKind(parcels.all()).getOrDefault(kind, 0L).intValue();
    }

    /**
     * Повертає загальну вартість доставлення всіх посилок.
     *
     * @return вартість у гривнях або {@code 0.0}, якщо записів немає
     */
    public double totalCost() {
        return ParcelQueries.totalCost(parcels.all());
    }
}
