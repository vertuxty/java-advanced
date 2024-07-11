package info.kgeorgiy.ja.leshchev.hello;

import com.sun.source.tree.LiteralTree;
import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.IntStream;


public class HelloUDPServer implements NewHelloServer {
    private ExecutorService executorService;
    private ExecutorService portsExecutors;
    private final List<DatagramSocket> datagramSocketList = new ArrayList<>();
    private final BiFunction<String,
            String, byte[]> answerFormat = (g, s) -> g.replaceAll("\\$", s).getBytes(StandardCharsets.UTF_8);
    private Map<Integer, String> ported;
    @Override
    public void start(int threads, Map<Integer, String> ports) {
        ported = ports;
        if (ports.isEmpty()) {
            return;
        }
        for (int port: ports.keySet()) {
            try {
                datagramSocketList.add(new DatagramSocket(port));
            } catch (SocketException e) {
                System.err.printf("Socket with port %d is not created: %s%n", port, e.getMessage());
            }
        }
        executorService = Executors.newFixedThreadPool(threads);
        portsExecutors = Executors.newFixedThreadPool(ported.size());
        List<Callable<Boolean>> tasks = createTasks();
        IntStream.iterate(0, i -> i < threads, i -> i + 1)
                .forEach(i -> executorService.execute(() -> {
                    try {
                        portsExecutors.invokeAll(tasks);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }


    private List<Callable<Boolean>> createTasks() {
        List<Callable<Boolean>> tasks = new ArrayList<>();
        datagramSocketList.forEach(socket -> {
            tasks.add(task(socket));
        });
        return tasks;
    }

    private Callable<Boolean> task(DatagramSocket socket) {
        return () -> {
            ResponseBody responseBody = new ResponseBody(socket);
            int port = socket.getLocalPort();
            while (!socket.isClosed()) {
                try {
                    responseBody.receive();
                    responseBody.send();
                } catch (IOException e) {
                    //
                    System.err.printf("Socket %d occurred: %s%n", port,  e.getMessage());
                }
            }
            return true;
        };
    }

    @Override
    public void close() {
        for (DatagramSocket datagramSocket1 : datagramSocketList) {
            datagramSocket1.close();
            datagramSocket1.disconnect();
        }
        if (!ported.isEmpty()) {
            executorService.close();
            portsExecutors.close();
        }
    }

    private class ResponseBody {
        private final int SIZE = 1024;
        private final byte[] buffer = new byte[SIZE];
        private final DatagramSocket socket;
        private byte[] receivedData;
        private int port;
        private InetAddress inetAddress;
        int len;

        public ResponseBody(DatagramSocket socket) {
            this.socket = socket;
        }

        public void receive() throws IOException {
            DatagramPacket datagramPacket = new DatagramPacket(buffer, SIZE);
            socket.setSoTimeout(10);
            socket.receive(datagramPacket);
            inetAddress = datagramPacket.getAddress();
            port = datagramPacket.getPort();
            receivedData = datagramPacket.getData();
            len = datagramPacket.getLength();
        }

        public void send() throws IOException {
            byte[] answer = answerFormat.apply(ported.get(socket.getLocalPort()), new String(receivedData, 0, len));
            DatagramPacket datagramPacketSend = new DatagramPacket(answer, answer.length, inetAddress, port);
            socket.send(datagramPacketSend);
        }
    }
}
