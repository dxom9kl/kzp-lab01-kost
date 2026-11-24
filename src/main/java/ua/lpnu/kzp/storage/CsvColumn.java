package ua.lpnu.kzp.storage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Позначає поле, яке експортується як CSV-стовпець, і задає назву цього стовпця.
 *
 * <p>{@link RetentionPolicy#RUNTIME} зберігає анотацію в class-файлі й робить її доступною рефлексії під час
 * виконання, а {@link ElementType#FIELD} дозволяє ставити її лише над полями, зокрема над компонентами
 * record, які компілятор перетворює на поля. Анотація лише описує контракт: експорт виконує
 * {@link CsvExporter}. Порядок стовпців задає не анотація, а сортування Java-імен полів.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn {
    /**
     * Повертає назву CSV-стовпця для рядка заголовка.
     *
     * @return непорожня назва стовпця, унікальна в межах класу
     */
    String value();
}
