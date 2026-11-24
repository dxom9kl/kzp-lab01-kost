package ua.lpnu.kzp.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import ua.lpnu.kzp.delivery.ExpressParcel;
import ua.lpnu.kzp.delivery.Parcel;
import ua.lpnu.kzp.delivery.RegularParcel;

class RepositoryTest {
    private static final Parcel LVIV = new RegularParcel("UA-1", 2.5, "Львів", 1);
    private static final Parcel KYIV = new ExpressParcel("UA-2", 0.75, "Київ", 2);
    private static final Parcel ODESA = new RegularParcel("UA-3", 12.4, "Одеса", 3);

    /* Знімок містить записи в порядку додавання. */
    @Test
    void returnsRecordsInInsertionOrder() {
        Repository<Parcel> repository = new Repository<>();
        repository.add(ODESA);
        repository.add(LVIV);
        repository.add(KYIV);

        assertEquals(List.of(ODESA, LVIV, KYIV), repository.all());
        assertEquals(3, repository.size());
    }

    /* Знімок не можна змінити ззовні, і він не бачить наступних додавань. */
    @Test
    void snapshotIsImmutableAndDetached() {
        Repository<Parcel> repository = new Repository<>();
        repository.add(LVIV);
        List<Parcel> snapshot = repository.all();

        repository.add(KYIV);

        assertEquals(List.of(LVIV), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(ODESA));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove(0));
        assertEquals(List.of(LVIV, KYIV), repository.all());
    }

    /* Пошук повертає незмінний список записів, що задовольняють умову, у порядку додавання. */
    @Test
    void findsRecordsByPredicate() {
        Repository<Parcel> repository = new Repository<>();
        List.of(LVIV, KYIV, ODESA).forEach(repository::add);

        List<Parcel> regular = repository.find(parcel -> !parcel.isExpress());

        assertEquals(List.of(LVIV, ODESA), regular);
        assertEquals(List.of(), repository.find(parcel -> parcel.getCity().equals("Суми")));
        assertThrows(UnsupportedOperationException.class, () -> regular.add(KYIV));
    }

    /* Predicate<? super T>: умову для супертипу можна передати сховищу підтипу. */
    @Test
    void acceptsPredicateOfSupertype() {
        Repository<ExpressParcel> repository = new Repository<>();
        repository.add(new ExpressParcel("UA-7", 1.0, "Львів", 3, 1));
        repository.add(new ExpressParcel("UA-8", 9.0, "Київ", 1));
        Predicate<Parcel> heavy = parcel -> parcel.getWeightKg() > 5;
        Predicate<Object> any = item -> true;

        assertEquals(List.of("UA-8"), repository.find(heavy).stream().map(Parcel::getId).toList());
        assertEquals(2, repository.find(any).size());
    }

    /* Сховище спільного типу зберігає посилки різних підтипів без приведень. */
    @Test
    void storesParcelsOfDifferentSubtypes() {
        Repository<Parcel> repository = new Repository<>();
        repository.add(LVIV);
        repository.add(KYIV);

        assertEquals(List.of(RegularParcel.class, ExpressParcel.class),
                repository.all().stream().map(Object::getClass).toList());
    }

    /* Порожнє сховище повертає порожні знімок і результат пошуку. */
    @Test
    void emptyRepositoryReturnsEmptyLists() {
        Repository<String> repository = new Repository<>();

        assertTrue(repository.all().isEmpty());
        assertTrue(repository.find(text -> true).isEmpty());
        assertEquals(0, repository.size());
    }

    /* null не приймається ні як запис, ні як умова, і сховище після цього не змінюється. */
    @Test
    void rejectsNullItemAndCondition() {
        Repository<String> repository = new Repository<>();

        assertThrows(NullPointerException.class, () -> repository.add(null));
        assertThrows(NullPointerException.class, () -> repository.find(null));
        assertEquals(0, repository.size());
    }
}
