package ru.dksu.semantic;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    public static void main(String[] args) {
        System.out.println(new ReentrantReadWriteLock().isFair());
    }
}