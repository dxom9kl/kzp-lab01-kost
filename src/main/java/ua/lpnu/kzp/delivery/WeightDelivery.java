package ua.lpnu.kzp.delivery;

import java.util.Locale;

/**
 * Незмінний проміжний підсумок за посилками: кількість, сумарна вага й найбільший термін.
 *
 * <p>Додавання посилки створює новий підсумок, а попередній лишається без змін.
 *
 * @param count кількість посилок
 * @param totalWeightKg сумарна вага в кілограмах
 * @param maxDeliveryDays найбільший термін доставлення в днях
 */
public record WeightDelivery(int count, double totalWeightKg, int maxDeliveryDays) {
    /** Підсумок порожнього набору посилок. */
    public static final WeightDelivery EMPTY = new WeightDelivery(0, 0.0, 0);

    /**
     * Перевіряє допустимість підсумкових значень.
     *
     * @throws IllegalArgumentException якщо значення від'ємне або сумарна вага є {@code NaN}
     */
    public WeightDelivery {
        if (count < 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "кількість посилок не може бути від'ємною: %d", count));
        }
        // Сума дуже великих скінченних ваг може дати +Infinity: звіт показує її, а не зупиняє програму.
        if (Double.isNaN(totalWeightKg) || totalWeightKg < 0) {
            throw new IllegalArgumentException(
                    "сумарна вага не може бути від'ємною або NaN: %s".formatted(totalWeightKg));
        }
        if (maxDeliveryDays < 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "найбільший термін не може бути від'ємним: %d", maxDeliveryDays));
        }
    }

    /**
     * Додає посилку до підсумку.
     *
     * @param parcel коректна посилка
     * @return новий підсумок із урахуванням посилки
     */
    public WeightDelivery plus(Parcel parcel) {
        return new WeightDelivery(count + 1, totalWeightKg + parcel.getWeightKg(),
                Math.max(maxDeliveryDays, parcel.getDeliveryDays()));
    }

    /**
     * Обчислює середню вагу посилки.
     *
     * @return середня вага в кілограмах або {@code 0.0}, якщо посилок немає
     */
    public double averageWeightKg() {
        // Порожній набір обробляємо окремо, щоб не ділити на нуль.
        return count == 0 ? 0.0 : totalWeightKg / count;
    }
}
