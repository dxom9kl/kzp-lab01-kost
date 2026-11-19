package ua.lpnu.kzp.delivery;

import java.util.Locale;

/**
 * Звичайна посилка: розрахунковий термін містить резерв днів, а вартість дорівнює базовому тарифу.
 */
public final class RegularParcel extends Parcel {
    private final int reserveDays;

    /**
     * Створює звичайну посилку без резерву днів, бо вхідний файл його не містить.
     *
     * @param id ідентифікатор посилки, непорожній
     * @param weightKg вага в кілограмах, додатне скінченне число
     * @param city місто доставлення, непорожнє
     * @param deliveryDays плановий термін у днях, не від'ємний
     * @throws NullPointerException якщо {@code id} або {@code city} дорівнює {@code null}
     * @throws IllegalArgumentException якщо значення порушує інваріант посилки
     */
    public RegularParcel(String id, double weightKg, String city, int deliveryDays) {
        this(id, weightKg, city, deliveryDays, 0);
    }

    /**
     * Створює звичайну посилку із заданим резервом днів.
     *
     * @param id ідентифікатор посилки, непорожній
     * @param weightKg вага в кілограмах, додатне скінченне число
     * @param city місто доставлення, непорожнє
     * @param deliveryDays плановий термін у днях, не від'ємний
     * @param reserveDays резерв днів, не від'ємний; сума з плановим терміном вміщується в {@code int}
     * @throws NullPointerException якщо {@code id} або {@code city} дорівнює {@code null}
     * @throws IllegalArgumentException якщо значення порушує інваріант посилки
     */
    public RegularParcel(String id, double weightKg, String city, int deliveryDays, int reserveDays) {
        super(id, weightKg, city, deliveryDays);
        if (reserveDays < 0) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "від'ємний резерв днів: %d", reserveDays));
        }
        // Базовий клас уже перевірив, що плановий термін не від'ємний, тому різниця не переповнює int.
        if (reserveDays > Integer.MAX_VALUE - deliveryDays) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "резерв днів завеликий: %d", reserveDays));
        }
        this.reserveDays = reserveDays;
    }

    /**
     * Повертає резерв днів.
     *
     * @return резерв у днях
     */
    public int getReserveDays() {
        return reserveDays;
    }

    /**
     * Повертає вид звичайного доставлення.
     *
     * @return {@link DeliveryKind#REGULAR}
     */
    @Override
    public DeliveryKind getKind() {
        return DeliveryKind.REGULAR;
    }

    /**
     * Додає резерв днів до планового терміну.
     *
     * @return розрахунковий термін у днях
     */
    @Override
    public int calculatedDays() {
        return getDeliveryDays() + reserveDays;
    }

    /**
     * Обчислює вартість звичайного доставлення за базовим тарифом.
     *
     * @return вартість у гривнях
     */
    @Override
    public double calculatedCost() {
        return baseCost();
    }
}
