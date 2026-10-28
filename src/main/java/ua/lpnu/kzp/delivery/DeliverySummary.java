package ua.lpnu.kzp.delivery;

/**
 * Накопичує чотири показники варіанта за коректними записами про посилки:
 * кількість записів, середню вагу, найбільший термін доставлення та кількість
 * експрес-доставлень.
 */
public final class DeliverySummary {
    private int count;
    private double totalWeightKg;
    private int expressCount;
    private int maxDeliveryDays;
    private String longestDeliveryId = "";
    private String longestDeliveryCity = "";

    /**
     * Додає перевірений запис до показників.
     *
     * @param fields поля запису, які повернув {@link ParcelParser#parse(String)}
     */
    public void add(String[] fields) {
        double weightKg = Double.parseDouble(fields[ParcelParser.WEIGHT_KG]);
        int deliveryDays = Integer.parseInt(fields[ParcelParser.DELIVERY_DAYS]);
        // Перший запис задає початковий максимум; за рівного терміну лишається перша посилка.
        if (count == 0 || deliveryDays > maxDeliveryDays) {
            maxDeliveryDays = deliveryDays;
            longestDeliveryId = fields[ParcelParser.ID];
            longestDeliveryCity = fields[ParcelParser.CITY];
        }
        count++;
        totalWeightKg += weightKg;
        if (Boolean.parseBoolean(fields[ParcelParser.EXPRESS])) {
            expressCount++;
        }
    }

    /**
     * Повертає кількість коректних записів.
     *
     * @return кількість доданих записів
     */
    public int count() {
        return count;
    }

    /**
     * Перевіряє, чи додано хоча б один запис.
     *
     * @return {@code true}, якщо коректних записів немає
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Обчислює середню вагу посилки.
     *
     * @return середня вага в кілограмах або {@code 0.0}, якщо записів немає
     */
    public double averageWeightKg() {
        // Порожній набір обробляємо окремо, щоб не ділити на нуль.
        return count == 0 ? 0.0 : totalWeightKg / count;
    }

    /**
     * Повертає найбільший термін доставлення.
     *
     * @return термін у днях або {@code 0}, якщо записів немає
     */
    public int maxDeliveryDays() {
        return maxDeliveryDays;
    }

    /**
     * Повертає ідентифікатор посилки з найбільшим терміном доставлення.
     *
     * @return ідентифікатор або порожній рядок, якщо записів немає
     */
    public String longestDeliveryId() {
        return longestDeliveryId;
    }

    /**
     * Повертає місто посилки з найбільшим терміном доставлення.
     *
     * @return назва міста або порожній рядок, якщо записів немає
     */
    public String longestDeliveryCity() {
        return longestDeliveryCity;
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
