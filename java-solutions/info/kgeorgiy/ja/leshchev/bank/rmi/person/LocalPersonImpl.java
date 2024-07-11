package info.kgeorgiy.ja.leshchev.bank.rmi.person;

import info.kgeorgiy.ja.leshchev.bank.rmi.account.Account;
import info.kgeorgiy.ja.leshchev.bank.rmi.util.CallType;
import info.kgeorgiy.ja.leshchev.bank.rmi.account.RemoteAccount;

import java.util.Objects;

public class LocalPersonImpl extends AbstractPerson implements LocalPerson {

    public LocalPersonImpl(String name, String lastName, String passport) {
        super(name, lastName, passport, CallType.LOCAL);
    }

    @Override
    public Account createAccount(String subId) {
        super.checkID(subId);
        Account account = new RemoteAccount(subId);
        return super.createOrGetAccount(subId, account);
    }

    @Override
    public String toString() {
        return "LocalPerson{" +
                "name=" + getName() +
                ", lastName=" + getLastName() +
                ", passport=" + getPassport() +
                ", type=" + getPersonEnum() +
                ", accounts=" + getAccounts() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalPersonImpl that)) return false;
        return Objects.equals(super.getName(), that.getName()) &&
                Objects.equals(super.getLastName(), that.getLastName()) &&
                Objects.equals(super.getPassport(), that.getPassport()) &&
                super.getPersonEnum() == that.getPersonEnum() &&
                Objects.equals(super.getAccounts(), that.getAccounts());
    }
}
