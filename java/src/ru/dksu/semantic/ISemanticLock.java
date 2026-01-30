package ru.dksu.semantic;

public interface ISemanticLock {
    void lock(int operationNumber);
    void unlock(int operationNumber);
}
