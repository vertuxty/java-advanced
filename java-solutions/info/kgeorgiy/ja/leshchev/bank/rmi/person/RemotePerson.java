package info.kgeorgiy.ja.leshchev.bank.rmi.person;

import info.kgeorgiy.ja.leshchev.bank.rmi.account.Account;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentMap;

public interface RemotePerson extends Remote, Person {
    Account getAccount(final String subId) throws RemoteException;
    Account createAccount(final String subId) throws RemoteException;
    ConcurrentMap<String, Account> getAccounts() throws RemoteException;
}
