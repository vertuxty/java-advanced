package info.kgeorgiy.ja.leshchev.iterative;

import info.kgeorgiy.java.advanced.iterative.NewScalarIP;
import info.kgeorgiy.java.advanced.iterative.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
/**
 * Utility for paralleling simple task on lists.
 *
 * @author vertuxty
 * @version 1.0
 */
public class IterativeParallelism implements NewScalarIP {

    private final ParallelMapper parallelMapper;
    /**
     * Default constructor. Initialize a util.
     * */
    public IterativeParallelism() {
        this.parallelMapper = null;
    }
    /**
     * Create IterativeParallelism. If parallelMapper null, we creates own threads.
     * @param parallelMapper a parallelMapper.
     * */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }
    private <T, E> E paralleling(int threads,
                                 List<? extends T> values,
                                 Function<Stream<? extends T>, E> function,
                                 Function<List<E>, E> end)
            throws InterruptedException {
        if (values.isEmpty()) {
            throw new InterruptedException("Error, list is empty, nothung to do");
        }
        List<E> threadResult = new ArrayList<>();
        int optimizedCount = getThreadsCount(threads, values.size());
        int[] threadsChunkSize = getThreadsDataSize(optimizedCount, values.size());
        List<Supplier<List<? extends T>>> lists = getSuppliers(values, optimizedCount, threadsChunkSize);
        if (parallelMapper == null) {
            List<MyThread<T, E>> threadList = getMyThreads(lists, function);
            for (MyThread<T, E> myThread: threadList) {
                myThread.join();
                threadResult.add(myThread.getAnswer());
            }
        } else {
            threadResult = parallelMapper.map(s -> function.apply(s.get().stream()), lists);
            return end.apply(threadResult);
        }
        return end.apply(threadResult);
    }

    private static <T> List<Supplier<List<? extends T>>> getSuppliers(List<? extends T> values, int optimizedCount, int[] threadsChunkSize) {
        List<Supplier<List<? extends T>>> lists = new ArrayList<>();
        int prev = 0;
        for (int i = 0; i < optimizedCount; i++) {
            final int prevFinal = prev;
            int finalI = i;
            lists.add(
                    () -> values.subList(prevFinal, nextPos(prevFinal, threadsChunkSize[finalI], values.size()))
            );
            prev = nextPos(prev, threadsChunkSize[i], values.size());
        }
        return lists;
    }

    private <T, E> List<MyThread<T, E>> getMyThreads(List<Supplier<List<? extends T>>> values,
                                                     Function<Stream<? extends T>, E> function) {
        List<MyThread<T, E>> threadList = new ArrayList<>();
        for (Supplier<List<? extends T>> list: values) {
            MyThread<T, E> thread = new MyThread<>(function, list);
            threadList.add(thread);
            thread.start();
        }
        return threadList;
    }

    private <T> List<T> coverFunction(List<T> values, int step) {
        return IntStream.iterate(0, i -> i < values.size(), i -> i + step).mapToObj(values::get).toList();
    }
    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return paralleling(threads, values,
                data -> data.max(comparator).orElse(null),
                res -> res.stream().max(comparator).orElse(null));
    }
    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        return maximum(threads, coverFunction(values, step), comparator);
    }
    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        return minimum(threads, coverFunction(values, step), comparator);
    }
    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return all(threads, coverFunction(values, step), predicate);
    }
    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return any(threads, coverFunction(values, step), predicate);
    }
    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return count(threads, coverFunction(values, step), predicate);
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return paralleling(threads, values,
                data -> data.min(comparator).orElse(null),
                res -> res.stream().min(comparator).orElse(null));
    }
    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> boolean all(int threads,
                           List<? extends T> values,
                           Predicate<? super T> predicate) throws InterruptedException {
        return paralleling(threads, values,
                data -> data.allMatch(predicate),
                res -> res.stream().reduce(Boolean::logicalAnd).orElse(true));
    }
    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> boolean any(int threads,
                           List<? extends T> values,
                           Predicate<? super T> predicate) throws InterruptedException {
        return paralleling(threads, values,
                data -> data.anyMatch(predicate),
                res -> res.stream().reduce(Boolean::logicalOr).orElse(true));
    }
    /**
     * {@inheritDoc}
     * */
    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return paralleling(threads, values,
                data -> data.filter(predicate).mapToInt(i -> 1).sum(),
                res -> res.stream().reduce(0, Integer::sum));
    }

    private static int[] getThreadsDataSize(int initialThreadCount, int valuesSize) {
        int[] threadsDataSize = new int[initialThreadCount];
        int div = initialThreadCount > 0 ? valuesSize / initialThreadCount : 0;
        int mod = valuesSize - div * initialThreadCount;
        if (div == 0) {
            Arrays.fill(threadsDataSize, 1);
        } else {
            int ind = 0;
            for (int i = 0; i < initialThreadCount; i++) {
                threadsDataSize[i] = ind < mod ? div + 1 : div;
                ++ind;
            }
        }
        return threadsDataSize;
    }

    private static int getThreadsCount(int initialThreadCount, int valuesSize) {
        return valuesSize / initialThreadCount > 0 ? initialThreadCount : valuesSize;
    }

    private static int nextPos(int prev, int step, int valueSize) { // Вроде работает...
        return Math.min(prev + step, valueSize);
    }
}
