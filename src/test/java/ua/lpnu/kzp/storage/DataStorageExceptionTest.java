package ua.lpnu.kzp.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class DataStorageExceptionTest {
    /* Конструктор із причиною зберігає і пояснення, і початкову помилку. */
    @Test
    void keepsMessageAndCause() {
        IOException cause = new IOException("диск недоступний");

        DataStorageException exception = new DataStorageException("не вдалося записати CSV-файл a.csv", cause);

        assertEquals("не вдалося записати CSV-файл a.csv", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /* Помилка, яку виявила сама програма, не має першопричини. */
    @Test
    void messageOnlyConstructorHasNoCause() {
        DataStorageException exception = new DataStorageException("CSV-файл a.csv порожній");

        assertEquals("CSV-файл a.csv порожній", exception.getMessage());
        assertNull(exception.getCause());
    }

    /* Виняток checked: компілятор вимагає catch або throws. */
    @Test
    void isCheckedException() {
        assertTrue(Exception.class.isAssignableFrom(DataStorageException.class));
        assertFalse(RuntimeException.class.isAssignableFrom(DataStorageException.class));
    }
}
