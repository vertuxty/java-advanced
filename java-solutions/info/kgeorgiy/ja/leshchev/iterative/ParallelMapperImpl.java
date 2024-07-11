package info.kgeorgiy.ja.leshchev.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
/**
 * {@inheritDoc}
 * */
public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final List<Thread> threadList = new ArrayList<>();
    /**
     * Constructor for parallelMapper. Generate threads and tasks.
     * @param threads a count of threads to launch.
     * */
    public ParallelMapperImpl(int threads) throws InterruptedException {
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(
                    () -> {
                        while (!Thread.interrupted()) {
                            try {
                                get();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
            );
            threadList.add(thread);
            threadList.get(i).start();
        }
    }

    private void set(Runnable task) throws InterruptedException {
        int QUEUE_MAX_SIZE = 1000;
        synchronized (tasks) {
            while (tasks.size() >= QUEUE_MAX_SIZE) {
                tasks.wait();
            }
            tasks.add(task);
            tasks.notifyAll();
        }
    }

    private void get() throws InterruptedException {
        Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
            tasks.notifyAll();
        }
        task.run();
    }
    /**
     * {@inheritDoc}
     * */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        int sz = args.size();
        List<R> result = new ArrayList<>(Collections.nCopies(sz, null));
        Indexing indexing = new Indexing(sz);
        IntStream.iterate(0, i -> i < sz, i -> i + 1)
                .forEach(i ->
                        {
                            try {
                                set(
                                        () -> {
                                                result.set(i, f.apply(args.get(i)));
                                                indexing.increaseIndex();
                                        }
                                );
                            } catch (InterruptedException e) {
                                //
                            }
                        }
                );
        indexing.syncIndex();
        return result;
    }

    private static class Indexing {
        private final int bSize;
        private int index = 0;
        public Indexing(int size) {
            this.bSize = size;
        }
        public synchronized void increaseIndex() {
            ++index;
            if (index == bSize) {
                notify();
            }
        }
        public synchronized void syncIndex() throws InterruptedException {
            while (index < bSize) {
                wait();
            }
        }
    }
    /**
     * {@inheritDoc}*/
    @Override
    public void close() {
        for (Thread thread : threadList) {
            thread.interrupt();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                //
            }
        }
    }
}
