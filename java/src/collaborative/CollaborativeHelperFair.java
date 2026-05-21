package collaborative;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import ru.dksu.semantic.SemanticLockGlobalLock;

public class CollaborativeHelperFair {
    public AtomicLong counter = new AtomicLong();
    private Random random = new Random();

    public SemanticLockGlobalLock semanticLock = new SemanticLockGlobalLock(
            4, // R, U, bR, bU
            new int[][]{
                {0, 0, 0, 1},
                {0, 0, 1, 1},
                {0, 1, 0, 1},
                {1, 1, 1, 1}
            }
    );
    public ConcurrentHashMap<Long, ConcurrentLinkedDeque<CollaborativeTask>> tasksQueues = new ConcurrentHashMap<Long, ConcurrentLinkedDeque<CollaborativeTask>>();

    public CollaborativeHelperFair() {
    }

    public <T, TempV> T collaborativeOperation(
            Supplier<Collection<CollaborativeTask>> tasks,
            Supplier<T> reduce,
            boolean isRead
    ) {
        int operationType = isRead ? 2 : 3;
        while (true) {
            SemanticLockGlobalLock.OperationRequest operationRequest = new SemanticLockGlobalLock.OperationRequest(
                    Thread.currentThread().threadId(), operationType
            );
            if (semanticLock.fairness) {
                semanticLock.fairnessQueue.add(operationRequest);
            }
            if (semanticLock.tryLock(operationRequest)) {
                try {
                    long targetOperation = counter.incrementAndGet();

                    this.inProgress.put(targetOperation, new AtomicInteger());

                    var colTasks = tasks.get();
                    this.tasksQueues.put(targetOperation, new ConcurrentLinkedDeque<>());
                    this.tasksQueues.get(targetOperation).addAll(colTasks);

                    this.helpIfNeeded(targetOperation);
                    this.waitForFinish(targetOperation);

                    this.tasksQueues.remove(targetOperation);
                    this.inProgress.remove(targetOperation);
                } finally {
                    semanticLock.unlock(operationType);
                }
                return reduce.get();
            } else {
                this.helpIfNeeded();
                Thread.yield();
            }
        }
    }

    public void helpIfNeeded() {
        helpIfNeeded(-1);
    }

    private ConcurrentLinkedDeque<CollaborativeTask> emptyDeque = new ConcurrentLinkedDeque<>();
    public ConcurrentHashMap<Long, AtomicInteger> inProgress = new ConcurrentHashMap<>();

    public void helpIfNeeded(long targetOperation) {
        long key = targetOperation;
        if (targetOperation == -1) {
            Long[] keys = tasksQueues.keySet().toArray(new Long[]{});
            if (keys.length == 0) {
                return;
            }
            key = keys[random.nextInt(keys.length)];
        }

        if (tasksQueues.getOrDefault(key, emptyDeque).isEmpty()) {
            return;
        }

        var counter = inProgress.get(key);
        if (counter == null) {
            return;
        }
        counter.incrementAndGet();

        while (true) {
            var task = tasksQueues.getOrDefault(key, emptyDeque).pollFirst();
            if (task == null) {
                counter.decrementAndGet();
                return;
            }
            task.start();
        }
    }

    public void waitForFinish(long targetOperation) {
        while (true) {
            AtomicInteger counter = inProgress.get(targetOperation);
            if ((counter == null || counter.get() == 0) && tasksQueues.getOrDefault(targetOperation, emptyDeque).isEmpty()) {
                return;
            }
            Thread.yield();
        }
    }

    public <T> T performWithHelpIfNeeded(
            Supplier<T> function,
            boolean isRead
    ) {
        int operationType = isRead ? 0 : 1;
        while (true) {
            SemanticLockGlobalLock.OperationRequest operationRequest = new SemanticLockGlobalLock.OperationRequest(
                    Thread.currentThread().threadId(), operationType
            );
            if (semanticLock.fairness) {
                semanticLock.fairnessQueue.add(operationRequest);
            }
            if (semanticLock.tryLock(operationRequest)) {
                try {
                    return function.get();
                } finally {
                    semanticLock.unlock(operationType);
                }
            } else {
                this.helpIfNeeded();
                Thread.yield();
            }
        }
    }

    public void performWithHelpIfNeeded(
            Runnable function,
            boolean isRead
    ) {
        int operationType = isRead ? 0 : 1;
        while (true) {
            SemanticLockGlobalLock.OperationRequest operationRequest = new SemanticLockGlobalLock.OperationRequest(
                    Thread.currentThread().threadId(), operationType
            );
            if (semanticLock.fairness) {
                semanticLock.fairnessQueue.add(operationRequest);
            }
            if (semanticLock.tryLock(operationRequest)) {
                try {
                    function.run();
                    return;
                } finally {
                    semanticLock.unlock(operationType);
                }
            } else {
                this.helpIfNeeded();
                Thread.yield();
            }
        }
    }
}
