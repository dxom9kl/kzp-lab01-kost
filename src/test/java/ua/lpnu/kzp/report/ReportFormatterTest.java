package ua.lpnu.kzp.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import ua.lpnu.kzp.delivery.DeliverySummary;

class ReportFormatterTest {
    private static final Path INPUT = Path.of("data", "input.csv");

    private static DeliverySummary sampleSummary() {
        DeliverySummary summary = new DeliverySummary();
        summary.add(new String[] {"UA-1", "2.50", "Львів", "1", "true"});
        summary.add(new String[] {"UA-2", "1.50", "Ужгород", "5", "false"});
        return summary;
    }

    private static boolean hasLine(String report, String regex) {
        return report.lines().anyMatch(line -> line.matches(regex));
    }

    /* Звіт містить усі чотири показники варіанта. */
    @Test
    void containsAllMetrics() {
        String report = ReportFormatter.format(INPUT, sampleSummary(), List.of());

        assertTrue(hasLine(report, "Коректних записів\\s+2"), report);
        assertTrue(hasLine(report, "Середня вага\\s+2\\.00 кг"), report);
        assertTrue(hasLine(report, "Найбільший термін доставлення\\s+5 дн\\. \\(UA-2, Ужгород\\)"), report);
        assertTrue(hasLine(report, "Експрес-доставлень\\s+1"), report);
        assertTrue(hasLine(report, "Пропущено рядків: 0"), report);
    }

    /* Пропущені рядки перелічуються з відступом у тому порядку, у якому їх передано. */
    @Test
    void listsSkippedLines() {
        List<String> errors = List.of("Рядок 3: порожній рядок", "Рядок 5: порожня вага");

        String report = ReportFormatter.format(INPUT, sampleSummary(), errors);

        assertTrue(report.contains("Пропущено рядків: 2" + System.lineSeparator()
                + "  Рядок 3: порожній рядок" + System.lineSeparator()
                + "  Рядок 5: порожня вага" + System.lineSeparator()), report);
    }

    /* Крапка лишається десятковим роздільником навіть з українською локаллю за замовчуванням. */
    @Test
    void usesDotRegardlessOfDefaultLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("uk-UA"));

            String report = ReportFormatter.format(INPUT, sampleSummary(), List.of());

            assertTrue(report.contains("2.00 кг"), report);
            assertFalse(report.contains("2,00"), report);
        } finally {
            Locale.setDefault(saved);
        }
    }

    /* Рядки завершуються роздільником поточної ОС, як і задає %n. */
    @Test
    void usesPlatformLineSeparator() {
        String report = ReportFormatter.format(INPUT, sampleSummary(), List.of());

        assertTrue(report.endsWith(System.lineSeparator()));
        assertEquals(report.lines().count(), report.split(System.lineSeparator(), -1).length - 1);
    }

    /* Без коректних записів показники не обчислюються, а звіт пояснює чому. */
    @Test
    void explainsEmptySummary() {
        String report = ReportFormatter.format(INPUT, new DeliverySummary(), List.of("Рядок 1: порожній рядок"));

        assertTrue(hasLine(report, "Коректних записів\\s+0"), report);
        assertTrue(report.contains("Коректних записів немає, тому показники не обчислено."), report);
        assertFalse(report.contains("Середня вага"), report);
    }
}
