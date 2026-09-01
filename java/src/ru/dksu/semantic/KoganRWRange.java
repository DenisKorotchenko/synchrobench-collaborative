package ru.dksu.semantic;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * A reader-writer range lock based on the lock-free ordered list described in
 * "Scalable Range Locks for Scalable Address Spaces and Beyond" by Kogan,
 * Dice, and Issa (EuroSys 2020).
 *
 * <p>Ranges are half-open: {@code [from, to)}. Overlapping readers may hold
 * the lock concurrently. A writer conflicts with every overlapping reader or
 * writer, while disjoint ranges never conflict.</p>
 *
 * <p>This is the basic reader-preferred algorithm from Listings 2 and 3 of the
 * paper. It deliberately does not include the optional fairness mechanism or
 * the single-owner fast path described in later sections.</p>
 */
public final class KoganRWRange {
    public enum Mode {
        READ,
        WRITE
    }

    /**
     * The acquisition token. It contains a direct reference to the published
     * list node, which makes release an O(1) logical deletion.
     */
    public static final class LockHandle {
        private final KoganRWRange owner;
        private final Node node;

        private LockHandle(KoganRWRange owner, Node node) {
            this.owner = owner;
            this.node = node;
        }

        public int from() {
            return node.from;
        }

        public int to() {
            return node.to;
        }

        public Mode mode() {
            return node.reader ? Mode.READ : Mode.WRITE;
        }
    }

    private static final int CURRENT_BEFORE_LOCK = -1;
    private static final int CONFLICT = 0;
    private static final int LOCK_BEFORE_CURRENT = 1;
    private static final int YIELD_MASK = 0xff;

    /**
     * A mark on {@code next} means that this node is logically deleted.
     * AtomicMarkableReference is the Java equivalent of the marked pointer
     * used by the paper. Java GC removes the need for its reclamation scheme.
     */
    private static final class Node {
        private final int from;
        private final int to;
        private final boolean reader;
        private volatile boolean deleted;
        private final AtomicMarkableReference<Node> next =
                new AtomicMarkableReference<>(null, false);

        private Node(int from, int to, boolean reader) {
            this.from = from;
            this.to = to;
            this.reader = reader;
        }
    }

    private final AtomicMarkableReference<Node> head =
            new AtomicMarkableReference<>(null, false);

    public LockHandle readLock(int from, int to) {
        return lock(from, to, Mode.READ);
    }

    public LockHandle writeLock(int from, int to) {
        return lock(from, to, Mode.WRITE);
    }

    public LockHandle lock(int from, int to, Mode mode) {
        checkRange(from, to);
        Objects.requireNonNull(mode, "mode");

        boolean reader = mode == Mode.READ;
        while (true) {
            Node node = new Node(from, to, reader);
            if (insertAndValidate(node)) {
                return new LockHandle(this, node);
            }

            // Only writer validation can fail. The rejected node has already
            // been marked as deleted, so a retry must publish a fresh node.
        }
    }

    /**
     * Logically deletes the node. Physical unlinking is performed by threads
     * traversing the list, as in the algorithm from the paper.
     */
    public void unlock(LockHandle handle) {
        Objects.requireNonNull(handle, "handle");
        if (handle.owner != this) {
            throw new IllegalArgumentException("The handle belongs to another range lock");
        }

        markDeleted(handle.node);
    }

    private boolean insertAndValidate(Node lock) {
        boolean[] markHolder = new boolean[1];

        retry:
        while (true) {
            AtomicMarkableReference<Node> previousLink = head;

            while (true) {
                Node current = previousLink.get(markHolder);
                if (markHolder[0]) {
                    // The predecessor disappeared, so previousLink is no
                    // longer a safe insertion point.
                    continue retry;
                }

                if (current != null) {
                    Node next = current.next.get(markHolder);
                    if (markHolder[0]) {
                        if (!previousLink.compareAndSet(current, next, false, false)) {
                            continue retry;
                        }
                        continue;
                    }
                }

                int comparison = compare(current, lock);
                if (comparison == CURRENT_BEFORE_LOCK) {
                    previousLink = current.next;
                    continue;
                }

                if (comparison == CONFLICT) {
                    awaitDeletion(current);
                    continue;
                }

                lock.next.set(current, false);
                if (!previousLink.compareAndSet(current, lock, false, false)) {
                    continue;
                }

                return lock.reader ? validateReader(lock) : validateWriter(lock);
            }
        }
    }

    /**
     * Orders non-overlapping nodes by their start and overlapping readers by
     * their start. Incompatible overlaps are deliberately not ordered: the
     * incoming node waits for the current owner to leave.
     */
    private static int compare(Node current, Node lock) {
        if (current == null) {
            return LOCK_BEFORE_CURRENT;
        }

        int readers = (current.reader ? 1 : 0) + (lock.reader ? 1 : 0);
        if (lock.from >= current.to) {
            return CURRENT_BEFORE_LOCK;
        }
        if (readers == 2 && lock.from >= current.from) {
            return CURRENT_BEFORE_LOCK;
        }
        if (current.from >= lock.to) {
            return LOCK_BEFORE_CURRENT;
        }
        if (readers == 2 && current.from >= lock.from) {
            return LOCK_BEFORE_CURRENT;
        }
        return CONFLICT;
    }

    /**
     * Reader validation from Listing 3. A reader scans its successors because
     * a concurrent writer may have been inserted farther down the list. The
     * reader wins such a race and waits for that writer to delete itself.
     */
    private static boolean validateReader(Node lock) {
        AtomicMarkableReference<Node> previousLink = lock.next;
        boolean[] markHolder = new boolean[1];

        while (true) {
            Node current = previousLink.get(markHolder);
            if (markHolder[0]) {
                previousLink = lock.next;
                continue;
            }
            if (current == null || current.from >= lock.to) {
                return true;
            }

            Node next = current.next.get(markHolder);
            if (markHolder[0]) {
                if (!previousLink.compareAndSet(current, next, false, false)) {
                    previousLink = lock.next;
                }
                continue;
            }

            if (current.reader || current.to <= lock.from) {
                previousLink = current.next;
                continue;
            }

            awaitDeletion(current);
        }
    }

    /**
     * Writer validation from Listing 3. A writer re-scans from the head to
     * find readers that raced into an earlier list position. On such a race,
     * the writer removes itself and retries the complete acquisition.
     */
    private boolean validateWriter(Node lock) {
        AtomicMarkableReference<Node> previousLink = head;
        boolean[] markHolder = new boolean[1];

        while (true) {
            Node current = previousLink.get(markHolder);
            if (markHolder[0]) {
                previousLink = head;
                continue;
            }
            if (current == lock) {
                return true;
            }

            if (current == null) {
                throw new IllegalStateException("Published writer is missing from the list");
            }

            Node next = current.next.get(markHolder);
            if (markHolder[0]) {
                if (!previousLink.compareAndSet(current, next, false, false)) {
                    previousLink = head;
                }
                continue;
            }

            if (current.to <= lock.from) {
                previousLink = current.next;
                continue;
            }

            // The overlapping predecessor is necessarily a reader: insertion
            // itself waits for every overlapping writer.
            markDeleted(lock);
            return false;
        }
    }

    private static void awaitDeletion(Node node) {
        int spins = 0;
        while (!node.deleted) {
            if ((++spins & YIELD_MASK) == 0) {
                Thread.onSpinWait();
                Thread.yield();
            }
        }
    }

    private static void markDeleted(Node node) {
        if (node.deleted) {
            return;
        }

        while (true) {
            Node next = node.next.getReference();
            if (node.next.attemptMark(next, true)) {
                // Publish the wait-friendly mirror only after the marked link:
                // observing deleted == true therefore also observes deletion.
                node.deleted = true;
                return;
            }
        }
    }

    private static void checkRange(int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException("from must be less than or equal to to");
        }
    }
}
