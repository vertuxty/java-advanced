package info.kgeorgiy.ja.leshchev.arrayset;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.Collections;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.TreeSet;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {
    private final List<E> data;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        // :NOTE: no copy from other constructor
        this(Collections.emptyList(), null);
    }

    public ArraySet(Collection<E> collection, Comparator<? super E> comparator) {
        TreeSet<E> tmp = new TreeSet<>(comparator);
        tmp.addAll(collection);
        // :NOTE: new ArrayList()
        this.data = List.copyOf(tmp);
        this.comparator = comparator;
//        List<E> tmp = new ArrayList<>(collection);
//        tmp.sort(comparator);
//        this.data = collection.isEmpty() ? new ArrayList<>(collection)
//                : removeDuplicates(tmp);
    }

    public ArraySet(Collection<E> collection) {
        this(collection, null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this(Collections.emptyList(), comparator);
    }

    private ArraySet(List<E> list, Comparator<? super E> comparator) {
        this.data = list;
        this.comparator = comparator;
    }
    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSetCustom(fromElement, toElement, true);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        try {
            return subSetCustom(first(), toElement, true);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return new ArraySet<>(comparator);
        }
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        try {
            return subSetCustom(fromElement, last(), false);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return new ArraySet<>(comparator);
        }
    }

    @Override
    public E first() throws NoSuchElementException {
        return data.getFirst();
    }

    @Override
    public E last() throws NoSuchElementException {
        return data.getLast();
    }

    @Override
    public Iterator<E> iterator() {
        return data.iterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) { // (E) o | OR | (Comparator<Object>) ?
        return Collections.binarySearch(data, (E) o, comparator) >= 0;
    }
    @Override
    public int size() {
        return data.size();
    }

    private SortedSet<E> subSetCustom(E fromElement, E toElement, boolean isStrict) {
        if (comparator.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        int posFrom = getPos(fromElement, true);
        int posTo = getPos(toElement, isStrict);
        List<E> subList = data.subList(posFrom, posTo);
        return new ArraySet<>(subList, comparator);
    }

    private int getPos(E element, boolean isStrict) {
        int pos = Collections.binarySearch(data, element, comparator);
        pos =  pos < 0 ? -pos - 1 : pos;
        return !isStrict ? pos + 1 : pos;
    }

    private List<E> removeDuplicates(List<E> list) {
        List<E> data = new ArrayList<>();
        E tmp = list.getFirst();
        for (E element: list) {
            if (getComparator().compare(element, tmp) != 0) {
                data.add(tmp);
                tmp = element;
            }
        }
        data.add(tmp);
        return data;
    }
    @SuppressWarnings("unchecked")
    private Comparator<? super E> getComparator() {
        return comparator != null ? comparator : (Comparator<? super E>) Comparator.naturalOrder();
    }
}
