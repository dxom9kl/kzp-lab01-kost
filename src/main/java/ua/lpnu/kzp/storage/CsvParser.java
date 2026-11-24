package ua.lpnu.kzp.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Розбирає CSV-документ на записи й поля.
 *
 * <p>Парсер є машиною станів над усім текстом документа, а не розбиттям за {@code split(",")}: кома й
 * перенесення рядка поза лапками розділяють поля та записи, а всередині лапок належать значенню поля. Тому
 * результат – список записів, а не фізичних рядків файла: запис із багаторядковим полем займає кілька рядків.
 *
 * <p>Правила розбору:
 * <ul>
 *   <li>записи розділяються LF, CRLF або CR; завершальний роздільник не утворює порожнього запису;</li>
 *   <li>лапка на початку поля відкриває поле в лапках, подвоєна лапка всередині нього означає одну лапку;</li>
 *   <li>після закривальної лапки може йти лише кома, роздільник рядка або кінець документа;</li>
 *   <li>лапка всередині поля без лапок є звичайним символом;</li>
 *   <li>порожній рядок між записами є записом з одним порожнім полем.</li>
 * </ul>
 */
public final class CsvParser {
    private static final char QUOTE = '"';
    private static final char COMMA = ',';
    private static final char CR = '\r';
    private static final char LF = '\n';

    /* Забороняє створення екземплярів службового класу. */
    private CsvParser() {
    }

    /**
     * Розбирає текст, що містить рівно один CSV-запис.
     *
     * @param line текст одного запису; завершальний роздільник рядка допускається
     * @return незмінний список полів запису
     * @throws DataStorageException якщо текст порожній, містить кілька записів або порушує правила CSV
     * @throws NullPointerException якщо {@code line} дорівнює {@code null}
     */
    public static List<String> parse(String line) throws DataStorageException {
        List<List<String>> records = parseDocument(line);
        if (records.size() != 1) {
            throw new DataStorageException(String.format(Locale.ROOT,
                    "очікується один CSV-запис, отримано %d", records.size()));
        }
        return records.get(0);
    }

    /**
     * Розбирає CSV-документ.
     *
     * @param text увесь текст документа
     * @return незмінний список записів, кожен із яких – незмінний список полів; порожній текст дає порожній список
     * @throws DataStorageException якщо лапки не закрито або після закривальної лапки є інший символ
     * @throws NullPointerException якщо {@code text} дорівнює {@code null}
     */
    public static List<List<String>> parseDocument(String text) throws DataStorageException {
        Objects.requireNonNull(text, "CSV-документ не може бути null");
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        // quoted – поле відкрите лапками; closed – лапки поля вже закрито, тому далі можлива лише кома
        // або кінець запису.
        boolean quoted = false;
        boolean closed = false;
        for (int index = 0; index < text.length(); index++) {
            char symbol = text.charAt(index);
            if (quoted) {
                // Усередині лапок кома й перенесення рядка є даними, а не роздільниками.
                if (symbol != QUOTE) {
                    field.append(symbol);
                } else if (index + 1 < text.length() && text.charAt(index + 1) == QUOTE) {
                    field.append(QUOTE);
                    index++;
                } else {
                    quoted = false;
                    closed = true;
                }
            } else if (symbol == COMMA) {
                fields.add(field.toString());
                field.setLength(0);
                closed = false;
            } else if (symbol == CR || symbol == LF) {
                fields.add(field.toString());
                records.add(List.copyOf(fields));
                fields.clear();
                field.setLength(0);
                closed = false;
                // CRLF – один роздільник, а не два.
                if (symbol == CR && index + 1 < text.length() && text.charAt(index + 1) == LF) {
                    index++;
                }
            } else if (closed) {
                throw new DataStorageException(String.format(Locale.ROOT,
                        "символ «%c» після закривальної лапки в CSV-записі %d", symbol, records.size() + 1));
            } else if (symbol == QUOTE && field.length() == 0) {
                quoted = true;
            } else {
                field.append(symbol);
            }
        }
        if (quoted) {
            throw new DataStorageException(String.format(Locale.ROOT,
                    "незакриті лапки в CSV-записі %d", records.size() + 1));
        }
        // Документ без завершального роздільника рядка закінчує останній запис; closed враховує поле "".
        if (!fields.isEmpty() || field.length() > 0 || closed) {
            fields.add(field.toString());
            records.add(List.copyOf(fields));
        }
        return List.copyOf(records);
    }
}
