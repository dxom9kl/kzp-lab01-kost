package ua.lpnu.kzp.delivery;

/** Вид доставлення посилки: фіксована множина категорій варіанта. */
public enum DeliveryKind {
    /** Звичайне доставлення. */
    REGULAR("звичайне"),
    /** Експрес-доставлення. */
    EXPRESS("експрес");

    private final String label;

    /* Конструктор enum викликається один раз для кожної константи. */
    DeliveryKind(String label) {
        this.label = label;
    }

    /**
     * Повертає назву виду доставлення для текстового подання посилки.
     *
     * @return назва виду доставлення
     */
    public String label() {
        return label;
    }
}
