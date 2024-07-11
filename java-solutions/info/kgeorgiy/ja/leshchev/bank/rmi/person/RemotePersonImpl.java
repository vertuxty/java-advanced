package info.kgeorgiy.ja.leshchev.bank.rmi.person;

import info.kgeorgiy.ja.leshchev.bank.rmi.account.Account;
import info.kgeorgiy.ja.leshchev.bank.rmi.bank.Bank;
import info.kgeorgiy.ja.leshchev.bank.rmi.util.CallType;

import java.rmi.RemoteException;
import java.util.Objects;

public class RemotePersonImpl extends AbstractPerson implements RemotePerson {
    private final Bank bank;

    public RemotePersonImpl(String name, String lastName, String passport, Bank bank) {
        super(name, lastName, passport, CallType.REMOTE);
        this.bank = bank;
    }

    @Override
    public Account createAccount(String subId) throws RemoteException {
        super.checkID(subId);
        Account account = bank.createAccount(subId, this.getPassport());
        return super.createOrGetAccount(subId, account);
    }

    private Bank getBank() {
        return bank;
    }

    @Override
    public String toString() {
        return "RemotePerson {" +
                "\n name=" + getName() +
                ",\n lastName=" + getLastName() +
                ",\n passport=" + getPassport() +
                ",\n type=" + getPersonEnum() +
                ",\n accounts=" + getAccounts() +
                ",\n bank=" + getBank() +
                "} \n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemotePersonImpl that)) return false;
        return Objects.equals(super.getName(), that.getName()) &&
                Objects.equals(super.getLastName(), that.getLastName()) &&
                Objects.equals(super.getPassport(), that.getPassport()) &&
                super.getPersonEnum() == that.getPersonEnum() &&
                Objects.equals(super.getAccounts(), that.getAccounts()) &&
                Objects.equals(this.getBank(), that.getBank());
    }
}
