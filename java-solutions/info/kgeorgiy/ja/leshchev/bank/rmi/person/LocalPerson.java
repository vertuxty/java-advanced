package info.kgeorgiy.ja.leshchev.bank.rmi.person;

import info.kgeorgiy.ja.leshchev.bank.rmi.account.Account;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;

public interface LocalPerson extends Person, Serializable {
    Account createAccount(final String subId);
    ConcurrentMap<String, Account> getAccounts();
}
