package ua.lpnu.kzp.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;

class CsvColumnTest {
    /* Record із позначеним і непозначеним компонентом. */
    record Sample(@CsvColumn("назва") String name, String note) {
    }

    /* RUNTIME залишає анотацію рефлексії, а FIELD дозволяє її лише над полями. */
    @Test
    void isRetainedAtRuntimeAndTargetsFieldsOnly() {
        assertEquals(RetentionPolicy.RUNTIME, CsvColumn.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] {ElementType.FIELD}, CsvColumn.class.getAnnotation(Target.class).value());
    }

    /* Анотація має один елемент value() типу String. */
    @Test
    void hasSingleStringValue() throws NoSuchMethodException {
        assertEquals(1, CsvColumn.class.getDeclaredMethods().length);
        assertEquals(String.class, CsvColumn.class.getDeclaredMethod("value").getReturnType());
    }

    /* Анотацію компонента record компілятор переносить на поле, і її видно під час виконання. */
    @Test
    void isReadableFromFieldAtRuntime() throws NoSuchFieldException {
        assertEquals("назва", Sample.class.getDeclaredField("name").getAnnotation(CsvColumn.class).value());
        assertNull(Sample.class.getDeclaredField("note").getAnnotation(CsvColumn.class));
    }
}
