package info.kgeorgiy.ja.leshchev.bank.rmi.bank;

import info.kgeorgiy.ja.leshchev.bank.rmi.util.CallType;
import info.kgeorgiy.ja.leshchev.bank.rmi.account.Account;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.LocalPersonImpl;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.Person;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.RemotePerson;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    /**
     * Creates a new account with specified identifier if it does not already exist.
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(final String id, final String prefix) throws RemoteException;
    Person getPerson(final String passport, CallType callType) throws RemoteException;
    RemotePerson createPerson(final String name,
                              final String lastName,
                              final String passport) throws RemoteException;
    LocalPersonImpl getLocalPerson(final String passport, CallType callType) throws RemoteException;
    RemotePerson getRemotePerson(String passport, CallType callType) throws RemoteException;
}