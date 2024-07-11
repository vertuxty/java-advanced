package info.kgeorgiy.ja.leshchev.bank.rmi.util;

public class UtilsMy {
    public final String subIdRegexp = "[a-zA-Z,0-9]+";
    public final String passportRegexp = "[a-zA-Z,0-9]+";
    public final String delRegexp = ":";
    public final String bankAccountIdRegexp = String.format("([a-zA-Z,0-9]+%s[a-zA-Z,0-9]+)", delRegexp);
}
