package info.kgeorgiy.ja.leshchev.iterative;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
/**
 * Custom Thread for working with lists.
 * @param <E> a type of thread's result
 * @param <T> a type of list elements type.
 * @author vertuxty
 * @version 1.0
 * */
public class MyThread<T, E> extends Thread {

    private E answer;
    private final Supplier<List<? extends T>> data;
    private final Function<Stream<? extends T>, E> function;

    /**
     * Constructor for initialize thread.
     *
     * @param function a function that applied to list.
     * @param data a list with data.
     * */
    public MyThread(Function<Stream<? extends T>, E> function, Supplier<List<? extends T>> data) {
        this.function = function;
        this.data = data;
    }
    /**
     * Rum method.
     * Run a main thread action.
     * */
    @Override
    public void run() {
        answer = function.apply(data.get().stream());
    }
    /**
     * Return an answer of thread.
     * Return result of thread's work.
     * @return answer.
     * */
    public E getAnswer() {
        return answer;
    }
}
