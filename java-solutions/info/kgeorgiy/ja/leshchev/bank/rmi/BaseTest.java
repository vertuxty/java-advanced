package info.kgeorgiy.ja.leshchev.bank.rmi;

import info.kgeorgiy.ja.leshchev.bank.rmi.account.Account;
import info.kgeorgiy.ja.leshchev.bank.rmi.account.RemoteAccount;
import info.kgeorgiy.ja.leshchev.bank.rmi.bank.Bank;
import info.kgeorgiy.ja.leshchev.bank.rmi.bank.RemoteBank;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.LocalPersonImpl;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.Person;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.RemotePerson;
import info.kgeorgiy.ja.leshchev.bank.rmi.person.RemotePersonImpl;
import info.kgeorgiy.ja.leshchev.bank.rmi.util.CallType;
import org.junit.jupiter.api.*;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@DisplayName("Test Bank")
public class BaseTest {

    @Nested
    public class TestLast {
        @BeforeAll
        static void before() {
            System.err.println("::::: Testing clients :::::");
        }
        @AfterAll
        static void after() {
            System.err.println("::::: Clients testing end :::::");
        }
        @BeforeEach
        void eachBefore() {
            System.err.println("==== NEW TEST ====");
        }
        @Test
        void test_7_run_client() throws RemoteException, MalformedURLException, NotBoundException {
            int port = 8889;
            Registry registry = LocateRegistry.createRegistry(port);
            Server.main(String.valueOf(port));
            Assertions.assertDoesNotThrow(() -> Client.main(new String[]{"Tests", "TET", "000000001", "1", "100", String.valueOf(port)}));
            Naming.unbind("//localhost:" + port + "/bank");
            UnicastRemoteObject.unexportObject(registry, true);
        }

        @Test
        void test_8_multi_thread_run_client() throws RemoteException, InterruptedException {
            int port = 8890;
            Registry registry = LocateRegistry.createRegistry(port);
            Server.main(String.valueOf(port));
            List<Thread> threads = new ArrayList<>();
            for (int  i =0 ; i < 5; i++ ) {
                Thread thread = new Thread(() -> {
                    try {
                        Client.main(new String[]{"Tests", "TET", "000000001", "1", "100", String.valueOf(port)});
                    } catch (RemoteException | MalformedURLException | NotBoundException e) {
                        throw new RuntimeException(e);
                    }
                });
                thread.start();
                threads.add(thread);
                Thread.sleep(5);
            }
            for (Thread thread: threads) {
                thread.join();
            }
            UnicastRemoteObject.unexportObject(registry, true);
        }

        @Test
        void test_9_many_client_account() throws RemoteException, MalformedURLException, NotBoundException {
            int port = 8891;
            Registry registry = LocateRegistry.createRegistry(port);
            Server.main(String.valueOf(port));
            Bank bank = (Bank) Naming.lookup("//localhost:" + port + "/bank");
            int value1 = 100;
            int value2 = 2000;
            String[] client_query_1 = new String[]{"Client-1", "clientNine", "111", "1", String.valueOf(value1), String.valueOf(port)};
            String[] client_query_2 = new String[]{"Client-1", "clientNine", "111", "2", String.valueOf(value2), String.valueOf(port)};
            Client.main(client_query_1);
            Client.main(client_query_2);
//            RemPerson client = bank.getPerson("111", CallType.REMOTE);
            RemotePerson client = bank.getRemotePerson("111", CallType.REMOTE);
            Assertions.assertNotNull(client, "Got null in client person");
            Assertions.assertNotNull(client.getAccount("1"), "Got null in client person account 1");
            Assertions.assertNotNull(client.getAccount("2"), "Got null in client person account 2");

            int run1 = 10;
            int run2 = 20;

            runClient(run1, client_query_1);
            runClient(run2, client_query_2);

            Assertions.assertEquals((run1 + 1) * value1, client.getAccount("1").getAmount());
            Assertions.assertEquals((run2 + 1) * value2, client.getAccount("2").getAmount());

            Naming.unbind("//localhost:" + port + "/bank");
            UnicastRemoteObject.unexportObject(registry, true);
        }
        @Test
        void test_10_diff_clients() throws RemoteException, MalformedURLException, NotBoundException {
            int port = 8892;
            Registry registry = LocateRegistry.createRegistry(port);
            Server.main(String.valueOf(port));
            Bank bank = (Bank) Naming.lookup("//localhost:" + port + "/bank");
            int value1 = 100;
            int value2 = 24242;
            int value3 = 234234;
            String[][] clients = new String[][]{
                    new String[]{"Client-1", "clientNine", "111", "1", String.valueOf(value1), String.valueOf(port)},
                    new String[]{"Client-2", "clientNine", "222", "1", String.valueOf(value2), String.valueOf(port)},
                    new String[]{"Client-3", "clientNine", "333", "1", String.valueOf(value3), String.valueOf(port)}
            };
            int run1 = 10;
            int run2 = 12;
            int run3 = 15;
            runClient(run1, clients[0]);
            runClient(run2, clients[1]);
            runClient(run3, clients[2]);

            RemotePerson client1 = bank.getRemotePerson("111", CallType.REMOTE);
            RemotePerson client2 = bank.getRemotePerson("222", CallType.REMOTE);
            RemotePerson client3 = bank.getRemotePerson("333", CallType.REMOTE);
            Assertions.assertNotNull(client1, "Got null in client1 person");
            Assertions.assertNotNull(client2, "Got null in client2 person");
            Assertions.assertNotNull(client3, "Got null in client3 person");
            Assertions.assertEquals(run1 * value1, client1.getAccount("1").getAmount());
            Assertions.assertEquals(run2 * value2, client2.getAccount("1").getAmount());
            Assertions.assertEquals(run3 * value3, client3.getAccount("1").getAmount());

            Naming.unbind("//localhost:" + port + "/bank");
            UnicastRemoteObject.unexportObject(registry, true);
        }

        private void runClient(int runCounts, String[] client) throws MalformedURLException, NotBoundException, RemoteException {
            for (int i = 0; i < runCounts; i++) {
                Client.main(client);
            }
        }
    }

    @Nested
    public class TestFirst {
        private static int pos;
        private static Registry registry;
        private static Bank bank;
        private static final String[][] data = new String[][] {{"Vova", "Leshchev", "367351"},
                {"Pasha", "Krasnov", "356324"}, {"Ilya", "Astafiev", "213142"},
                {"Lesha", "Abramov", "234232"}, {"Test", "Testovich", "232343"}};
        private static final Random random = new Random(8888);

        private <T> String errorEquals(T s, T t) {
            return String.format("Expected: %s, but got: %s", s, t);
        }

        @BeforeEach
        void init() throws RemoteException {
            registry = LocateRegistry.createRegistry(8888);
            bank = new RemoteBank(8888);
            UnicastRemoteObject.exportObject(bank, 8888);
        }

        @AfterEach
        void close() throws RemoteException {
            UnicastRemoteObject.unexportObject(registry, true);
            UnicastRemoteObject.unexportObject(bank, true);
            bank = null;
        }

        @BeforeEach
        void genValues() {
            pos = random.nextInt(data.length);
        }

        @Test
        void test_1_create_Remote_Person() throws RemoteException {
            String[] credentials = data[pos];
            RemotePersonImpl basePerson = new RemotePersonImpl(credentials[0], credentials[1], credentials[2], bank);
            Assertions.assertNotNull(basePerson, "base person is Null");
            Person genPerson = bank.createPerson(credentials[0], credentials[1], credentials[2]);
            Assertions.assertNotNull(genPerson, "created person is Null");
            Assertions.assertEquals(basePerson, genPerson, errorEquals(basePerson, genPerson));
        }

        @Test
        void test_2_Remote_Person_Account() throws RemoteException {
            RemotePerson person = bank.createPerson(data[pos][0], data[pos][1], data[pos][2]);
            String subId = String.valueOf(random.nextInt(0, 255));
            person.createAccount(subId);
            Account account = new RemoteAccount(subId);
            Assertions.assertEquals(account, person.getAccount(subId), errorEquals(account, person.getAccount(subId)));
            int randVal = random.nextInt(0, 100000);
            person.getAccount(subId).setAmount(randVal);
            account.setAmount(randVal);
            int bound = random.nextInt(0, 232);
            for (int i = 0; i < bound; i++) {
                person.getAccount(subId).setAmount(person.getAccount(subId).getAmount() + 1);
                account.setAmount(account.getAmount() + 1);
                Assertions.assertEquals(account, person.getAccount(subId), errorEquals(account, person.getAccount(subId)));
            }
        }

        @Test
        void test_3_Local_Persons() throws RemoteException {
            int ITER = random.nextInt(100, 150);
            RemotePerson remotePerson = bank.createPerson("MetOpt", "JVM", "212333");
            Assertions.assertNotNull(remotePerson, String.format("Error, remote person is null for %s", remotePerson));
            String subId = String.valueOf(random.nextInt(255, 1000));
            remotePerson.createAccount(subId);
            Assertions.assertNotNull(remotePerson.getAccount(subId), String.format("Error, account with id=%s do not exist", subId));
            Set<LocalPersonImpl> localPersons = ConcurrentHashMap.newKeySet();
            for (int i = 0; i < ITER; i++) {
                remotePerson.createAccount(String.valueOf(i));
                Assertions.assertNotNull(remotePerson.getAccount(subId), String.format("Error, account with id=%s do not exist", subId));
                remotePerson.getAccount(subId).setAmount(201847 + i);
                LocalPersonImpl localPerson = bank.getLocalPerson(remotePerson.getPassport(), CallType.LOCAL);
                Assertions.assertNotNull(localPerson, String.format("Error, local person is null for %s", remotePerson));
                Assertions.assertFalse(localPersons.contains(localPerson), String.format("Expected: <%b>, got: <%b> ==> %s, are in %s", true, localPerson, localPerson, localPersons));
                localPersons.add(localPerson);
            }
            Assertions.assertEquals(ITER, localPersons.size(), String.format("Expected: %d, but got: %d", ITER, localPersons.size()));

        }

        @Test
        void test_3_Wrong_format() throws RemoteException {
            String pswd = ":::1231";
            Assertions.assertThrows(IllegalArgumentException.class, () -> new LocalPersonImpl("Vova", "Vladimir", pswd), String.format("Expected error: wrong person passport format: %s", pswd));
            Assertions.assertThrows(IllegalArgumentException.class, () -> new RemotePersonImpl("Vova", "Vladimir", pswd, bank), String.format("Expected error: wrong person passport format: %s", pswd));
            Assertions.assertThrows(IllegalArgumentException.class, () -> bank.createPerson("Vova", "Vladimir", pswd), String.format("Expected error: wrong person passport format: %s", pswd));
            RemotePerson person = bank.createPerson("Vova", "Vladimir", "123213");
            Assertions.assertThrows(IllegalArgumentException.class, () -> new RemoteAccount(pswd), "Expected wrong account id format");
            Assertions.assertThrows(IllegalArgumentException.class, () -> person.createAccount(pswd), String.format("Expected error: wrong account id format: %s", pswd));
            Assertions.assertThrows(IllegalArgumentException.class, () -> bank.createAccount(pswd, "123213"), String.format("Expected error: wrong account id format: %s", pswd));
            Assertions.assertThrows(IllegalArgumentException.class, () -> bank.createAccount(pswd, "123213223424324"));
        }

        @Test
        void test_4_MultiThread_One_Account_Changes() throws RemoteException {
            RemotePerson remotePerson = bank.createPerson("Vova", "Leschev", "367351");
            Assertions.assertNotNull(remotePerson, String.format("Error, remote person is null for %s", remotePerson));
            String subId = "01";
            remotePerson.createAccount(subId);
            Assertions.assertNotNull(remotePerson.getAccount(subId), String.format("Error, account with id=%s do not exist", subId));
            final int threads = 5;
            AtomicInteger result = new AtomicInteger(0);
            ExecutorService executorService = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < 10000; i++) {
                executorService.execute(() -> result.addAndGet(100));
                int finalI = i;
                executorService.execute(() -> result.addAndGet(-finalI * 2));
            }
            for (int i = 0; i < 10000; i++) {
                executorService.execute(
                        () -> {
                            try {
                                remotePerson.getAccount(subId).deposit(100);
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                int finalI = i;
                executorService.execute(() -> {
                    try {
                        remotePerson.getAccount(subId).withdraw(finalI * 2);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            executorService.close();
            Assertions.assertEquals(result.get(), remotePerson.getAccount(subId).getAmount());
        }

        @Test
        void test_5_multiply_Accounts_one_Person() throws RemoteException {
            int num = random.nextInt(2, 100);
            RemotePerson remotePerson = bank.createPerson("Vova", "Leschev", "367351");
            Assertions.assertNotNull(remotePerson, String.format("Error, remote person is null for %s", remotePerson));
            List<String> ids = new ArrayList<>();
            IntStream.iterate(0, i -> i < num, i -> i + 1)
                    .forEach(i -> {
                        ids.add(String.valueOf(i));
                        try {
                            remotePerson.createAccount(ids.get(i));
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });
            Assertions.assertEquals(num, remotePerson.getAccounts().size(), String.format("Error, expected: %d accounts, got: %d", num, remotePerson.getAccounts().size()));
            IntStream.iterate(0, i -> i < num, i -> i + 1)
                    .forEach(i -> {
                        try {
                            Account account = remotePerson.getAccounts().get(String.valueOf(i));
                            Assertions.assertEquals(0, account.getAmount(), String.format("Expected: %d, got: %d", 0, account.getAmount()));
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        @Test
        void test_6_multiply_Accounts_changes() throws RemoteException {
            int num = 10;
            RemotePerson remotePerson = bank.createPerson("Vova", "Leschev", "367351");
            Assertions.assertNotNull(remotePerson, String.format("Error, remote person is null for %s", remotePerson));

            List<String> ids = new ArrayList<>();
            ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
            IntStream.iterate(0, i -> i < num, i -> i + 1)
                    .forEach(i -> {
                        ids.add(String.valueOf(i));
                        try {
                            remotePerson.createAccount(ids.get(i));
                            accounts.put(ids.get(i), new RemoteAccount(ids.get(i)));
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });
            Assertions.assertEquals(num, remotePerson.getAccounts().size(), String.format("Error, expected: %d accounts, got: %d", num, remotePerson.getAccounts().size()));
            Assertions.assertEquals(num, accounts.size(), String.format("Error, expected: %d accounts, got: %d", num, accounts.size()));
            IntStream.iterate(0, i -> i < num, i -> i + 1)
                    .forEach(i -> {
                        try {
                            accounts.get(ids.get(i)).setAmount((i + 23) % 10);
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });
            Assertions.assertNotEquals(accounts, remotePerson.getAccounts(), String.format("Expected: %s, got: %s", remotePerson.getAccounts(), accounts));
            IntStream.iterate(0, i -> i < num, i -> i + 1)
                    .forEach(i -> {
                        try {
                            remotePerson.getAccounts().get(ids.get(i)).deposit((i + 23) % 10);
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });
            Assertions.assertEquals(accounts, remotePerson.getAccounts(), String.format("Expected: %s, got: %s", accounts, remotePerson.getAccounts()));
        }

        @Test
        void test_7_local_changes_remote_changes() throws RemoteException {
            RemotePerson person = bank.createPerson("Vova", "Leshchev", "122222");
            Account account = bank.createAccount("1", "122222");
            account.setAmount(1000);
            LocalPersonImpl localPerson = bank.getLocalPerson("122222", CallType.LOCAL);
            Account localAccount = localPerson.getAccount("1");
            Assertions.assertEquals(account, localAccount, String.format("Expected account: %s, got account: %s", account, localAccount));
            account.deposit(111);
            Assertions.assertNotEquals(account, localAccount, String.format("Accounts should not be equals. LocalPerson account: %s, RemotePerson account: %s", localAccount, account));
            localPerson.createAccount("2");
            localPerson.getAccount("2").deposit(12313);
            Assertions.assertNotEquals(person.getAccounts(), localPerson.getAccounts(), String.format("LocalPerson accounts should not be equals to Remote accounts!: local -> %s, remote -> %s", localPerson.getAccounts(), person.getAccounts()));
        }
    }
}