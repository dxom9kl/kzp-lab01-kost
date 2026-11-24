package ua.lpnu.kzp.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CsvParserTest {
    /* Кома й перенесення рядка поза лапками розділяють поля та записи. */
    @Test
    void splitsFieldsAndRecords() throws DataStorageException {
        assertEquals(List.of(List.of("Львів", "2.5"), List.of("Київ", "0.75")),
                CsvParser.parseDocument("Львів,2.5\nКиїв,0.75\n"));
    }

    /* Кома в лапках належить значенню: split(",") дав би три поля замість двох. */
    @Test
    void keepsCommaInsideQuotes() throws DataStorageException {
        String line = "\"Біла Церква, Київська обл.\",0.75";

        assertEquals(List.of("Біла Церква, Київська обл.", "0.75"), CsvParser.parse(line));
        assertEquals(3, line.split(",").length);
    }

    /* Подвоєна лапка всередині поля означає одну лапку. */
    @Test
    void unescapesDoubledQuotes() throws DataStorageException {
        assertEquals(List.of("Одеса, відділення \"Центральне\"", "25.0"),
                CsvParser.parse("\"Одеса, відділення \"\"Центральне\"\"\",25.0"));
    }

    /* Перенесення рядка в лапках не завершує запис: три фізичні рядки дають два записи. */
    @Test
    void keepsLineBreakInsideQuotes() throws DataStorageException {
        assertEquals(List.of(List.of("Ужгород\nвул. Корзо, 3", "3.1"), List.of("Львів", "2.5")),
                CsvParser.parseDocument("\"Ужгород\nвул. Корзо, 3\",3.1\nЛьвів,2.5\n"));
    }

    /* CR і CRLF усередині лапок зберігаються як частина значення. */
    @Test
    void keepsCarriageReturnInsideQuotes() throws DataStorageException {
        assertEquals(List.of("рядок 1\r\nрядок 2\rрядок 3", "x"),
                CsvParser.parse("\"рядок 1\r\nрядок 2\rрядок 3\",x"));
    }

    /* LF, CRLF і CR однаково розділяють записи, а CRLF не дає порожнього запису. */
    @ParameterizedTest(name = "[{index}]")
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void acceptsLineSeparatorsOfAllSystems(String separator) throws DataStorageException {
        String text = "a,b" + separator + "c,d" + separator;

        assertEquals(List.of(List.of("a", "b"), List.of("c", "d")), CsvParser.parseDocument(text));
    }

    /* Порожній документ не має записів, а останній запис може не мати роздільника рядка. */
    @Test
    void handlesEmptyDocumentAndLastRecordWithoutSeparator() throws DataStorageException {
        assertEquals(List.of(), CsvParser.parseDocument(""));
        assertEquals(List.of(List.of("a"), List.of("b")), CsvParser.parseDocument("a\nb"));
    }

    /* Порожні поля зберігаються, зокрема порожнє поле в лапках наприкінці документа. */
    @Test
    void keepsEmptyFields() throws DataStorageException {
        assertEquals(List.of(List.of("", "", ""), List.of("x", "")), CsvParser.parseDocument(",,\nx,\"\""));
        assertEquals(List.of(List.of("")), CsvParser.parseDocument("\"\""));
    }

    /* Порожній рядок між записами є записом з одним порожнім полем, а не пропуском. */
    @Test
    void blankLineIsRecordWithOneEmptyField() throws DataStorageException {
        assertEquals(List.of(List.of("a"), List.of(""), List.of("b")), CsvParser.parseDocument("a\n\nb\n"));
    }

    /* Лапка всередині поля без лапок є звичайним символом. */
    @Test
    void keepsQuoteInsideUnquotedField() throws DataStorageException {
        assertEquals(List.of("30\"", "x"), CsvParser.parse("30\",x"));
    }

    /* Незакриті лапки є помилкою формату з номером запису. */
    @Test
    void rejectsUnclosedQuote() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvParser.parseDocument("місто,вага\n\"Львів,2.5\n"));

        assertEquals("незакриті лапки в CSV-записі 2", exception.getMessage());
    }

    /* Після закривальної лапки допустимі лише кома, роздільник рядка або кінець документа. */
    @Test
    void rejectsCharacterAfterClosingQuote() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvParser.parse("\"Львів\"x,2.5"));

        assertEquals("символ «x» після закривальної лапки в CSV-записі 1", exception.getMessage());
    }

    /* parse приймає рівно один запис. */
    @Test
    void parseRequiresExactlyOneRecord() {
        DataStorageException two = assertThrows(DataStorageException.class, () -> CsvParser.parse("a\nb"));
        DataStorageException none = assertThrows(DataStorageException.class, () -> CsvParser.parse(""));

        assertEquals("очікується один CSV-запис, отримано 2", two.getMessage());
        assertEquals("очікується один CSV-запис, отримано 0", none.getMessage());
    }

    /* Записи й поля повертаються незмінними списками. */
    @Test
    void returnsImmutableLists() throws DataStorageException {
        List<List<String>> records = CsvParser.parseDocument("a,b\n");

        assertThrows(UnsupportedOperationException.class, () -> records.add(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> records.get(0).add("c"));
    }
}
