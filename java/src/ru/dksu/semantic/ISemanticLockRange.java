package ru.dksu.semantic;

public interface ISemanticLockRange {
    public record SLRanges(
            long id,
            int left,
            int right
    ) {}
    public record SLOp(
            int operationNumber,
            SLRanges ranges
    ) {}

    SLOp lock(int operationNumber, int left, int right);
    void unlock(SLOp p);
}
