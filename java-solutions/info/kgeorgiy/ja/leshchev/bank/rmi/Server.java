package info.kgeorgiy.ja.leshchev.bank.rmi;

import info.kgeorgiy.ja.leshchev.bank.rmi.bank.Bank;
import info.kgeorgiy.ja.leshchev.bank.rmi.bank.RemoteBank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public final class Server {
    private final static int DEFAULT_PORT = 8888;

    public static void main(final String... args) throws RemoteException {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        final Bank bank = new RemoteBank(port);
        try {
            UnicastRemoteObject.exportObject(bank, port);
            Naming.rebind("//localhost:" + port + "/bank", bank);
            System.out.println("Server started");
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (final MalformedURLException e) {
            System.out.println("Malformed URL");
        }
    }
}
