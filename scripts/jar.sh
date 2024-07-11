#!/bin/bash

javac -d compiled -cp ../../java-advanced-2024/artifacts/info.kgeorgiy.java.advanced.implementor.jar ../java-solutions/info/kgeorgiy/ja/leshchev/implementor/Implementor.java

jar cmf MANIFEST.MF Implementor.jar -C compiled/ .