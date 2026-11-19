package ua.lpnu.kzp.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ParcelFromCsvTest {
    /* Коректний рядок перетворюється на посилку з полями в порядку формату. */
    @Test
    void parsesValidLine() {
        Parcel parcel = Parcel.fromCsv("UA-1001;2.50;Львів;1;true");

        assertEquals("UA-1001", parcel.getId());
        assertEquals(2.50, parcel.getWeightKg());
        assertEquals("Львів", parcel.getCity());
        assertEquals(1, parcel.getDeliveryDays());
        assertTrue(parcel.isExpress());
    }

    /* Українські літери, апостроф, дефіс і пробіл у назві міста зберігаються без змін. */
    @Test
    void keepsUkrainianCityName() {
        Parcel parcel = Parcel.fromCsv("UA-1007;3.10;Кам'янець-Подільський;6;true");

        assertEquals("Кам'янець-Подільський", parcel.getCity());
        assertEquals("Біла Церква", Parcel.fromCsv("UA-1008;1.60;Біла Церква;0;true").getCity());
    }

    /* Нуль днів означає доставлення в день відправлення і є допустимим значенням. */
    @Test
    void acceptsSameDayDelivery() {
        assertEquals(0, Parcel.fromCsv("UA-1;1.00;Київ;0;false").getDeliveryDays());
    }

    /* Вага записується цілим числом або десятковим із крапкою. */
    @ParameterizedTest
    @ValueSource(strings = {"2", "2.5", "0.75", "12.40"})
    void acceptsDecimalWeight(String weightKg) {
        assertEquals(Double.parseDouble(weightKg), Parcel.fromCsv("UA-1;" + weightKg + ";Київ;2;false").getWeightKg());
    }

    /* Ознака експрес-доставлення приймає рівно два значення. */
    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void acceptsBooleanExpress(String express) {
        assertEquals(Boolean.parseBoolean(express), Parcel.fromCsv("UA-1;1.00;Київ;2;" + express).isExpress());
    }

    /* Кожен хибний рядок відкидається з точною причиною. */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("invalidLines")
    void rejectsInvalidLine(String line, String reason) {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> Parcel.fromCsv(line));

        assertEquals(reason, exception.getMessage());
    }

    static Stream<Arguments> invalidLines() {
        return Stream.of(
                arguments("", "порожній рядок"),
                arguments("   ", "порожній рядок"),
                arguments("UA-1;1.00;Луцьк;2", "очікується 5 полів, отримано 4"),
                arguments("UA-1;1.00;Луцьк;2;true;зайве", "очікується 5 полів, отримано 6"),
                arguments("UA-1,1.00,Луцьк,2,true", "очікується 5 полів, отримано 1"),
                arguments(";1.00;Київ;2;false", "порожній ідентифікатор посилки"),
                arguments("  ;1.00;Київ;2;false", "порожній ідентифікатор посилки"),
                arguments("UA-1;;Київ;2;false", "порожня вага"),
                arguments("UA-1;1.00;;2;false", "порожнє місто"),
                arguments("UA-1;1.00;Київ;;false", "порожній термін доставлення"),
                arguments("UA-1;1.00;Київ;2;", "порожня ознака експрес-доставлення"),
                arguments("UA-1;важка;Одеса;3;false", "вага не є числом: \"важка\""),
                arguments("UA-1;2,50;Одеса;3;false", "вага не є числом: \"2,50\""),
                arguments("UA-1;0;Рівне;1;false", "вага має бути додатною: 0"),
                arguments("UA-1;-3.00;Рівне;1;false", "вага має бути додатною: -3.00"),
                arguments("UA-1;NaN;Суми;2;false", "вага не є числом: \"NaN\""),
                arguments("UA-1;Infinity;Суми;2;false", "вага не є числом: \"Infinity\""),
                arguments("UA-1;1e3;Суми;2;false", "вага не є числом: \"1e3\""),
                arguments("UA-1;10d;Суми;2;false", "вага не є числом: \"10d\""),
                arguments("UA-1;0x1p3;Суми;2;false", "вага не є числом: \"0x1p3\""),
                arguments("UA-1;+2.00;Суми;2;false", "вага не є числом: \"+2.00\""),
                arguments("UA-1; 2.00;Суми;2;false", "вага не є числом: \" 2.00\""),
                arguments("UA-1;2.00 ;Суми;2;false", "вага не є числом: \"2.00 \""),
                arguments("UA-1;" + "9".repeat(400) + ";Суми;2;false", "вага завелика"),
                arguments("UA-1;1.00;Дніпро;2.5;false", "термін доставлення не є цілим числом: \"2.5\""),
                arguments("UA-1;1.00;Дніпро;два;false", "термін доставлення не є цілим числом: \"два\""),
                arguments("UA-1;1.00;Дніпро;-1;false", "від'ємний термін доставлення: -1"),
                arguments("UA-1;1.00;Дніпро; 2;false", "термін доставлення не є цілим числом: \" 2\""),
                arguments("UA-1;1.00;Дніпро;+2;false", "термін доставлення не є цілим числом: \"+2\""),
                arguments("UA-1;1.00;Дніпро;99999999999;false",
                        "термін доставлення завеликий: \"99999999999\""),
                arguments("UA-1;1.00;Полтава;2;так",
                        "ознака експрес-доставлення має бути true або false: \"так\""),
                arguments("UA-1;1.00;Полтава;2;TRUE",
                        "ознака експрес-доставлення має бути true або false: \"TRUE\""),
                arguments("UA-1;1.00;Полтава;2;1",
                        "ознака експрес-доставлення має бути true або false: \"1\""));
    }
}
