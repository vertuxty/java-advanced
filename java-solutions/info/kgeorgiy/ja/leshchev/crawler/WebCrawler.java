package info.kgeorgiy.ja.leshchev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
/**
 * Class for downloading pages by links, and extract other links recursevly
 * */
public class WebCrawler implements NewCrawler {
    private final ExecutorService downloaderExecutorService;
    private final ExecutorService extractorExecutorService;
    private final Downloader downloader;

    /**
     * Constructor webcrawler. Create webcrawler for downloading pages.
     * @param downloader a downloader of pages.
     * @param downloaders a count of threads downloader.
     * @param extractors a count of threads extractors.
     * @param perHost a top border of count pages that can be downloaded from each host at the same time.
     * */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaderExecutorService = Executors.newFixedThreadPool(downloaders);
        this.extractorExecutorService = Executors.newFixedThreadPool(extractors);
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public Result download(String url, int depth) {
        return download(url, depth, ConcurrentHashMap.newKeySet());
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public Result download(String url, int depth, Set<String> excludes) {
        Set<String> executed = ConcurrentHashMap.newKeySet();
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        Set<String> layer = ConcurrentHashMap.newKeySet();
        ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();
        if (contains(url, excludes)) {
            return new Result(new ArrayList<>(), new ConcurrentHashMap<>());
        }
        if (depth == 1) {
            downloadCheck(errors, downloaded, url);
        }
        executed.add(url);
        layer.add(url);
        for (int i = 1; i < depth; i++) {
            layer = getNextLayer(layer, errors, executed, downloaded, excludes);
            if (i == depth - 1) {
                checkLast(layer, errors, executed, downloaded, excludes);
            }
            executed.addAll(layer);
        }
        executed.removeAll(errors.keySet());
        return new Result(executed.parallelStream().toList(), errors);
    }

    private boolean contains(String url, Set<String> excluded) {
        for (String s: excluded) {
            if (url.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private void downloadCheck(ConcurrentMap<String, IOException> errors, Set<String> downloaded, String url) {
        if (!downloaded.contains(url)) {
            try {
                downloaded.add(url);
                downloader.download(url);
            } catch (IOException e) {
                errors.put(url, e);
            }
        }
    }

    private void checkLast(Set<String> layer, ConcurrentMap<String, IOException> errors, Set<String> executed, Set<String> downloaded, Set<String> excludes) {
        List<Future<?>> futures = new ArrayList<>();
        layer.forEach(url -> {
            if (!contains(url, excludes)) {
                futures.add(downloaderExecutorService.submit(() -> {
                    downloadCheck(errors, downloaded, url);
                }));
            }
        });
        dumpFutures(futures);
    }

    private Set<String> getNextLayer(Set<String> layer, ConcurrentMap<String, IOException> errors, Set<String> executed, Set<String> downloaded, Set<String> excludes) {
        Set<String> newLayer = ConcurrentHashMap.newKeySet();
        List<Future<?>> futuresList = new ArrayList<>();
        for (String url: layer) {
            AtomicReference<Document> documentAtomicReference = new AtomicReference<>();
            Future<?> futureDownload = downloaderExecutorService.submit(
                () -> {
                    if (!downloaded.contains(url) && !contains(url, excludes)) {
                        downloaded.add(url);
                        try {
                            documentAtomicReference.set(downloader.download(url));
                        } catch (IOException e) {
                            errors.put(url, e);
                        }
                    }
                }
            );
            Future<?> futureExtractor = extractorExecutorService.submit(
                () -> {
                    try {
                        futureDownload.get();
                        Document document = documentAtomicReference.get();
                        if (document != null) {
                            newLayer.addAll(document.extractLinks().parallelStream()
                                    .filter(s -> !downloaded.contains(s) && !layer.contains(s) && !contains(s, excludes)).toList());
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        errors.put(url, e);
                    }
                }
            );
            futuresList.add(futureExtractor);
        }
        dumpFutures(futuresList);
        return newLayer;
    }


    private void dumpFutures(List<Future<?>> futures) {
        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Main class
     * @param args a arguments from command line.
     * */
    public static void main(String[] args) throws IOException {
        WebCrawler webCrawler = new WebCrawler(new CachingDownloader(1), 100, 100, 100);
        Result result = webCrawler.download("https://www.youtube.com/", 3);
        webCrawler.close();
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public void close() {
        extractorExecutorService.shutdown();
        extractorExecutorService.close();
        downloaderExecutorService.shutdown();
        downloaderExecutorService.close();
    }
}
