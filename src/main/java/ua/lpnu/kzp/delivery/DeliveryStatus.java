package ua.lpnu.kzp.delivery;

/** Стан посилки на контрольний день після відправлення. */
public enum DeliveryStatus {
    /** Посилку доставлено не пізніше контрольного дня. */
    DELIVERED("доставлено"),
    /** Посилка ще в дорозі після контрольного дня. */
    IN_TRANSIT("у дорозі");

    /** Контрольний день після відправлення: вхідний файл не містить статусу, тому його визначає термін. */
    public static final int CONTROL_DAY = 2;

    private final String label;

    /* Конструктор enum викликається один раз для кожної константи. */
    DeliveryStatus(String label) {
        this.label = label;
    }

    /**
     * Визначає стан посилки за терміном доставлення.
     *
     * @param days розрахунковий термін доставлення в днях
     * @return {@link #IN_TRANSIT}, якщо термін більший за контрольний день, інакше {@link #DELIVERED}
     */
    public static DeliveryStatus ofDays(int days) {
        return days > CONTROL_DAY ? IN_TRANSIT : DELIVERED;
    }

    /**
     * Повертає назву стану для текстового подання.
     *
     * @return назва стану
     */
    public String label() {
        return label;
    }
}
