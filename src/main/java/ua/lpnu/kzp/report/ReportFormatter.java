package ua.lpnu.kzp.report;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import ua.lpnu.kzp.delivery.DeliverySummary;

/**
 * Формує текст звіту, однаковий для консолі та файла.
 *
 * <p>Числа форматуються з {@link Locale#ROOT}, тому десятковим роздільником
 * завжди є крапка, а рядки завершуються роздільником поточної ОС ({@code %n}).
 */
public final class ReportFormatter {
    private static final String ROW = "%-30s %s%n";

    /* Забороняє створення екземплярів службового класу. */
    private ReportFormatter() {
    }

    /**
     * Формує звіт із показниками та переліком пропущених рядків.
     *
     * @param input шлях до вхідного файла для заголовка звіту
     * @param summary обчислені показники
     * @param errors повідомлення про пропущені рядки у форматі «Рядок N: причина»
     * @return текст звіту
     */
    public static String format(Path input, DeliverySummary summary, List<String> errors) {
        StringBuilder report = new StringBuilder();
        report.append(String.format(Locale.ROOT, "Звіт: доставлення посилок%n"));
        report.append(String.format(Locale.ROOT, "Вхідний файл: %s%n%n", input));
        report.append(row("Показник", "Значення"));
        report.append(row("Коректних записів", Integer.toString(summary.count())));
        if (summary.isEmpty()) {
            report.append(String.format(Locale.ROOT, "%nКоректних записів немає, тому показники не обчислено.%n"));
        } else {
            report.append(row("Середня вага",
                    String.format(Locale.ROOT, "%.2f кг", summary.averageWeightKg())));
            report.append(row("Найбільший термін доставлення",
                    String.format(Locale.ROOT, "%d дн. (%s, %s)", summary.maxDeliveryDays(),
                            summary.longestDeliveryId(), summary.longestDeliveryCity())));
            report.append(row("Експрес-доставлень", Integer.toString(summary.expressCount())));
        }
        report.append(String.format(Locale.ROOT, "%nПропущено рядків: %d%n", errors.size()));
        for (String error : errors) {
            report.append(String.format(Locale.ROOT, "  %s%n", error));
        }
        return report.toString();
    }

    private static String row(String name, String value) {
        return String.format(Locale.ROOT, ROW, name, value);
    }
}
