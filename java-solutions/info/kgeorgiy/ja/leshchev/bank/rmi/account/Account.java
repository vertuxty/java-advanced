package info.kgeorgiy.ja.leshchev.bank.rmi.account;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote, Serializable {
    /** Returns account identifier. */
    String getId() throws RemoteException;

    /** Returns amount of money in the account. */
    int getAmount() throws RemoteException;

    /** Sets amount of money in the account. */
    void setAmount(int amount) throws RemoteException;

    void deposit(final int amount) throws RemoteException;
    void withdraw(final int amount) throws RemoteException;
}
