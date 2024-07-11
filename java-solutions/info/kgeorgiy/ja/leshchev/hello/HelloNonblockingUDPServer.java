package info.kgeorgiy.ja.leshchev.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;

public class HelloNonblockingUDPServer implements NewHelloServer {
    private Selector selector;
    private ExecutorService executorService;
    private ExecutorService mainService;
    private ArrayBlockingQueue<DataPackage> toReceiveData;
    private ArrayBlockingQueue<DataPackage> sendData;
    List<DatagramChannel> datagramChannelList = new ArrayList<>();
    @Override
    public void start(int threads, Map<Integer, String> ports) {
        if (ports.isEmpty()) {
            return;
        }
        executorService = Executors.newFixedThreadPool(threads);
        mainService = Executors.newSingleThreadExecutor();
        toReceiveData = new ArrayBlockingQueue<>(ports.size());
        sendData = new ArrayBlockingQueue<>(ports.size());
        try {
            selector = Selector.open();
            ports.forEach((key, value) -> {
                try {
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(key);
                    ServerChannelUDP serverChannelUDP = new ServerChannelUDP(inetSocketAddress, value);
                    DatagramChannel datagramChannel = HelloUtil.createDatagramChannel(true, inetSocketAddress);
                    HelloUtil.registerDatagramChannel(datagramChannel, selector, true, serverChannelUDP);
                    toReceiveData.add(
                        new DataPackage(serverChannelUDP.setBuffer(datagramChannel.socket().getReceiveBufferSize()),
                                null)
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            mainService.execute(this::runServer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void runServer() {
        while (!selector.keys().isEmpty() && !Thread.interrupted()) {
            try {
                selector.select(HelloUtil.TIMEOUT);
            } catch (IOException e) {
                System.err.println("Selector timeout is end, no channels was found");
                return;
            }
            Set<SelectionKey> selectionKey = selector.selectedKeys();
            for (final Iterator<SelectionKey> i = selectionKey.iterator(); i.hasNext(); ) {
                try {
                    final SelectionKey key = i.next();
                    if (!key.isValid()) {
                        continue;
                    }
                    executorService.execute(() -> {
                        if (key.isReadable()) {
                            try {
                                readChannel((DatagramChannel) key.channel(), (ServerChannelUDP) key.attachment());
                                key.interestOps(HelloUtil.WRITE);
                            } catch (IOException e) {
                                //
                            }
                        }
                        if (key.isWritable()) {
                            try {
                                writeChannel((DatagramChannel) key.channel(), (ServerChannelUDP) key.attachment());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            key.interestOps(HelloUtil.READ);
                        }
                    });
                } finally {
                    i.remove();
                }
            }
        }
    }

    private boolean checkChannels() {
        System.err.println(datagramChannelList.stream()
                .filter(datagramChannel -> datagramChannel.socket().isClosed())
                .count());
        return datagramChannelList.stream()
                .filter(datagramChannel -> datagramChannel.socket().isClosed())
                .count() == datagramChannelList.size();
    }

    private void readChannel(DatagramChannel datagramChannel, ServerChannelUDP serverChannelUDP) throws IOException {
        serverChannelUDP.byteBuffer.clear();
        SocketAddress socketAddress = datagramChannel.receive(serverChannelUDP.byteBuffer);
        serverChannelUDP.byteBuffer.flip();
        String received = new String(serverChannelUDP.byteBuffer.array(),
                0, serverChannelUDP.byteBuffer.limit(), StandardCharsets.UTF_8).trim();
        deque.add(
                new Data(received,
                socketAddress,
                serverChannelUDP.inetSocketAddress
        ));
        serverChannelUDP.byteBuffer.clear();
    }

    private boolean writeChannel(DatagramChannel datagramChannel, ServerChannelUDP serverChannelUDP) throws IOException {
        serverChannelUDP.byteBuffer.clear();
        Data receivedData = deque.poll();
        if (serverChannelUDP.inetSocketAddress == receivedData.receiverInetSocketAddress) {
            serverChannelUDP.byteBuffer.put(serverChannelUDP.answer(receivedData.response));
            serverChannelUDP.byteBuffer.flip();
            datagramChannel.send(serverChannelUDP.byteBuffer, receivedData.socketAddress);
            return true;
        }
        return false;
    }

    private static class ServerChannelUDP {
        private final String mappingResponse;
        private final InetSocketAddress inetSocketAddress;
        private ByteBuffer byteBuffer;
        public ServerChannelUDP(final InetSocketAddress inetSocketAddress, String mappingResponse) {
            this.mappingResponse = mappingResponse;
            this.inetSocketAddress = inetSocketAddress;
        }

        private final BiFunction<String, String, String> response = (s, t) -> s.replaceAll("\\$", t);
        private byte[] answer(String request) {
            return response.apply(mappingResponse, request).getBytes(StandardCharsets.UTF_8);
        }

        private ByteBuffer setBuffer(int size) {
            byteBuffer = ByteBuffer.allocate(size);
            return byteBuffer;
        }
    }

    private record Data(SocketAddress socketAddress) {
    }

    @Override
    public void close() {
        executorService.close();
        mainService.close();
        try {
            for(SelectionKey selectionKey: selector.keys()) {
                selectionKey.channel().close();
            }
            selector.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record DataPackage(ByteBuffer byteBuffer, SocketAddress SocketAddress) {
    }
}
