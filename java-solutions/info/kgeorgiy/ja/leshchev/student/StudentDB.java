package info.kgeorgiy.ja.leshchev.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StudentDB implements StudentQuery {
    private final Comparator<Student> studentComparator = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            // Student compareTo
            .thenComparing(Comparator.comparing(Student::getId).reversed());

    private <E, R extends Collection<E>> R get(Collection<Student> collection,
                                               Function<Student, E> getter,
                                               Collector<E, ?, R> collector) {
        return collection.stream().map(getter).collect(collector);
    }

    private <E> List<E> getUnmodifiableList(Collection<Student> collection,
                                            Function<Student, E> getter) {
        return get(collection, getter, Collectors.toUnmodifiableList());
    }

    private <E, R extends Collection<E>> R sort(Collection<E> collection,
                                                Comparator<E> comparator,
                                                Collector<E, ?, R> collector) {
        return collection.stream().sorted(comparator).collect(collector);
    }

    private <E> List<E> sortToUnmodifiableList(Collection<E> collection,
                                               Comparator<E> comparator) {
        return sort(collection, comparator, Collectors.toUnmodifiableList());
    }

    // BinaryOperator.minBy/maxBy
    private final BinaryOperator<String> binaryOperator =
            (prev, next) -> prev.compareTo(next) < 0 ? prev : next;

    private final Function<Student, String> getFullName =
            student -> student.getFirstName() + " " + student.getLastName();

    private String getStudentName(List<Student> students,
                                  Comparator<Student> comparator,
                                  Function<Student, String> getter) {
        return students.stream().max(comparator).map(getter).orElse("");
    }

    // Methods:
    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getUnmodifiableList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getUnmodifiableList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getUnmodifiableList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getUnmodifiableList(students, getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return get(students, Student::getFirstName, Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return getStudentName(students, Student::compareTo, Student::getFirstName);
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortToUnmodifiableList(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortToUnmodifiableList(students, studentComparator);
    }


    private <E> Predicate<Student> findByPredicate(E matcher, Function<Student, E> mapper) {
        // mapper.concat(Predicate.isEqual(matcher))
        return s -> mapper.apply(s).equals(matcher);
    }

    private <E> List<Student> findBy(Collection<Student> collection, E matcher, Function<Student, E> mapper) {
        return sortToUnmodifiableList(collection.stream().
                filter(findByPredicate(matcher, mapper)).toList(),
                studentComparator);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findBy(students, name, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findBy(students, name, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findBy(students, group, Student::getGroup);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors.toMap(Student::getLastName,
                        Student::getFirstName,
                        binaryOperator));
    }
}
