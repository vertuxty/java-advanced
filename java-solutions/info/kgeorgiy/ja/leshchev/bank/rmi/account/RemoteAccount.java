package info.kgeorgiy.ja.leshchev.bank.rmi.account;

import info.kgeorgiy.ja.leshchev.bank.rmi.util.UtilsMy;

import java.util.Objects;

public class RemoteAccount implements Account {
    private final String id;
    private int amount;
    public RemoteAccount(final String id) {
        if (!id.matches((new UtilsMy()).subIdRegexp)) {
            throw new IllegalArgumentException("Wrong id format");
        }
        this.id = id;
        amount = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        this.amount = amount;
        notifyAll();
    }

    @Override
    public synchronized void deposit(final int amount) {
        this.amount += amount;
    }

    @Override
    public synchronized void withdraw(final int amount) {
        this.amount -= amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteAccount that)) return false;
        return amount == that.amount && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, amount);
    }

    @Override
    public String toString() {
        return "RemoteAccount{" +
                "id='" + id + '\'' +
                ", amount=" + amount +
                '}';
    }
}
