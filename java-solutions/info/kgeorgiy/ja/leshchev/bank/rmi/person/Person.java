package info.kgeorgiy.ja.leshchev.bank.rmi.person;


import info.kgeorgiy.ja.leshchev.bank.rmi.account.Account;

import java.rmi.RemoteException;

public interface Person {
    String getName() throws RemoteException;
    String getLastName() throws RemoteException;
    String getPassport() throws RemoteException;
    Account getAccount(final String subId) throws RemoteException;
    void addAccount(Account account) throws RemoteException;
}
