package info.kgeorgiy.ja.leshchev.bank.rmi.bank;

import info.kgeorgiy.ja.leshchev.bank.rmi.util.CallType;
import info.kgeorgiy.ja.leshchev.bank.rmi.util.UtilsMy;
import info.kgeorgiy.ja.leshchev.bank.rmi.account.Account;
import info.kgeorgiy.ja.leshchev.bank.rmi.account.RemoteAccount;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.LocalPersonImpl;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.Person;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.RemotePerson;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.RemotePersonImpl;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class RemoteBank implements Bank {
    private final int port;
    private final String splitRegexp = ":";
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Person> persons = new ConcurrentHashMap<>();
    private final BiFunction<String, String, String> genId = (s, t) -> String.format("%s:%s", s, t);

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public Account createAccount(final String subId, final String prefix)
            throws RemoteException {
        if (!persons.containsKey(prefix)) {
            throw new IllegalArgumentException(String.format("Person with this passport=%s does not exist!", prefix));
        }
        if (!genId.apply(prefix, subId).matches((new UtilsMy()).bankAccountIdRegexp)) {
            throw new IllegalArgumentException(String.format("Wrong id structures! got %s expected: <passport><:><person_account_id>", subId));
        }
        Account account = createObjects(getAccount, accounts,
                new RemoteAccount(subId),
                genId.apply(prefix, subId), CallType.BLANK);
        getPerson(prefix, CallType.REMOTE).addAccount(account);
        return account;
    }

    @Override
    public RemotePerson createPerson(final String name,
                                     final String lastName,
                                     final String passport) throws RemoteException {
        RemotePerson remotePerson = new RemotePersonImpl(name, lastName, passport, this);
        if (persons.putIfAbsent(passport, remotePerson) == null) {
            return writeObject(remotePerson);
        }
        return (RemotePerson) getPersonFunction.apply(passport, CallType.REMOTE);
    }

    public RemotePerson getRemotePerson(String passport, CallType callType) throws RemoteException {
        assertHave(persons, passport, String.format("No such person with passport: %s", passport));
        return (RemotePerson) persons.get(passport);
    }

    private <T extends Remote> T createObjects(BiFunction<String, CallType, T> apply,
                                               ConcurrentMap<String, T> map,
                                               T object,
                                               final String identity,
                                               CallType callType) throws RemoteException {
        if (map.putIfAbsent(identity, object) == null) {
            return writeObject(object);
        }
        return apply.apply(identity, callType);
    }

    private <T extends Remote> T writeObject(T object) throws RemoteException {
        UnicastRemoteObject.exportObject(object, port);
        return object;
    }

    public LocalPersonImpl getLocalPerson(final String passport, CallType callType) throws RemoteException {
        assertHave(persons, passport, String.format("No such person with passport: %s", passport));
        Person person = persons.get(passport);
        LocalPersonImpl localPerson = new LocalPersonImpl(person.getName(), person.getLastName(), person.getPassport());
        ConcurrentMap<String, Account> prevAccounts = accounts.entrySet()
                .stream().filter(entry -> entry.getKey().split(splitRegexp)[0].equals(passport))
                .collect(Collectors.toConcurrentMap(
                        entry -> entry.getKey().split(splitRegexp)[1],
                        entry -> new RemoteAccount(entry.getKey().split(splitRegexp)[1])));
        prevAccounts.forEach((key, value) -> {
            try {
                value.setAmount(accounts.get(genId.apply(passport, key)).getAmount());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
        localPerson.setAccounts(prevAccounts);
        return localPerson;
    }

    public Person getPerson(final String passport, CallType callType) {
        return getPersonFunction.apply(passport, callType);
    }

    private Account getAccount(final String id) throws RemoteException {
        assertHave(accounts, id, String.format("Account with id=%s do not exist", id));
        return accounts.get(id);
    }

    private <T> void assertHave(ConcurrentMap<String, T> map,
                                String identity, String message)
            throws RemoteException {
        if (!map.containsKey(identity)) {
            throw new RemoteException(message);
        }
    }

    private final BiFunction<String, CallType, Person> getPersonFunction = (id, en) -> {
        try {
            if (CallType.REMOTE == en) {
                return getRemotePerson(id, en);
            }
            return getLocalPerson(id, en);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    };

    private final BiFunction<String, CallType, Account> getAccount = (id, en) -> {
        try {
            return getAccount(id);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    };
}