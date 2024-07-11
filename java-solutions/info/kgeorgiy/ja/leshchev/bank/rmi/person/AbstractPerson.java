package info.kgeorgiy.ja.leshchev.bank.rmi.person;

import info.kgeorgiy.ja.leshchev.bank.rmi.account.Account;
import info.kgeorgiy.ja.leshchev.bank.rmi.util.CallType;
import info.kgeorgiy.ja.leshchev.bank.rmi.util.UtilsMy;

import java.rmi.RemoteException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson implements Person {

    private final String name;
    private final String lastName;
    private final String passport;
    private final CallType callType;
    private ConcurrentMap<String, Account> accounts;
    public AbstractPerson(String name, String lastName, String passport, CallType callType) {
        this.name = name;
        this.lastName = lastName;
        if (!passport.matches((new UtilsMy()).passportRegexp)) {
            throw new IllegalArgumentException(String.format("Wrong passport format, should be: %s, got: %s", (new UtilsMy()).passportRegexp, passport));
        }
        this.passport = passport;
        this.callType = callType;
        this.accounts = new ConcurrentHashMap<>();
    }

    protected Account createOrGetAccount(String subId, Account account) {
        if (this.getAccounts().putIfAbsent(subId, account) == null) {
            return account;
        } else {
            return getAccount(subId);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPassport() {
        return passport;
    }

    public ConcurrentMap<String, Account> getAccounts() {
        return accounts;
    }

    public CallType getPersonEnum() {
        return callType;
    }

    @Override
    public void addAccount(Account account) throws RemoteException {
        getAccounts().putIfAbsent(account.getId().split(":")[0], account);
    }

    public void setAccounts(ConcurrentMap<String, Account> accounts) {
        this.accounts = accounts;
    }

    @Override
    public Account getAccount(final String subId) {
        return accounts.get(subId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, lastName, passport, callType, accounts);
    }

    protected void checkID(String subId) {
        if (!subId.matches((new UtilsMy()).subIdRegexp)) {
            throw new IllegalArgumentException(String.format("Wrong id format for: %s", subId));
        }
    }
}
