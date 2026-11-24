package ua.lpnu.kzp.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvImporterTest {
    @TempDir
    Path directory;

    /* Клас іншої предметної області з власним правилом розбору числа. */
    record City(@CsvColumn("назва") String name, @CsvColumn("населення") int population) {
        static City fromFields(List<String> fields) throws DataStorageException {
            if (fields.size() != 2) {
                throw new DataStorageException("очікується 2 поля, отримано " + fields.size());
            }
            try {
                return new City(fields.get(0), Integer.parseInt(fields.get(1)));
            } catch (NumberFormatException exception) {
                throw new DataStorageException("населення не є числом: " + fields.get(1), exception);
            }
        }
    }

    private Path file(String text) throws IOException {
        return Files.writeString(directory.resolve("cities.csv"), text, StandardCharsets.UTF_8);
    }

    private static List<City> read(Path path) throws DataStorageException {
        return CsvImporter.read(path, City.class, City::fromFields);
    }

    /* Записане експортером читається назад без змін, зокрема поле з комою. */
    @Test
    void readsBackWhatExporterWrote() throws DataStorageException {
        List<City> cities = List.of(new City("Львів", 717_000), new City("Біла Церква, Київська обл.", 200_000));
        Path path = directory.resolve("cities.csv");

        CsvExporter.write(path, City.class, cities);

        assertEquals(cities, read(path));
    }

    /* Файл лише із заголовком – це порожній набір записів. */
    @Test
    void headerOnlyFileGivesEmptyList() throws Exception {
        assertEquals(List.of(), read(file("назва,населення\n")));
    }

    /* Порожній файл без заголовка – помилка формату, а не порожній набір. */
    @Test
    void emptyFileIsFormatError() throws Exception {
        Path path = file("");

        DataStorageException exception = assertThrows(DataStorageException.class, () -> read(path));

        assertEquals("CSV-файл %s порожній: немає рядка заголовка".formatted(path), exception.getMessage());
    }

    /* Інший порядок стовпців переплутав би значення, тому заголовок має точно збігатися. */
    @Test
    void rejectsUnexpectedHeader() throws Exception {
        Path path = file("населення,назва\n717000,Львів\n");

        DataStorageException exception = assertThrows(DataStorageException.class, () -> read(path));

        assertTrue(exception.getMessage().endsWith("очікується «назва,населення», отримано «населення,назва»"),
                exception.getMessage());
    }

    /* Хибний запис називає номер з урахуванням заголовка, а ланцюг причин доходить до NumberFormatException. */
    @Test
    void namesBrokenRecordAndKeepsCauseChain() throws Exception {
        Path path = file("назва,населення\nЛьвів,717000\nКиїв,багато\n");

        DataStorageException exception = assertThrows(DataStorageException.class, () -> read(path));

        assertEquals("запис 3 CSV-файла %s: населення не є числом: багато".formatted(path), exception.getMessage());
        DataStorageException domain = assertInstanceOf(DataStorageException.class, exception.getCause());
        assertInstanceOf(NumberFormatException.class, domain.getCause());
    }

    /* Неперевірений виняток предметного розбору теж отримує номер запису й лишається причиною. */
    @Test
    void wrapsUncheckedMapperExceptionWithRecordNumber() throws Exception {
        Path path = file("назва,населення\nЛьвів,717000\nКиїв,багато\n");
        CsvImporter.RowMapper<City> careless = fields -> new City(fields.get(0), Integer.parseInt(fields.get(1)));

        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvImporter.read(path, City.class, careless));

        assertTrue(exception.getMessage().startsWith("запис 3 CSV-файла %s: ".formatted(path)), exception.getMessage());
        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }

    /* Перетворення, що повернуло null, дає помилку з номером запису, а не NullPointerException без контексту. */
    @Test
    void rejectsNullFromMapper() throws Exception {
        Path path = file("назва,населення\nЛьвів,717000\n");

        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvImporter.read(path, City.class, fields -> null));

        assertEquals("запис 2 CSV-файла %s не відновлено: перетворення повернуло null".formatted(path),
                exception.getMessage());
    }

    /* Позначка порядку байтів не потрапляє в першу назву стовпця. */
    @Test
    void ignoresByteOrderMark() throws Exception {
        assertEquals(List.of(new City("Львів", 717_000)), read(file("﻿назва,населення\nЛьвів,717000\n")));
    }

    /* Пошкоджені лапки – помилка формату з назвою файла й причиною від парсера. */
    @Test
    void reportsBrokenQuotesWithFileName() throws Exception {
        Path path = file("назва,населення\n\"Львів,717000\n");

        DataStorageException exception = assertThrows(DataStorageException.class, () -> read(path));

        assertEquals("пошкоджений CSV-файл %s: незакриті лапки в CSV-записі 2".formatted(path), exception.getMessage());
        assertInstanceOf(DataStorageException.class, exception.getCause());
    }

    /* Відсутній файл: першопричиною лишається NoSuchFileException. */
    @Test
    void missingFileKeepsNoSuchFileException() {
        Path missing = directory.resolve("немає.csv");

        DataStorageException exception = assertThrows(DataStorageException.class, () -> read(missing));

        assertEquals("не вдалося прочитати CSV-файл %s".formatted(missing), exception.getMessage());
        assertInstanceOf(NoSuchFileException.class, exception.getCause());
    }

    /* Файл не в UTF-8 не читається зі спотвореними літерами: причина – CharacterCodingException. */
    @Test
    void nonUtf8FileKeepsCharacterCodingException() throws Exception {
        Path path = directory.resolve("cp1251.csv");
        Files.write(path, "назва,населення\nЛьвів,717000\n".getBytes(Charset.forName("windows-1251")));

        DataStorageException exception = assertThrows(DataStorageException.class, () -> read(path));

        assertInstanceOf(CharacterCodingException.class, exception.getCause());
    }
}
