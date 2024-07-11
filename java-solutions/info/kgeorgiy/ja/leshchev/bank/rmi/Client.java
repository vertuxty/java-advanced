package info.kgeorgiy.ja.leshchev.bank.rmi;

import info.kgeorgiy.ja.leshchev.bank.rmi.account.Account;
import info.kgeorgiy.ja.leshchev.bank.rmi.bank.Bank;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.Person;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.RemotePerson;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public final class Client {
    /** Utility class. */
    private Client() {}

    public static void main(final String[] args) throws RemoteException, MalformedURLException, NotBoundException {
        if (args == null) {
            throw new RemoteException("No credentials format was given");
        }
        if (args.length != 6) {
            throw new RemoteException(String.format("Wrong credentials format. Expected 5 arguments, got: %d", args.length));
        }
        int port = Integer.parseInt(args[5]);
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost:" + port + "/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }
        RemotePerson person = bank.createPerson(args[0], args[1], args[2]);
        final String accountId = args[3];
        Account account = person.getAccount(accountId);
        if (account == null) {
            System.out.println("Creating account");
            account = person.createAccount(accountId);
        } else {
            System.out.println("Account already exists");
            account = person.getAccount(accountId);
        }
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.deposit(Integer.parseInt(args[4]));
        System.out.println("Money: " + account.getAmount());
    }

    static Person makePerson(String[] args, Bank bank) throws RemoteException {
        final String name = args[0];
        final String lastName = args[1];
        final String passport = args[2];
        return bank.createPerson(name, lastName, passport);
    }
}
