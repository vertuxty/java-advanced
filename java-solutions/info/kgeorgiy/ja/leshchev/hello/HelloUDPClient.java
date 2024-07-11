package info.kgeorgiy.ja.leshchev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.IntStream;


public class HelloUDPClient implements HelloClient {

    private final BiFunction<String, String, String> requestFormat = (s, t) -> String.format("%s_%s", s, t);

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        ExecutorService clientService = Executors.newFixedThreadPool(threads);
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        List<Callable<Boolean>> callables = new ArrayList<>();
        for (int threadNum = 0; threadNum < threads; threadNum++) {
            int finalThreadNum = threadNum + 1;
            Callable<Boolean> task = () -> {
                try (DatagramSocket datagramSocket = new DatagramSocket()) {
                    RequestBody requestBody = new RequestBody(datagramSocket);
                    IntStream.iterate(0, j -> j < requests, j -> j + 1)
                            .forEach(
                                    requestNum -> {
                                        String request = requestFormat.apply(prefix + finalThreadNum, String.valueOf(requestNum + 1));
                                        byte[] data = request.getBytes();
                                        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);
                                        boolean flag = false;
                                        while (!flag) {
                                            try {
                                                DatagramPacket receiveDatagramPacket = new DatagramPacket(new byte[0], 0);
                                                requestBody.send(datagramPacket);
                                                requestBody.receive(receiveDatagramPacket);
                                                flag = requestBody.checkResponse(request, new String(requestBody.clearData(receiveDatagramPacket.getData())));
                                            } catch (IOException e) {
                                                //
                                            }
                                        }
                                    }
                            );
                    datagramSocket.disconnect();
                } catch (SocketException e) {
                    throw new RuntimeException(e);
                }
                return true;
            };
            callables.add(task);
        }
        try {
            clientService.invokeAll(callables);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        clientService.close();
        clientService.shutdown();
    }

    private static class RequestBody {
        private final int RECEIVE_SIZE = 1024;
        private final DatagramSocket socket;
        public RequestBody(DatagramSocket socket) {
            this.socket = socket;
        }


        public void send(DatagramPacket datagramPacket) throws IOException {
            socket.send(datagramPacket);
        }

        public void receive(DatagramPacket receiveDatagramPacket) throws SocketTimeoutException, SocketException {
            receiveDatagramPacket.setData(new byte[RECEIVE_SIZE]);
            receiveDatagramPacket.setLength(RECEIVE_SIZE);
            socket.setSoTimeout(10); // time to wait
            try {
                socket.receive(receiveDatagramPacket);
            } catch (SocketTimeoutException e) {
                throw new SocketTimeoutException(e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private byte[] clearData(byte[] bytes)  {
            int pos = IntStream.iterate(bytes.length - 1, i -> i >= 0, i -> i - 1)
                    .filter(i -> bytes[i] != 0)
                    .findFirst().orElse(0);
            return Arrays.copyOfRange(bytes, 0, pos + 1);
        }

        private boolean checkResponse(String request, String response) {
            return String.format("Hello, %s", request).equals(response);
        }
    }
}