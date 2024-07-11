#!/bin/bash
runJar="./lib/junit-platform-console-standalone-1.10.2.jar"
libs="./lib/junit-jupiter-engine-5.10.2.jar:
./lib/junit-platform-engine-1.10.2.jar:
./lib/apiguardian-api-1.1.2.jar:
./lib/junit-platform-launcher-1.10.2.jar:
./lib/opentest4j-1.3.0.jar:
./lib/quickcheck-0.6.jar:
./lib/junit-jupiter-api-5.10.2.jar:
./lib/junit-platform-commons-1.10.2.jar:
./lib/jsoup-1.8.1.jar
"
clazz="info.kgeorgiy.ja.leshchev.bank.rmi.BaseTest"
compileDir="./compiledBank"
mkdir $compileDir
# shellcheck disable=SC2086
javac -d $compileDir -cp "$libs:$runJar" ./java-solutions/info/kgeorgiy/ja/leshchev/bank/rmi/account/*.java \
                                          ./java-solutions/info/kgeorgiy/ja/leshchev/bank/rmi/bank/*.java \
                                          ./java-solutions/info/kgeorgiy/ja/leshchev/bank/rmi/person/*.java \
                                          ./java-solutions/info/kgeorgiy/ja/leshchev/bank/rmi/util/*.java \
                                          ./java-solutions/info/kgeorgiy/ja/leshchev/bank/rmi/*.java
java -jar $runJar execute --class-path $compileDir --select-class $clazz
rm -rf compiledBank
