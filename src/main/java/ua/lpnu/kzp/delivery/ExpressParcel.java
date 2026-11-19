package ua.lpnu.kzp.delivery;

import java.util.Locale;

/**
 * Експрес-посилка: прискорення скорочує розрахунковий термін, а вартість містить надбавку.
 */
public final class ExpressParcel extends Parcel {
    /* Демонстраційні коефіцієнти, а не ціни реального перевізника. */
    private static final double EXPRESS_RATE = 1.5;
    private static final double ACCELERATION_FEE = 25.0;

    private final int acceleratedDays;

    /**
     * Створює експрес-посилку без прискорення, бо вхідний файл його не містить.
     *
     * @param id ідентифікатор посилки, непорожній
     * @param weightKg вага в кілограмах, додатне скінченне число
     * @param city місто доставлення, непорожнє
     * @param deliveryDays плановий термін у днях, не від'ємний
     * @throws NullPointerException якщо {@code id} або {@code city} дорівнює {@code null}
     * @throws IllegalArgumentException якщо значення порушує інваріант посилки
     */
    public ExpressParcel(String id, double weightKg, String city, int deliveryDays) {
        this(id, weightKg, city, deliveryDays, 0);
    }

    /**
     * Створює експрес-посилку із заданим прискоренням.
     *
     * @param id ідентифікатор посилки, непорожній
     * @param weightKg вага в кілограмах, додатне скінченне число
     * @param city місто доставлення, непорожнє
     * @param deliveryDays плановий термін у днях, не від'ємний
     * @param acceleratedDays кількість днів прискорення, від нуля до планового терміну
     * @throws NullPointerException якщо {@code id} або {@code city} дорівнює {@code null}
     * @throws IllegalArgumentException якщо значення порушує інваріант посилки
     */
    public ExpressParcel(String id, double weightKg, String city, int deliveryDays, int acceleratedDays) {
        super(id, weightKg, city, deliveryDays);
        if (acceleratedDays < 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "від'ємне прискорення доставлення: %d", acceleratedDays));
        }
        if (acceleratedDays > deliveryDays) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "прискорення %d дн. перевищує плановий термін %d дн.", acceleratedDays, deliveryDays));
        }
        this.acceleratedDays = acceleratedDays;
    }

    /**
     * Повертає кількість днів прискорення.
     *
     * @return прискорення в днях
     */
    public int getAcceleratedDays() {
        return acceleratedDays;
    }

    /**
     * Повертає вид експрес-доставлення.
     *
     * @return {@link DeliveryKind#EXPRESS}
     */
    @Override
    public DeliveryKind getKind() {
        return DeliveryKind.EXPRESS;
    }

    /**
     * Віднімає дні прискорення від планового терміну.
     *
     * @return розрахунковий термін у днях
     */
    @Override
    public int calculatedDays() {
        return getDeliveryDays() - acceleratedDays;
    }

    /**
     * Обчислює вартість експрес-доставлення: базовий тариф із надбавкою та плату за прискорення.
     *
     * @return вартість у гривнях
     */
    @Override
    public double calculatedCost() {
        return baseCost() * EXPRESS_RATE + acceleratedDays * ACCELERATION_FEE;
    }
}
