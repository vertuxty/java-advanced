package info.kgeorgiy.ja.leshchev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HelloNonblockingUDPClient implements HelloClient {
    private void writeChannel(SelectionKey key) throws IOException {
        DatagramChannel datagramChannel = (DatagramChannel) key.channel();
        final ClientChannelUDP clientChannelUDP = (ClientChannelUDP) key.attachment();
        clientChannelUDP.byteBuffer.clear();
        clientChannelUDP.byteBuffer.put(clientChannelUDP.getRequest().getBytes(StandardCharsets.UTF_8));
        clientChannelUDP.byteBuffer.flip();
        datagramChannel.send(clientChannelUDP.byteBuffer, clientChannelUDP.socketAddress);
    }

    private boolean readChannel(DatagramChannel datagramChannel, ClientChannelUDP clientChannelUDP) throws IOException {
        String expectedResponse = clientChannelUDP.getExpectedResponse();
        clientChannelUDP.byteBuffer.clear(); // к записи
        datagramChannel.socket().setSoTimeout(HelloUtil.TIMEOUT);
        datagramChannel.receive(clientChannelUDP.byteBuffer);
        clientChannelUDP.byteBuffer.flip();
        clientChannelUDP.increaseTotal();
        final String receivedMsg = new String(clientChannelUDP.byteBuffer.array(),
                0, clientChannelUDP.byteBuffer.limit(), StandardCharsets.UTF_8).trim();
        return expectedResponse.equals(receivedMsg);
    }

    private void runSelector(Selector selector) {
        while (!selector.keys().isEmpty()) {
            try {
                selector.select(HelloUtil.TIMEOUT);
            } catch (IOException e) {
                System.err.println("Selector timeout is end, no channels was found");
                return;
            }
            Set<SelectionKey> selectionKey = selector.selectedKeys();
            if (selectionKey.isEmpty()) {
                selector.keys().forEach(selectionKey1 -> selectionKey1.interestOps(HelloUtil.WRITE));
            }
            for (final Iterator<SelectionKey> i = selectionKey.iterator(); i.hasNext(); ) {
                try {
                    final SelectionKey key = i.next();
                    if (!key.isValid()) {
                        continue;
                    }
                    try {
                        if (key.isWritable()) {
                            try {
                                writeChannel(key);
                                key.interestOps(HelloUtil.READ);
                            } catch (IOException e) {
                                key.interestOps(HelloUtil.WRITE);
                            }
                        }
                        if (key.isReadable()) {
                            DatagramChannel datagramChannel = (DatagramChannel) key.channel();
                            final ClientChannelUDP clientChannelUDP = (ClientChannelUDP) key.attachment();
                            try {
                                if (!readChannel(datagramChannel, clientChannelUDP)) {
                                    clientChannelUDP.decreaseTotal();
                                }
                            } catch (SocketException e) {
                                clientChannelUDP.decreaseTotal();
                            }
                            key.interestOps(HelloUtil.WRITE);
                            if (clientChannelUDP.checkBound()) {
                                key.channel().close();
                                datagramChannel.close();
                            }
                        }
                    } catch (IOException e) {
                        //
                    }
                } finally {
                    i.remove();
                }
            }
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        SocketAddress socketAddress;
        try {
            socketAddress = new InetSocketAddress(host, port);
        } catch (IllegalArgumentException e) {
            System.err.printf("Illegal port argument: %d%n", port);
            return;
        }
        try (Selector selector = Selector.open()) {
            for (int threadNum = 1; threadNum < threads + 1; threadNum++) {
                DatagramChannel datagramChannel = HelloUtil.createDatagramChannel(false, socketAddress);
                ClientChannelUDP clientChannelUDP = new ClientChannelUDP(socketAddress,
                        prefix, threadNum, requests);
                HelloUtil.registerDatagramChannel(datagramChannel, selector, false, clientChannelUDP);
            }
            runSelector(selector);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ClientChannelUDP {
        private final int threadNum;
        private int totalRequests;
        private final int maxRequests;
        private final String prefix;
        private final ByteBuffer byteBuffer;
        private final SocketAddress socketAddress;
        public ClientChannelUDP(final SocketAddress socketAddress,
                                final String prefix,
                                final int threadNum,
                                final int maxRequests) {
            this.threadNum = threadNum;
            this.totalRequests = 0;
            this.maxRequests = maxRequests;
            this.prefix = prefix;
            this.byteBuffer = ByteBuffer.allocate(2048);
            this.socketAddress = socketAddress;
        }

        private void increaseTotal() {
            this.totalRequests += 1;
        }
        private boolean checkBound() {
            return totalRequests >= maxRequests;
        }
        private String getRequest() {
            return String.format("%s%d_%d", prefix, threadNum, totalRequests + 1);
        }
        private String getExpectedResponse() {
            return String.format("Hello, %s", this.getRequest()).trim();
        }

        public void decreaseTotal() {
            this.totalRequests -= 1;
        }
    }
}
