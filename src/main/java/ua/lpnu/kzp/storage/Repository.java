package ua.lpnu.kzp.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Типобезпечне сховище об'єктів у пам'яті.
 *
 * <p>Параметр типу {@code T} задається під час створення сховища: {@code Repository<String>} приймає лише
 * рядки, тому компілятор перевіряє типи без приведень. Записи зберігаються в порядку додавання, а назовні
 * сховище віддає лише незмінні списки, тож зовнішній код не може змінити його внутрішній список.
 *
 * @param <T> тип записів сховища
 */
public final class Repository<T> {
    private final List<T> items = new ArrayList<>();

    /** Створює порожнє сховище. */
    public Repository() {
        // Початковий стан задано в оголошенні поля.
    }

    /**
     * Додає запис у кінець сховища.
     *
     * @param item запис, не {@code null}
     * @throws NullPointerException якщо {@code item} дорівнює {@code null}
     */
    public void add(T item) {
        items.add(Objects.requireNonNull(item, "запис не може бути null"));
    }

    /**
     * Повертає незмінний знімок усіх записів.
     *
     * @return записи в порядку додавання; наступні виклики {@link #add(Object)} цей знімок не змінюють
     */
    public List<T> all() {
        return List.copyOf(items);
    }

    /**
     * Повертає записи, що задовольняють умову.
     *
     * <p>Тип {@code Predicate<? super T>} дозволяє передати умову для {@code T} або будь-якого його супертипу,
     * наприклад {@code Predicate<CharSequence>} для {@code Repository<String>}.
     *
     * @param condition умова відбору, не {@code null}
     * @return незмінний список відібраних записів у порядку додавання
     * @throws NullPointerException якщо {@code condition} дорівнює {@code null}
     */
    public List<T> find(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "умова не може бути null");
        return items.stream()
                .filter(condition)
                .toList();
    }

    /**
     * Повертає кількість записів без копіювання списку.
     *
     * @return кількість доданих записів
     */
    public int size() {
        return items.size();
    }
}
