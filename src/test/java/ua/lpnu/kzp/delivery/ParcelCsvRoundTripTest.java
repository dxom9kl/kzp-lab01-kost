package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.lpnu.kzp.storage.CsvExporter;
import ua.lpnu.kzp.storage.CsvImporter;
import ua.lpnu.kzp.storage.CsvParser;
import ua.lpnu.kzp.storage.DataStorageException;
import ua.lpnu.kzp.storage.Repository;

class ParcelCsvRoundTripTest {
    /* Контрольні CSV-файли варіанта: повний набір, файл із хибним рядком і файл без записів. */
    private static final Path DATA = Path.of("src", "test", "resources", "lab05");
    private static final String HEADER = "місто,габарити,статус,номер,вага";

    @TempDir
    Path directory;

    /* Шість записів: кома, кома з лапками, перенесення рядка й порожні габарити в текстових полях. */
    private static List<ParcelRecord> sample() {
        return List.of(
                new ParcelRecord("UA-5001", 2.5, "30x20x15", "Львів", DeliveryStatus.DELIVERED),
                new ParcelRecord("UA-5002", 12.4, "60x40x40", "Київ", DeliveryStatus.IN_TRANSIT),
                new ParcelRecord("UA-5003", 0.75, "25x25x10", "Біла Церква, Київська обл.", DeliveryStatus.DELIVERED),
                new ParcelRecord("UA-5004", 25.0, "120x60x50", "Одеса, відділення \"Центральне\"",
                        DeliveryStatus.IN_TRANSIT),
                new ParcelRecord("UA-5005", 3.1, "", "Ужгород\nвул. Корзо, 3", DeliveryStatus.IN_TRANSIT),
                new ParcelRecord("UA-5006", 1.25, "10x10x5", "Кам'янець-Подільський", DeliveryStatus.DELIVERED));
    }

    private static List<ParcelRecord> load(Path path) throws DataStorageException {
        return CsvImporter.read(path, ParcelRecord.class, ParcelRecord::fromFields);
    }

    /* Сховище → CSV → об'єкти: відновлений список рівний початковому за всіма полями й порядком. */
    @Test
    void roundTripRestoresEqualRecords() throws DataStorageException {
        Repository<ParcelRecord> repository = new Repository<>();
        sample().forEach(repository::add);
        Path path = directory.resolve("parcels.csv");

        CsvExporter.write(path, ParcelRecord.class, repository.all());
        List<ParcelRecord> restored = load(path);

        assertEquals(repository.all(), restored);
        assertEquals(sample(), repository.all());
    }

    /* 8 фізичних рядків: заголовок і 6 записів, один із яких займає два рядки; 7 CSV-записів, 6 об'єктів. */
    @Test
    void countsPhysicalLinesRecordsAndObjects() throws Exception {
        Path path = directory.resolve("parcels.csv");
        CsvExporter.write(path, ParcelRecord.class, sample());
        String csv = Files.readString(path, StandardCharsets.UTF_8);

        assertEquals(8, csv.lines().count());
        assertEquals(7, CsvParser.parseDocument(csv).size());
        assertEquals(6, load(path).size());
    }

    /* Кожен запис відновлюється за ключем trackingNumber рівно один раз і без змін. */
    @Test
    void restoresEachRecordByTrackingNumber() throws DataStorageException {
        Path path = directory.resolve("parcels.csv");
        CsvExporter.write(path, ParcelRecord.class, sample());
        Repository<ParcelRecord> restored = new Repository<>();
        load(path).forEach(restored::add);

        for (ParcelRecord original : sample()) {
            assertEquals(List.of(original),
                    restored.find(copy -> copy.trackingNumber().equals(original.trackingNumber())),
                    original.trackingNumber());
        }
    }

    /* Контрольний файл – саме те, що пише експортер, з точністю до роздільника рядків ОС. */
    @Test
    void sampleFileMatchesExporterOutput() throws Exception {
        String expected = Files.readString(DATA.resolve("parcels.csv"), StandardCharsets.UTF_8);

        String actual = CsvExporter.toCsv(ParcelRecord.class, sample()).replace(System.lineSeparator(), "\n");

        assertEquals(expected, actual);
        assertTrue(actual.contains("\"Одеса, відділення \"\"Центральне\"\"\",120x60x50,IN_TRANSIT,UA-5004,25.0\n"));
        assertEquals(sample(), load(DATA.resolve("parcels.csv")));
    }

    /* Порожній набір: файл лише із заголовком читається як порожній список. */
    @Test
    void emptySetWritesHeaderOnly() throws Exception {
        Path path = directory.resolve("empty.csv");

        CsvExporter.write(path, ParcelRecord.class, new Repository<ParcelRecord>().all());

        assertEquals(HEADER + System.lineSeparator(), Files.readString(path, StandardCharsets.UTF_8));
        assertEquals(List.of(), load(path));
        assertEquals(List.of(), load(DATA.resolve("empty.csv")));
    }

    /* Хибний рядок набору називає номер запису, а ланцюг причин доходить до NumberFormatException. */
    @Test
    void brokenFileNamesRecordAndCause() {
        Path path = DATA.resolve("broken.csv");

        DataStorageException exception = assertThrows(DataStorageException.class, () -> load(path));

        assertEquals("запис 3 CSV-файла %s: вага не є числом: \"важка\"".formatted(path), exception.getMessage());
        DataStorageException cause = assertInstanceOf(DataStorageException.class, exception.getCause());
        assertInstanceOf(NumberFormatException.class, cause.getCause());
    }

    /* Посилки різних підтипів: статус за розрахунковим терміном, габарити порожні, round-trip без змін. */
    @Test
    void polymorphicParcelsSurviveRoundTrip() throws DataStorageException {
        Repository<Parcel> parcels = new Repository<>();
        parcels.add(new ExpressParcel("UA-1001", 2.5, "Львів", 1));
        parcels.add(new RegularParcel("UA-1003", 12.4, "Одеса", 3));
        parcels.add(new RegularParcel("UA-1009", 1.0, "Суми", 1, 2));
        Repository<ParcelRecord> records = new Repository<>();
        parcels.all().forEach(parcel -> records.add(ParcelRecord.of(parcel)));
        Path path = directory.resolve("parcels.csv");

        CsvExporter.write(path, ParcelRecord.class, records.all());

        assertEquals(records.all(), load(path));
        assertEquals(List.of(DeliveryStatus.DELIVERED, DeliveryStatus.IN_TRANSIT, DeliveryStatus.IN_TRANSIT),
                load(path).stream().map(ParcelRecord::status).toList());
    }
}
