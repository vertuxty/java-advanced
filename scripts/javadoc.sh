#!/bin/bash
SHARED=java-advanced-2024/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor
SOLUTIONS=java-solutions/info/kgeorgiy/ja/leshchev/implementor
javadoc -author -private -doctitle -version ../$SOLUTIONS/Implementor.java \
   ../../$SHARED/Impler.java \
   ../../$SHARED/JarImpler.java \
   ../../$SHARED/ImplerException.java -d javadocs


#SHARED=shared/java-advanced-2024/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor
#SOLUTIONS=java-solutions/info/kgeorgiy/ja/leshchev/implementor
#javadoc -author -private -doctitle -version ../$SOLUTIONS/Implementor.java \
#   ../../../shared/java-advanced-2024/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/Impler.java \
#   ../../../shared/java-advanced-2024/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/JarImpler.java \
#   ../../../shared/java-advanced-2024/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/ImplerException.java -d javadocs
#
#javadoc -doctitle -author -version ../java-solutions/info/kgeorgiy/ja/leshchev/iterative/IterativeParallelism.java ../java-solutions/info/kgeorgiy/ja/leshchev/iterative/MyThread.java ../../../shared/java-advanced-2024/modules/info.kgeorgiy.java.advanced.iterative/info/kgeorgiy/java/advanced/iterative/ScalarIP.java -d javadocs
