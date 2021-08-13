//package com.okami.test.aqs;
//
//import java.lang.invoke.MethodHandles;
//import java.lang.invoke.VarHandle;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Date;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.AbstractOwnableSynchronizer;
//import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.LockSupport;
//
//
///**
// * 提供一个框架，用于实现依赖先进先出 (FIFO) 等待队列的阻塞锁和相关同步器（信号量、事件等）。
// * 此类旨在成为大多数依赖单个原子int值来表示状态的同步器的有用基础。
// * 子类必须定义更改此状态的受保护方法，并定义该状态在获取或释放此对象方面的含义。
// * 鉴于这些，此类中的其他方法执行所有排队和阻塞机制。
// * 子类可以维护其他状态字段，但只有使用方法getState 、 setState和compareAndSetState操作的原子更新的int值才会在同步方面进行跟踪。
// * 子类应定义为非公共内部帮助类，用于实现其封闭类的同步属性。
// * AbstractQueuedSynchronizer类不实现任何同步接口。
// * 相反，它定义了诸如acquireInterruptibly方法，这些方法可以由具体锁和相关同步器适当调用以实现它们的公共方法。
// * 此类支持默认独占模式和共享模式中的一种或两种。
// * 当以独占模式获取时，其他线程尝试获取不会成功。 多个线程获取的共享模式可能（但不一定）成功。
// *
// * 这个类不“理解”这些差异，除了机械意义上的区别，当共享模式获取成功时，下一个等待线程（如果存在）也必须确定它是否也可以获取。
// * 在不同模式下等待的线程共享同一个 FIFO 队列。
// * 通常，实现子类仅支持这些模式中的一种，但两种模式都可以发挥作用，例如在ReadWriteLock 。
// * 仅支持独占或仅共享模式的子类不需要定义支持未使用模式的方法。
// * 该类定义了一个嵌套的AbstractQueuedSynchronizer.ConditionObject类，该类可以被支持独占模式的子类用作Condition实现，
// *  其中方法isHeldExclusively报告是否针对当前线程独占同步，
// *  使用当前getState值调用的方法release完全释放此对象，并acquire ，给定这个保存的状态值，最终将此对象恢复到其先前获取的状态。
// * 没有AbstractQueuedSynchronizer方法否则会创建这样的条件，因此如果无法满足此约束，请不要使用它。
// *
// * AbstractQueuedSynchronizer.ConditionObject的行为当然取决于其同步器实现的语义。
// * 此类为内部队列提供检查、检测和监视方法，以及为条件对象提供类似方法。
// * 这些可以根据需要使用AbstractQueuedSynchronizer的同步机制导出到类中。
// * 此类的序列化仅存储底层原子整数维护状态，因此反序列化的对象具有空线程队列。
// * 需要可序列化的典型子类将定义一个readObject方法，该方法在反序列化时将其恢复到已知的初始状态。
// * 用法
// * 要将此类用作同步器的基础，请根据适用情况重新定义以下方法，
// * 方法是使用getState 、 setState和/或compareAndSetState检查和/或修改同步状态：
// *      tryAcquire
// *      tryRelease
// *      tryAcquireShared
// *      tryReleaseShared
// *      isHeldExclusively
// *
// * 默认情况下，这些方法中的每一个都会抛出UnsupportedOperationException 。
// * 这些方法的实现必须是内部线程安全的，并且通常应该是简短的而不是阻塞的。
// * 定义这些方法是使用此类的唯一支持方式。 所有其他方法都被声明为final因为它们不能独立变化。
// *
// * 您可能还会发现从AbstractOwnableSynchronizer继承的方法对于跟踪拥有独占同步器的线程很有用。
// * 鼓励您使用它们——这使监视和诊断工具能够帮助用户确定哪些线程持有锁。
// *
// * 即使此类基于内部 FIFO 队列，它也不会自动执行 FIFO 采集策略。 独占同步的核心形式为：
// *    Acquire:
// *        while (!tryAcquire(arg)) {
// *           enqueue thread if it is not already queued;
// *           possibly block current thread;
// *        }
// *
// *    Release:
// *        if (tryRelease(arg))
// *           unblock the first queued thread;
// *
// * （共享模式类似，但可能涉及级联信号。）
// *
// * 因为在入队之前调用获取中的检查，所以新的获取线程可能会抢在其他被阻塞和排队的线程之前。
// * 但是，如果需要，您可以定义tryAcquire和/或tryAcquireShared以通过内部调用一种或多种检查方法来禁用插入，从而提供公平的FIFO 获取顺序。
// * 特别是，如果hasQueuedPredecessors （一种专门设计用于公平同步器使用的方法）返回true ，则大多数公平同步器可以定义tryAcquire以返回false 。 其他变化也是可能的。
// * 默认插入（也称为greedy 、 renouncement和convoy-avoidance ）策略的吞吐量和可扩展性通常最高。
// * 虽然这不能保证公平或无饥饿，但允许较早的排队线程在较晚的排队线程之前重新竞争，并且每次重新竞争都有机会成功对抗传入的线程。
// * 此外，虽然获取不会在通常意义上“旋转”，但它们可能会在阻塞之前执行多次调用tryAcquire穿插其他计算。
// * 当仅短暂保持独占同步时，这提供了自旋的大部分好处，而在不保持时则没有大部分责任。
// * 如果需要，您可以通过使用“快速路径”检查预先调用获取方法来增强这一点，可能预先检查hasContended和/或hasQueuedThreads以仅在同步器可能不竞争时才这样做。
// * 此类通过将其使用范围专门用于可以依赖int状态、获取和释放参数以及内部 FIFO 等待队列的同步器，为同步提供了高效且可扩展的基础。
// * 如果这还不够，您可以使用atomic类、您自己的自定义java.util.Queue类和LockSupport阻塞支持从较低级别构建同步器。
// *
// * 使用示例
// * 这里是一个不可重入的互斥锁类，它使用值 0 表示解锁状态，使用值 1 表示锁定状态。
// * 虽然不可重入锁并不严格要求记录当前所有者线程，但这个类无论如何这样做是为了使使用更容易监控。
// * 它还支持条件并公开一些检测方法：
// *    class Mutex implements Lock, java.io.Serializable {
// *
// *     // Our internal helper class
// *     private static class Sync extends AbstractQueuedSynchronizer {
// *       // Acquires the lock if state is zero
// *       public boolean tryAcquire(int acquires) {
// *         assert acquires == 1; // Otherwise unused
// *         if (compareAndSetState(0, 1)) {
// *           setExclusiveOwnerThread(Thread.currentThread());
// *           return true;
// *         }
// *         return false;
// *       }
// *
// *       // Releases the lock by setting state to zero
// *       protected boolean tryRelease(int releases) {
// *         assert releases == 1; // Otherwise unused
// *         if (!isHeldExclusively())
// *           throw new IllegalMonitorStateException();
// *         setExclusiveOwnerThread(null);
// *         setState(0);
// *         return true;
// *       }
// *
// *       // Reports whether in locked state
// *       public boolean isLocked() {
// *         return getState() != 0;
// *       }
// *
// *       public boolean isHeldExclusively() {
// *         // a data race, but safe due to out-of-thin-air guarantees
// *         return getExclusiveOwnerThread() == Thread.currentThread();
// *       }
// *
// *       // Provides a Condition
// *       public Condition newCondition() {
// *         return new ConditionObject();
// *       }
// *
// *       // Deserializes properly
// *       private void readObject(ObjectInputStream s)
// *           throws IOException, ClassNotFoundException {
// *         s.defaultReadObject();
// *         setState(0); // reset to unlocked state
// *       }
// *     }
// *
// *     // The sync object does all the hard work. We just forward to it.
// *     private final Sync sync = new Sync();
// *
// *     public void lock()              { sync.acquire(1); }
// *     public boolean tryLock()        { return sync.tryAcquire(1); }
// *     public void unlock()            { sync.release(1); }
// *     public Condition newCondition() { return sync.newCondition(); }
// *     public boolean isLocked()       { return sync.isLocked(); }
// *     public boolean isHeldByCurrentThread() {
// *       return sync.isHeldExclusively();
// *     }
// *     public boolean hasQueuedThreads() {
// *       return sync.hasQueuedThreads();
// *     }
// *     public void lockInterruptibly() throws InterruptedException {
// *       sync.acquireInterruptibly(1);
// *     }
// *     public boolean tryLock(long timeout, TimeUnit unit)
// *         throws InterruptedException {
// *       return sync.tryAcquireNanos(1, unit.toNanos(timeout));
// *     }
// *   }
// * 这是一个类似于CountDownLatch的闩锁类，只是它只需要一个signal即可触发。 因为锁存器是非独占的，所以它使用shared获取和释放方法。
// *    class BooleanLatch {
// *
// *     private static class Sync extends AbstractQueuedSynchronizer {
// *       boolean isSignalled() { return getState() != 0; }
// *
// *       protected int tryAcquireShared(int ignore) {
// *         return isSignalled() ? 1 : -1;
// *       }
// *
// *       protected boolean tryReleaseShared(int ignore) {
// *         setState(1);
// *         return true;
// *       }
// *     }
// *
// *     private final Sync sync = new Sync();
// *     public boolean isSignalled() { return sync.isSignalled(); }
// *     public void signal()         { sync.releaseShared(1); }
// *     public void await() throws InterruptedException {
// *       sync.acquireSharedInterruptibly(1);
// *     }
// *   }
// */
//public abstract class AbstractQueuedSynchronizer
//        extends AbstractOwnableSynchronizer
//        implements java.io.Serializable {
//
//    /**
//     * The number of nanoseconds for which it is faster to spin
//     * rather than to use timed park. A rough estimate suffices
//     * to improve responsiveness with very short timeouts.
//     */
//    static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1000L;
//    private static final long serialVersionUID = 7373984972572414691L;
//    // VarHandle mechanics
//    private static final VarHandle STATE;
//    private static final VarHandle HEAD;
//    private static final VarHandle TAIL;
//
//    static {
//        try {
//            MethodHandles.Lookup l = MethodHandles.lookup();
//            STATE = l.findVarHandle(AbstractQueuedSynchronizer.class, "state", int.class);
//            HEAD = l.findVarHandle(AbstractQueuedSynchronizer.class, "head", Node.class);
//            TAIL = l.findVarHandle(AbstractQueuedSynchronizer.class, "tail", Node.class);
//        } catch (ReflectiveOperationException e) {
//            throw new ExceptionInInitializerError(e);
//        }
//
//        // Reduce the risk of rare disastrous classloading in first call to
//        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
//        Class<?> ensureLoaded = LockSupport.class;
//    }
//
//    /**
//     * Head of the wait queue, lazily initialized.  Except for
//     * initialization, it is modified only via method setHead.  Note:
//     * If head exists, its waitStatus is guaranteed not to be
//     * CANCELLED.
//     */
//    private transient volatile Node head;
//    /**
//     * Tail of the wait queue, lazily initialized.  Modified only via
//     * method enq to add new wait node.
//     */
//    private transient volatile Node tail;
//    /**
//     * The synchronization state.
//     */
//    private volatile int state;
//
//    // Queuing utilities
//
//    protected AbstractQueuedSynchronizer() {
//    }
//
//    /**
//     * Checks and updates status for a node that failed to acquire.
//     * Returns true if thread should block. This is the main signal
//     * control in all acquire loops.  Requires that pred == node.prev.
//     *
//     * @param pred node's predecessor holding status
//     * @param node the node
//     * @return {@code true} if thread should block
//     */
//    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
//        int ws = pred.waitStatus;
//        if (ws == Node.SIGNAL)
//            /*
//             * This node has already set status asking a release
//             * to signal it, so it can safely park.
//             */
//            return true;
//        if (ws > 0) {
//            /*
//             * Predecessor was cancelled. Skip over predecessors and
//             * indicate retry.
//             */
//            do {
//                node.prev = pred = pred.prev;
//            } while (pred.waitStatus > 0);
//            pred.next = node;
//        } else {
//            /*
//             * waitStatus must be 0 or PROPAGATE.  Indicate that we
//             * need a signal, but don't park yet.  Caller will need to
//             * retry to make sure it cannot acquire before parking.
//             */
//            pred.compareAndSetWaitStatus(ws, Node.SIGNAL);
//        }
//        return false;
//    }
//
//    /**
//     * Convenience method to interrupt current thread.
//     */
//    static void selfInterrupt() {
//        Thread.currentThread().interrupt();
//    }
//
//    /**
//     * Returns the current value of synchronization state.
//     * This operation has memory semantics of a {@code volatile} read.
//     *
//     * @return current state value
//     */
//    protected final int getState() {
//        return state;
//    }
//
//    /**
//     * Sets the value of synchronization state.
//     * This operation has memory semantics of a {@code volatile} write.
//     *
//     * @param newState the new state value
//     */
//    protected final void setState(int newState) {
//        state = newState;
//    }
//
//    /**
//     * Atomically sets synchronization state to the given updated
//     * value if the current state value equals the expected value.
//     * This operation has memory semantics of a {@code volatile} read
//     * and write.
//     *
//     * @param expect the expected value
//     * @param update the new value
//     * @return {@code true} if successful. False return indicates that the actual
//     * value was not equal to the expected value.
//     */
//    protected final boolean compareAndSetState(int expect, int update) {
//        return STATE.compareAndSet(this, expect, update);
//    }
//
//    /**
//     * Inserts node into queue, initializing if necessary. See picture above.
//     *
//     * @param node the node to insert
//     * @return node's predecessor
//     */
//    private Node enq(Node node) {
//        for (; ; ) {
//            Node oldTail = tail;
//            if (oldTail != null) {
//                node.setPrevRelaxed(oldTail);
//                if (compareAndSetTail(oldTail, node)) {
//                    oldTail.next = node;
//                    return oldTail;
//                }
//            } else {
//                initializeSyncQueue();
//            }
//        }
//    }
//
//    // Utilities for various versions of acquire
//
//    /**
//     * Creates and enqueues node for current thread and given mode.
//     *
//     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
//     * @return the new node
//     */
//    private Node addWaiter(Node mode) {
//        Node node = new Node(mode);
//
//        for (; ; ) {
//            Node oldTail = tail;
//            if (oldTail != null) {
//                node.setPrevRelaxed(oldTail);
//                if (compareAndSetTail(oldTail, node)) {
//                    oldTail.next = node;
//                    return node;
//                }
//            } else {
//                initializeSyncQueue();
//            }
//        }
//    }
//
//    /**
//     * Sets head of queue to be node, thus dequeuing. Called only by
//     * acquire methods.  Also nulls out unused fields for sake of GC
//     * and to suppress unnecessary signals and traversals.
//     *
//     * @param node the node
//     */
//    private void setHead(Node node) {
//        head = node;
//        node.thread = null;
//        node.prev = null;
//    }
//
//    /**
//     * Wakes up node's successor, if one exists.
//     *
//     * @param node the node
//     */
//    private void unparkSuccessor(Node node) {
//        /*
//         * If status is negative (i.e., possibly needing signal) try
//         * to clear in anticipation of signalling.  It is OK if this
//         * fails or if status is changed by waiting thread.
//         */
//        int ws = node.waitStatus;
//        if (ws < 0)
//            node.compareAndSetWaitStatus(ws, 0);
//
//        /*
//         * Thread to unpark is held in successor, which is normally
//         * just the next node.  But if cancelled or apparently null,
//         * traverse backwards from tail to find the actual
//         * non-cancelled successor.
//         */
//        Node s = node.next;
//        if (s == null || s.waitStatus > 0) {
//            s = null;
//            for (Node p = tail; p != node && p != null; p = p.prev)
//                if (p.waitStatus <= 0)
//                    s = p;
//        }
//        if (s != null)
//            LockSupport.unpark(s.thread);
//    }
//
//    /**
//     * Release action for shared mode -- signals successor and ensures
//     * propagation. (Note: For exclusive mode, release just amounts
//     * to calling unparkSuccessor of head if it needs signal.)
//     */
//    private void doReleaseShared() {
//        /*
//         * Ensure that a release propagates, even if there are other
//         * in-progress acquires/releases.  This proceeds in the usual
//         * way of trying to unparkSuccessor of head if it needs
//         * signal. But if it does not, status is set to PROPAGATE to
//         * ensure that upon release, propagation continues.
//         * Additionally, we must loop in case a new node is added
//         * while we are doing this. Also, unlike other uses of
//         * unparkSuccessor, we need to know if CAS to reset status
//         * fails, if so rechecking.
//         */
//        for (; ; ) {
//            Node h = head;
//            if (h != null && h != tail) {
//                int ws = h.waitStatus;
//                if (ws == Node.SIGNAL) {
//                    if (!h.compareAndSetWaitStatus(Node.SIGNAL, 0))
//                        continue;            // loop to recheck cases
//                    unparkSuccessor(h);
//                } else if (ws == 0 &&
//                        !h.compareAndSetWaitStatus(0, Node.PROPAGATE))
//                    continue;                // loop on failed CAS
//            }
//            if (h == head)                   // loop if head changed
//                break;
//        }
//    }
//
//    /*
//     * Various flavors of acquire, varying in exclusive/shared and
//     * control modes.  Each is mostly the same, but annoyingly
//     * different.  Only a little bit of factoring is possible due to
//     * interactions of exception mechanics (including ensuring that we
//     * cancel if tryAcquire throws exception) and other control, at
//     * least not without hurting performance too much.
//     */
//
//    /**
//     * Sets head of queue, and checks if successor may be waiting
//     * in shared mode, if so propagating if either propagate > 0 or
//     * PROPAGATE status was set.
//     *
//     * @param node      the node
//     * @param propagate the return value from a tryAcquireShared
//     */
//    private void setHeadAndPropagate(Node node, int propagate) {
//        Node h = head; // Record old head for check below
//        setHead(node);
//        /*
//         * Try to signal next queued node if:
//         *   Propagation was indicated by caller,
//         *     or was recorded (as h.waitStatus either before
//         *     or after setHead) by a previous operation
//         *     (note: this uses sign-check of waitStatus because
//         *      PROPAGATE status may transition to SIGNAL.)
//         * and
//         *   The next node is waiting in shared mode,
//         *     or we don't know, because it appears null
//         *
//         * The conservatism in both of these checks may cause
//         * unnecessary wake-ups, but only when there are multiple
//         * racing acquires/releases, so most need signals now or soon
//         * anyway.
//         */
//        if (propagate > 0 || h == null || h.waitStatus < 0 ||
//                (h = head) == null || h.waitStatus < 0) {
//            Node s = node.next;
//            if (s == null || s.isShared())
//                doReleaseShared();
//        }
//    }
//
//    /**
//     * Cancels an ongoing attempt to acquire.
//     *
//     * @param node the node
//     */
//    private void cancelAcquire(Node node) {
//        // Ignore if node doesn't exist
//        if (node == null)
//            return;
//
//        node.thread = null;
//
//        // Skip cancelled predecessors
//        Node pred = node.prev;
//        while (pred.waitStatus > 0)
//            node.prev = pred = pred.prev;
//
//        // predNext is the apparent node to unsplice. CASes below will
//        // fail if not, in which case, we lost race vs another cancel
//        // or signal, so no further action is necessary, although with
//        // a possibility that a cancelled node may transiently remain
//        // reachable.
//        Node predNext = pred.next;
//
//        // Can use unconditional write instead of CAS here.
//        // After this atomic step, other Nodes can skip past us.
//        // Before, we are free of interference from other threads.
//        node.waitStatus = Node.CANCELLED;
//
//        // If we are the tail, remove ourselves.
//        if (node == tail && compareAndSetTail(node, pred)) {
//            pred.compareAndSetNext(predNext, null);
//        } else {
//            // If successor needs signal, try to set pred's next-link
//            // so it will get one. Otherwise wake it up to propagate.
//            int ws;
//            if (pred != head &&
//                    ((ws = pred.waitStatus) == Node.SIGNAL ||
//                            (ws <= 0 && pred.compareAndSetWaitStatus(ws, Node.SIGNAL))) &&
//                    pred.thread != null) {
//                Node next = node.next;
//                if (next != null && next.waitStatus <= 0)
//                    pred.compareAndSetNext(predNext, next);
//            } else {
//                unparkSuccessor(node);
//            }
//
//            node.next = node; // help GC
//        }
//    }
//
//    /**
//     * Convenience method to park and then check if interrupted.
//     *
//     * @return {@code true} if interrupted
//     */
//    private final boolean parkAndCheckInterrupt() {
//        LockSupport.park(this);
//        return Thread.interrupted();
//    }
//
//    /**
//     * Acquires in exclusive uninterruptible mode for thread already in
//     * queue. Used by condition wait methods as well as acquire.
//     *
//     * @param node the node
//     * @param arg  the acquire argument
//     * @return {@code true} if interrupted while waiting
//     */
//    final boolean acquireQueued(final Node node, int arg) {
//        boolean interrupted = false;
//        try {
//            for (; ; ) {
//                final Node p = node.predecessor();
//                if (p == head && tryAcquire(arg)) {
//                    setHead(node);
//                    p.next = null; // help GC
//                    return interrupted;
//                }
//                if (shouldParkAfterFailedAcquire(p, node))
//                    interrupted |= parkAndCheckInterrupt();
//            }
//        } catch (Throwable t) {
//            cancelAcquire(node);
//            if (interrupted)
//                selfInterrupt();
//            throw t;
//        }
//    }
//
//    /**
//     * Acquires in exclusive interruptible mode.
//     *
//     * @param arg the acquire argument
//     */
//    private void doAcquireInterruptibly(int arg)
//            throws InterruptedException {
//        final Node node = addWaiter(Node.EXCLUSIVE);
//        try {
//            for (; ; ) {
//                final Node p = node.predecessor();
//                if (p == head && tryAcquire(arg)) {
//                    setHead(node);
//                    p.next = null; // help GC
//                    return;
//                }
//                if (shouldParkAfterFailedAcquire(p, node) &&
//                        parkAndCheckInterrupt())
//                    throw new InterruptedException();
//            }
//        } catch (Throwable t) {
//            cancelAcquire(node);
//            throw t;
//        }
//    }
//
//    /**
//     * Acquires in exclusive timed mode.
//     *
//     * @param arg          the acquire argument
//     * @param nanosTimeout max wait time
//     * @return {@code true} if acquired
//     */
//    private boolean doAcquireNanos(int arg, long nanosTimeout)
//            throws InterruptedException {
//        if (nanosTimeout <= 0L)
//            return false;
//        final long deadline = System.nanoTime() + nanosTimeout;
//        final Node node = addWaiter(Node.EXCLUSIVE);
//        try {
//            for (; ; ) {
//                final Node p = node.predecessor();
//                if (p == head && tryAcquire(arg)) {
//                    setHead(node);
//                    p.next = null; // help GC
//                    return true;
//                }
//                nanosTimeout = deadline - System.nanoTime();
//                if (nanosTimeout <= 0L) {
//                    cancelAcquire(node);
//                    return false;
//                }
//                if (shouldParkAfterFailedAcquire(p, node) &&
//                        nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD)
//                    LockSupport.parkNanos(this, nanosTimeout);
//                if (Thread.interrupted())
//                    throw new InterruptedException();
//            }
//        } catch (Throwable t) {
//            cancelAcquire(node);
//            throw t;
//        }
//    }
//
//    // Main exported methods
//
//    /**
//     * Acquires in shared uninterruptible mode.
//     *
//     * @param arg the acquire argument
//     */
//    private void doAcquireShared(int arg) {
//        final Node node = addWaiter(Node.SHARED);
//        boolean interrupted = false;
//        try {
//            for (; ; ) {
//                final Node p = node.predecessor();
//                if (p == head) {
//                    int r = tryAcquireShared(arg);
//                    if (r >= 0) {
//                        setHeadAndPropagate(node, r);
//                        p.next = null; // help GC
//                        return;
//                    }
//                }
//                if (shouldParkAfterFailedAcquire(p, node))
//                    interrupted |= parkAndCheckInterrupt();
//            }
//        } catch (Throwable t) {
//            cancelAcquire(node);
//            throw t;
//        } finally {
//            if (interrupted)
//                selfInterrupt();
//        }
//    }
//
//    /**
//     * Acquires in shared interruptible mode.
//     *
//     * @param arg the acquire argument
//     */
//    private void doAcquireSharedInterruptibly(int arg)
//            throws InterruptedException {
//        final Node node = addWaiter(Node.SHARED);
//        try {
//            for (; ; ) {
//                final Node p = node.predecessor();
//                if (p == head) {
//                    int r = tryAcquireShared(arg);
//                    if (r >= 0) {
//                        setHeadAndPropagate(node, r);
//                        p.next = null; // help GC
//                        return;
//                    }
//                }
//                if (shouldParkAfterFailedAcquire(p, node) &&
//                        parkAndCheckInterrupt())
//                    throw new InterruptedException();
//            }
//        } catch (Throwable t) {
//            cancelAcquire(node);
//            throw t;
//        }
//    }
//
//    /**
//     * Acquires in shared timed mode.
//     *
//     * @param arg          the acquire argument
//     * @param nanosTimeout max wait time
//     * @return {@code true} if acquired
//     */
//    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
//            throws InterruptedException {
//        if (nanosTimeout <= 0L)
//            return false;
//        final long deadline = System.nanoTime() + nanosTimeout;
//        final Node node = addWaiter(Node.SHARED);
//        try {
//            for (; ; ) {
//                final Node p = node.predecessor();
//                if (p == head) {
//                    int r = tryAcquireShared(arg);
//                    if (r >= 0) {
//                        setHeadAndPropagate(node, r);
//                        p.next = null; // help GC
//                        return true;
//                    }
//                }
//                nanosTimeout = deadline - System.nanoTime();
//                if (nanosTimeout <= 0L) {
//                    cancelAcquire(node);
//                    return false;
//                }
//                if (shouldParkAfterFailedAcquire(p, node) &&
//                        nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD)
//                    LockSupport.parkNanos(this, nanosTimeout);
//                if (Thread.interrupted())
//                    throw new InterruptedException();
//            }
//        } catch (Throwable t) {
//            cancelAcquire(node);
//            throw t;
//        }
//    }
//
//    /**
//     * Attempts to acquire in exclusive mode. This method should query
//     * if the state of the object permits it to be acquired in the
//     * exclusive mode, and if so to acquire it.
//     *
//     * <p>This method is always invoked by the thread performing
//     * acquire.  If this method reports failure, the acquire method
//     * may queue the thread, if it is not already queued, until it is
//     * signalled by a release from some other thread. This can be used
//     * to implement method {@link Lock#tryLock()}.
//     *
//     * <p>The default
//     * implementation throws {@link UnsupportedOperationException}.
//     *
//     * @param arg the acquire argument. This value is always the one
//     *            passed to an acquire method, or is the value saved on entry
//     *            to a condition wait.  The value is otherwise uninterpreted
//     *            and can represent anything you like.
//     * @return {@code true} if successful. Upon success, this object has
//     * been acquired.
//     * @throws IllegalMonitorStateException  if acquiring would place this
//     *                                       synchronizer in an illegal state. This exception must be
//     *                                       thrown in a consistent fashion for synchronization to work
//     *                                       correctly.
//     * @throws UnsupportedOperationException if exclusive mode is not supported
//     */
//    protected boolean tryAcquire(int arg) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Attempts to set the state to reflect a release in exclusive
//     * mode.
//     *
//     * <p>This method is always invoked by the thread performing release.
//     *
//     * <p>The default implementation throws
//     * {@link UnsupportedOperationException}.
//     *
//     * @param arg the release argument. This value is always the one
//     *            passed to a release method, or the current state value upon
//     *            entry to a condition wait.  The value is otherwise
//     *            uninterpreted and can represent anything you like.
//     * @return {@code true} if this object is now in a fully released
//     * state, so that any waiting threads may attempt to acquire;
//     * and {@code false} otherwise.
//     * @throws IllegalMonitorStateException  if releasing would place this
//     *                                       synchronizer in an illegal state. This exception must be
//     *                                       thrown in a consistent fashion for synchronization to work
//     *                                       correctly.
//     * @throws UnsupportedOperationException if exclusive mode is not supported
//     */
//    protected boolean tryRelease(int arg) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Attempts to acquire in shared mode. This method should query if
//     * the state of the object permits it to be acquired in the shared
//     * mode, and if so to acquire it.
//     *
//     * <p>This method is always invoked by the thread performing
//     * acquire.  If this method reports failure, the acquire method
//     * may queue the thread, if it is not already queued, until it is
//     * signalled by a release from some other thread.
//     *
//     * <p>The default implementation throws {@link
//     * UnsupportedOperationException}.
//     *
//     * @param arg the acquire argument. This value is always the one
//     *            passed to an acquire method, or is the value saved on entry
//     *            to a condition wait.  The value is otherwise uninterpreted
//     *            and can represent anything you like.
//     * @return a negative value on failure; zero if acquisition in shared
//     * mode succeeded but no subsequent shared-mode acquire can
//     * succeed; and a positive value if acquisition in shared
//     * mode succeeded and subsequent shared-mode acquires might
//     * also succeed, in which case a subsequent waiting thread
//     * must check availability. (Support for three different
//     * return values enables this method to be used in contexts
//     * where acquires only sometimes act exclusively.)  Upon
//     * success, this object has been acquired.
//     * @throws IllegalMonitorStateException  if acquiring would place this
//     *                                       synchronizer in an illegal state. This exception must be
//     *                                       thrown in a consistent fashion for synchronization to work
//     *                                       correctly.
//     * @throws UnsupportedOperationException if shared mode is not supported
//     */
//    protected int tryAcquireShared(int arg) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Attempts to set the state to reflect a release in shared mode.
//     *
//     * <p>This method is always invoked by the thread performing release.
//     *
//     * <p>The default implementation throws
//     * {@link UnsupportedOperationException}.
//     *
//     * @param arg the release argument. This value is always the one
//     *            passed to a release method, or the current state value upon
//     *            entry to a condition wait.  The value is otherwise
//     *            uninterpreted and can represent anything you like.
//     * @return {@code true} if this release of shared mode may permit a
//     * waiting acquire (shared or exclusive) to succeed; and
//     * {@code false} otherwise
//     * @throws IllegalMonitorStateException  if releasing would place this
//     *                                       synchronizer in an illegal state. This exception must be
//     *                                       thrown in a consistent fashion for synchronization to work
//     *                                       correctly.
//     * @throws UnsupportedOperationException if shared mode is not supported
//     */
//    protected boolean tryReleaseShared(int arg) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Returns {@code true} if synchronization is held exclusively with
//     * respect to the current (calling) thread.  This method is invoked
//     * upon each call to a {@link ConditionObject} method.
//     *
//     * <p>The default implementation throws {@link
//     * UnsupportedOperationException}. This method is invoked
//     * internally only within {@link ConditionObject} methods, so need
//     * not be defined if conditions are not used.
//     *
//     * @return {@code true} if synchronization is held exclusively;
//     * {@code false} otherwise
//     * @throws UnsupportedOperationException if conditions are not supported
//     */
//    protected boolean isHeldExclusively() {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Acquires in exclusive mode, ignoring interrupts.  Implemented
//     * by invoking at least once {@link #tryAcquire},
//     * returning on success.  Otherwise the thread is queued, possibly
//     * repeatedly blocking and unblocking, invoking {@link
//     * #tryAcquire} until success.  This method can be used
//     * to implement method {@link Lock#lock}.
//     *
//     * @param arg the acquire argument.  This value is conveyed to
//     *            {@link #tryAcquire} but is otherwise uninterpreted and
//     *            can represent anything you like.
//     */
//    public final void acquire(int arg) {
//        if (!tryAcquire(arg) &&
//                acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
//            selfInterrupt();
//    }
//
//    /**
//     * Acquires in exclusive mode, aborting if interrupted.
//     * Implemented by first checking interrupt status, then invoking
//     * at least once {@link #tryAcquire}, returning on
//     * success.  Otherwise the thread is queued, possibly repeatedly
//     * blocking and unblocking, invoking {@link #tryAcquire}
//     * until success or the thread is interrupted.  This method can be
//     * used to implement method {@link Lock#lockInterruptibly}.
//     *
//     * @param arg the acquire argument.  This value is conveyed to
//     *            {@link #tryAcquire} but is otherwise uninterpreted and
//     *            can represent anything you like.
//     * @throws InterruptedException if the current thread is interrupted
//     */
//    public final void acquireInterruptibly(int arg)
//            throws InterruptedException {
//        if (Thread.interrupted())
//            throw new InterruptedException();
//        if (!tryAcquire(arg))
//            doAcquireInterruptibly(arg);
//    }
//
//    /**
//     * Attempts to acquire in exclusive mode, aborting if interrupted,
//     * and failing if the given timeout elapses.  Implemented by first
//     * checking interrupt status, then invoking at least once {@link
//     * #tryAcquire}, returning on success.  Otherwise, the thread is
//     * queued, possibly repeatedly blocking and unblocking, invoking
//     * {@link #tryAcquire} until success or the thread is interrupted
//     * or the timeout elapses.  This method can be used to implement
//     * method {@link Lock#tryLock(long, TimeUnit)}.
//     *
//     * @param arg          the acquire argument.  This value is conveyed to
//     *                     {@link #tryAcquire} but is otherwise uninterpreted and
//     *                     can represent anything you like.
//     * @param nanosTimeout the maximum number of nanoseconds to wait
//     * @return {@code true} if acquired; {@code false} if timed out
//     * @throws InterruptedException if the current thread is interrupted
//     */
//    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
//            throws InterruptedException {
//        if (Thread.interrupted())
//            throw new InterruptedException();
//        return tryAcquire(arg) ||
//                doAcquireNanos(arg, nanosTimeout);
//    }
//
//    /**
//     * Releases in exclusive mode.  Implemented by unblocking one or
//     * more threads if {@link #tryRelease} returns true.
//     * This method can be used to implement method {@link Lock#unlock}.
//     *
//     * @param arg the release argument.  This value is conveyed to
//     *            {@link #tryRelease} but is otherwise uninterpreted and
//     *            can represent anything you like.
//     * @return the value returned from {@link #tryRelease}
//     */
//    public final boolean release(int arg) {
//        if (tryRelease(arg)) {
//            Node h = head;
//            if (h != null && h.waitStatus != 0)
//                unparkSuccessor(h);
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * Acquires in shared mode, ignoring interrupts.  Implemented by
//     * first invoking at least once {@link #tryAcquireShared},
//     * returning on success.  Otherwise the thread is queued, possibly
//     * repeatedly blocking and unblocking, invoking {@link
//     * #tryAcquireShared} until success.
//     *
//     * @param arg the acquire argument.  This value is conveyed to
//     *            {@link #tryAcquireShared} but is otherwise uninterpreted
//     *            and can represent anything you like.
//     */
//    public final void acquireShared(int arg) {
//        if (tryAcquireShared(arg) < 0)
//            doAcquireShared(arg);
//    }
//
//    // Queue inspection methods
//
//    /**
//     * Acquires in shared mode, aborting if interrupted.  Implemented
//     * by first checking interrupt status, then invoking at least once
//     * {@link #tryAcquireShared}, returning on success.  Otherwise the
//     * thread is queued, possibly repeatedly blocking and unblocking,
//     * invoking {@link #tryAcquireShared} until success or the thread
//     * is interrupted.
//     *
//     * @param arg the acquire argument.
//     *            This value is conveyed to {@link #tryAcquireShared} but is
//     *            otherwise uninterpreted and can represent anything
//     *            you like.
//     * @throws InterruptedException if the current thread is interrupted
//     */
//    public final void acquireSharedInterruptibly(int arg)
//            throws InterruptedException {
//        if (Thread.interrupted())
//            throw new InterruptedException();
//        if (tryAcquireShared(arg) < 0)
//            doAcquireSharedInterruptibly(arg);
//    }
//
//    /**
//     * Attempts to acquire in shared mode, aborting if interrupted, and
//     * failing if the given timeout elapses.  Implemented by first
//     * checking interrupt status, then invoking at least once {@link
//     * #tryAcquireShared}, returning on success.  Otherwise, the
//     * thread is queued, possibly repeatedly blocking and unblocking,
//     * invoking {@link #tryAcquireShared} until success or the thread
//     * is interrupted or the timeout elapses.
//     *
//     * @param arg          the acquire argument.  This value is conveyed to
//     *                     {@link #tryAcquireShared} but is otherwise uninterpreted
//     *                     and can represent anything you like.
//     * @param nanosTimeout the maximum number of nanoseconds to wait
//     * @return {@code true} if acquired; {@code false} if timed out
//     * @throws InterruptedException if the current thread is interrupted
//     */
//    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
//            throws InterruptedException {
//        if (Thread.interrupted())
//            throw new InterruptedException();
//        return tryAcquireShared(arg) >= 0 ||
//                doAcquireSharedNanos(arg, nanosTimeout);
//    }
//
//    /**
//     * Releases in shared mode.  Implemented by unblocking one or more
//     * threads if {@link #tryReleaseShared} returns true.
//     *
//     * @param arg the release argument.  This value is conveyed to
//     *            {@link #tryReleaseShared} but is otherwise uninterpreted
//     *            and can represent anything you like.
//     * @return the value returned from {@link #tryReleaseShared}
//     */
//    public final boolean releaseShared(int arg) {
//        if (tryReleaseShared(arg)) {
//            doReleaseShared();
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * Queries whether any threads are waiting to acquire. Note that
//     * because cancellations due to interrupts and timeouts may occur
//     * at any time, a {@code true} return does not guarantee that any
//     * other thread will ever acquire.
//     *
//     * @return {@code true} if there may be other threads waiting to acquire
//     */
//    public final boolean hasQueuedThreads() {
//        for (Node p = tail, h = head; p != h && p != null; p = p.prev)
//            if (p.waitStatus <= 0)
//                return true;
//        return false;
//    }
//
//    /**
//     * Queries whether any threads have ever contended to acquire this
//     * synchronizer; that is, if an acquire method has ever blocked.
//     *
//     * <p>In this implementation, this operation returns in
//     * constant time.
//     *
//     * @return {@code true} if there has ever been contention
//     */
//    public final boolean hasContended() {
//        return head != null;
//    }
//
//    /**
//     * Returns the first (longest-waiting) thread in the queue, or
//     * {@code null} if no threads are currently queued.
//     *
//     * <p>In this implementation, this operation normally returns in
//     * constant time, but may iterate upon contention if other threads are
//     * concurrently modifying the queue.
//     *
//     * @return the first (longest-waiting) thread in the queue, or
//     * {@code null} if no threads are currently queued
//     */
//    public final Thread getFirstQueuedThread() {
//        // handle only fast path, else relay
//        return (head == tail) ? null : fullGetFirstQueuedThread();
//    }
//
//    /**
//     * Version of getFirstQueuedThread called when fastpath fails.
//     */
//    private Thread fullGetFirstQueuedThread() {
//        /*
//         * The first node is normally head.next. Try to get its
//         * thread field, ensuring consistent reads: If thread
//         * field is nulled out or s.prev is no longer head, then
//         * some other thread(s) concurrently performed setHead in
//         * between some of our reads. We try this twice before
//         * resorting to traversal.
//         */
//        Node h, s;
//        Thread st;
//        if (((h = head) != null && (s = h.next) != null &&
//                s.prev == head && (st = s.thread) != null) ||
//                ((h = head) != null && (s = h.next) != null &&
//                        s.prev == head && (st = s.thread) != null))
//            return st;
//
//        /*
//         * Head's next field might not have been set yet, or may have
//         * been unset after setHead. So we must check to see if tail
//         * is actually first node. If not, we continue on, safely
//         * traversing from tail back to head to find first,
//         * guaranteeing termination.
//         */
//
//        Thread firstThread = null;
//        for (Node p = tail; p != null && p != head; p = p.prev) {
//            Thread t = p.thread;
//            if (t != null)
//                firstThread = t;
//        }
//        return firstThread;
//    }
//
//    // Instrumentation and monitoring methods
//
//    /**
//     * Returns true if the given thread is currently queued.
//     *
//     * <p>This implementation traverses the queue to determine
//     * presence of the given thread.
//     *
//     * @param thread the thread
//     * @return {@code true} if the given thread is on the queue
//     * @throws NullPointerException if the thread is null
//     */
//    public final boolean isQueued(Thread thread) {
//        if (thread == null)
//            throw new NullPointerException();
//        for (Node p = tail; p != null; p = p.prev)
//            if (p.thread == thread)
//                return true;
//        return false;
//    }
//
//    /**
//     * Returns {@code true} if the apparent first queued thread, if one
//     * exists, is waiting in exclusive mode.  If this method returns
//     * {@code true}, and the current thread is attempting to acquire in
//     * shared mode (that is, this method is invoked from {@link
//     * #tryAcquireShared}) then it is guaranteed that the current thread
//     * is not the first queued thread.  Used only as a heuristic in
//     * ReentrantReadWriteLock.
//     */
//    final boolean apparentlyFirstQueuedIsExclusive() {
//        Node h, s;
//        return (h = head) != null &&
//                (s = h.next) != null &&
//                !s.isShared() &&
//                s.thread != null;
//    }
//
//    /**
//     * 查询是否有任何线程等待获取的时间比当前线程长。
//     * 调用此方法等效于（但可能比）：
//     *        getFirstQueuedThread() != Thread.currentThread()
//     *         && hasQueuedThreads()
//     * 需要注意的是，因为取消由于中断和超时可以发生在任何时间，一个true收益并不能保证其他线程将当前线程之前获得。
//     * 同样，有可能另一个线程赢得了比赛，入队后，这个方法返回false ，由于队列为空。
//     * 此方法被设计成由一个公平的同步器被用来避免闯入。
//     * 这种同步的tryAcquire方法应该返回false ，其tryAcquireShared方法应该返回一个负值，如果该方法返回true （除非这是一个折返获取）。
//     * 例如， tryAcquire一个公平，折返，独占模式同步方法可能是这样的：
//     *        protected boolean tryAcquire(int arg) {
//     *         if (isHeldExclusively()) {
//     *           // A reentrant acquire; increment hold count
//     *           return true;
//     *         } else if (hasQueuedPredecessors()) {
//     *           return false;
//     *         } else {
//     *           // try to acquire normally
//     *         }
//     *       }
//     *
//     * @return {@code true} 如果有当前线程前面的线程排队， {@code false}  如果当前线程是在队列的头部或队列为空
//     *
//     * @since 1.7
//     */
//    public final boolean hasQueuedPredecessors() {
//        Node h, s;
//        if ((h = head) != null) {
//            if ((s = h.next) == null || s.waitStatus > 0) {
//                s = null; // traverse in case of concurrent cancellation
//                for (Node p = tail; p != h && p != null; p = p.prev) {
//                    if (p.waitStatus <= 0)
//                        s = p;
//                }
//            }
//            if (s != null && s.thread != Thread.currentThread())
//                return true;
//        }
//        return false;
//    }
//
//    /**
//     * 返回等待获取的线程数的估计值。
//     * 该值只是一个估计值，因为当此方法遍历内部数据结构时，线程数可能会动态变化。
//     * 该方法设计用于监视系统状态，而不是用于同步控制。
//     *
//     * @return the estimated number of threads waiting to acquire
//     */
//    public final int getQueueLength() {
//        int n = 0;
//        for (Node p = tail; p != null; p = p.prev) {
//            if (p.thread != null)
//                ++n;
//        }
//        return n;
//    }
//
//    /**
//     * 返回一个包含可能正在等待获取的线程的集合。
//     * 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
//     * 返回集合的元素没有特定的顺序。
//     * 此方法旨在促进子类的构建，以提供更广泛的监视设施。
//     *
//     * @return the collection of threads
//     */
//    public final Collection<Thread> getQueuedThreads() {
//        ArrayList<Thread> list = new ArrayList<>();
//        for (Node p = tail; p != null; p = p.prev) {
//            Thread t = p.thread;
//            if (t != null)
//                list.add(t);
//        }
//        return list;
//    }
//
//
//    // Internal support methods for Conditions
//
//    /**
//     * 返回一个包含可能在独占模式下等待获取的线程的集合。
//     * 它与getQueuedThreads具有相同的属性，除了它只返回由于独占获取而等待的线程。
//     *
//     * @return the collection of threads
//     */
//    public final Collection<Thread> getExclusiveQueuedThreads() {
//        ArrayList<Thread> list = new ArrayList<>();
//        for (Node p = tail; p != null; p = p.prev) {
//            if (!p.isShared()) {
//                Thread t = p.thread;
//                if (t != null)
//                    list.add(t);
//            }
//        }
//        return list;
//    }
//
//    /**
//     * Returns a collection containing threads that may be waiting to
//     * acquire in shared mode. This has the same properties
//     * as {@link #getQueuedThreads} except that it only returns
//     * those threads waiting due to a shared acquire.
//     *
//     * @return the collection of threads
//     */
//    public final Collection<Thread> getSharedQueuedThreads() {
//        ArrayList<Thread> list = new ArrayList<>();
//        for (Node p = tail; p != null; p = p.prev) {
//            if (p.isShared()) {
//                Thread t = p.thread;
//                if (t != null)
//                    list.add(t);
//            }
//        }
//        return list;
//    }
//
//    /**
//     * 返回标识此同步器及其状态的字符串。
//     * 括号中的状态包括字符串"State ="后跟getState的当前值，
//     * 以及"nonempty"或"empty"具体取决于队列是否为空。
//     *
//     * @return a string identifying this synchronizer, as well as its state
//     */
//    public String toString() {
//        return super.toString()
//                + "[State = " + getState() + ", "
//                + (hasQueuedThreads() ? "non" : "") + "empty queue]";
//    }
//
//    /**
//     * 如果一个节点（始终是最初放置在条件队列中的节点）现在正在等待重新获取同步队列，则返回 true。
//     *
//     * @param node the node
//     * @return true if is reacquiring
//     */
//    final boolean isOnSyncQueue(Node node) {
//        if (node.waitStatus == Node.CONDITION || node.prev == null)
//            return false;
//        if (node.next != null) // If has successor, it must be on queue
//            return true;
//        /*
//         * node.prev can be non-null, but not yet on queue because
//         * the CAS to place it on queue can fail. So we have to
//         * traverse from tail to make sure it actually made it.  It
//         * will always be near the tail in calls to this method, and
//         * unless the CAS failed (which is unlikely), it will be
//         * there, so we hardly ever traverse much.
//         */
//        return findNodeFromTail(node);
//    }
//
//    /**
//     * 如果节点通过从尾部向后搜索在同步队列上，则返回 true。 仅在 isOnSyncQueue 需要时调用。
//     *
//     * @return true if present
//     */
//    private boolean findNodeFromTail(Node node) {
//        // We check for node first, since it's likely to be at or near tail.
//        // tail is known to be non-null, so we could re-order to "save"
//        // one null check, but we leave it this way to help the VM.
//        for (Node p = tail; ; ) {
//            if (p == node)
//                return true;
//            if (p == null)
//                return false;
//            p = p.prev;
//        }
//    }
//
//    // Instrumentation methods for conditions
//
//    /**
//     * 将节点从条件队列转移到同步队列。 如果成功则返回真。
//     *
//     * @param node the node
//     * @return true if successfully transferred (else the node was
//     * cancelled before signal)
//     */
//    final boolean transferForSignal(Node node) {
//        /*
//         * If cannot change waitStatus, the node has been cancelled.
//         */
//        if (!node.compareAndSetWaitStatus(Node.CONDITION, 0))
//            return false;
//
//        /*
//         * Splice onto queue and try to set waitStatus of predecessor to
//         * indicate that thread is (probably) waiting. If cancelled or
//         * attempt to set waitStatus fails, wake up to resync (in which
//         * case the waitStatus can be transiently and harmlessly wrong).
//         */
//        Node p = enq(node);
//        int ws = p.waitStatus;
//        if (ws > 0 || !p.compareAndSetWaitStatus(ws, Node.SIGNAL))
//            LockSupport.unpark(node.thread);
//        return true;
//    }
//
//    /**
//     * Transfers node, if necessary, to sync queue after a cancelled wait.
//     * Returns true if thread was cancelled before being signalled.
//     *
//     * @param node the node
//     * @return true if cancelled before the node was signalled
//     */
//    final boolean transferAfterCancelledWait(Node node) {
//        if (node.compareAndSetWaitStatus(Node.CONDITION, 0)) {
//            enq(node);
//            return true;
//        }
//        /*
//         * If we lost out to a signal(), then we can't proceed
//         * until it finishes its enq().  Cancelling during an
//         * incomplete transfer is both rare and transient, so just
//         * spin.
//         */
//        while (!isOnSyncQueue(node))
//            Thread.yield();
//        return false;
//    }
//
//    /**
//     * Invokes release with current state value; returns saved state.
//     * Cancels node and throws exception on failure.
//     *
//     * @param node the condition node for this wait
//     * @return previous sync state
//     */
//    final int fullyRelease(Node node) {
//        try {
//            int savedState = getState();
//            if (release(savedState))
//                return savedState;
//            throw new IllegalMonitorStateException();
//        } catch (Throwable t) {
//            node.waitStatus = Node.CANCELLED;
//            throw t;
//        }
//    }
//
//    /**
//     * Queries whether the given ConditionObject
//     * uses this synchronizer as its lock.
//     *
//     * @param condition the condition
//     * @return {@code true} if owned
//     * @throws NullPointerException if the condition is null
//     */
//    public final boolean owns(ConditionObject condition) {
//        return condition.isOwnedBy(this);
//    }
//
//    /**
//     * Queries whether any threads are waiting on the given condition
//     * associated with this synchronizer. Note that because timeouts
//     * and interrupts may occur at any time, a {@code true} return
//     * does not guarantee that a future {@code signal} will awaken
//     * any threads.  This method is designed primarily for use in
//     * monitoring of the system state.
//     *
//     * @param condition the condition
//     * @return {@code true} if there are any waiting threads
//     * @throws IllegalMonitorStateException if exclusive synchronization
//     *                                      is not held
//     * @throws IllegalArgumentException     if the given condition is
//     *                                      not associated with this synchronizer
//     * @throws NullPointerException         if the condition is null
//     */
//    public final boolean hasWaiters(ConditionObject condition) {
//        if (!owns(condition))
//            throw new IllegalArgumentException("Not owner");
//        return condition.hasWaiters();
//    }
//
//    /**
//     * Returns an estimate of the number of threads waiting on the
//     * given condition associated with this synchronizer. Note that
//     * because timeouts and interrupts may occur at any time, the
//     * estimate serves only as an upper bound on the actual number of
//     * waiters.  This method is designed for use in monitoring system
//     * state, not for synchronization control.
//     *
//     * @param condition the condition
//     * @return the estimated number of waiting threads
//     * @throws IllegalMonitorStateException if exclusive synchronization
//     *                                      is not held
//     * @throws IllegalArgumentException     if the given condition is
//     *                                      not associated with this synchronizer
//     * @throws NullPointerException         if the condition is null
//     */
//    public final int getWaitQueueLength(ConditionObject condition) {
//        if (!owns(condition))
//            throw new IllegalArgumentException("Not owner");
//        return condition.getWaitQueueLength();
//    }
//
//    /**
//     * Returns a collection containing those threads that may be
//     * waiting on the given condition associated with this
//     * synchronizer.  Because the actual set of threads may change
//     * dynamically while constructing this result, the returned
//     * collection is only a best-effort estimate. The elements of the
//     * returned collection are in no particular order.
//     *
//     * @param condition the condition
//     * @return the collection of threads
//     * @throws IllegalMonitorStateException if exclusive synchronization
//     *                                      is not held
//     * @throws IllegalArgumentException     if the given condition is
//     *                                      not associated with this synchronizer
//     * @throws NullPointerException         if the condition is null
//     */
//    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
//        if (!owns(condition))
//            throw new IllegalArgumentException("Not owner");
//        return condition.getWaitingThreads();
//    }
//
//    /**
//     * Initializes head and tail fields on first contention.
//     */
//    private final void initializeSyncQueue() {
//        Node h;
//        if (HEAD.compareAndSet(this, null, (h = new Node())))
//            tail = h;
//    }
//
//    /**
//     * CASes tail field.
//     */
//    private final boolean compareAndSetTail(Node expect, Node update) {
//        return TAIL.compareAndSet(this, expect, update);
//    }
//
//
//    /**
//     * 作为Lock实现基础的AbstractQueuedSynchronizer条件实现。
//     * 此类的方法文档从锁定和条件用户的角度描述了机制，而不是行为规范。
//     * 此类的导出版本通常需要随附描述依赖于关联AbstractQueuedSynchronizer条件语义的文档。
//     * 此类是可序列化的，但所有字段都是瞬态的，因此反序列化的条件没有等待者。
//     */
//    public class ConditionObject implements Condition, java.io.Serializable {
//        private static final long serialVersionUID = 1173984872572414699L;
//        /**
//         * Mode meaning to reinterrupt on exit from wait
//         */
//        private static final int REINTERRUPT = 1;
//        /**
//         * Mode meaning to throw InterruptedException on exit from wait
//         */
//        private static final int THROW_IE = -1;
//        /**
//         * First node of condition queue.
//         */
//        private transient Node firstWaiter;
//
//        // Internal methods
//        /**
//         * Last node of condition queue.
//         */
//        private transient Node lastWaiter;
//
//        /**
//         * Creates a new {@code ConditionObject} instance.
//         */
//        public ConditionObject() {
//        }
//
//        /**
//         * Adds a new waiter to wait queue.
//         *
//         * @return its new wait node
//         */
//        private Node addConditionWaiter() {
//            if (!isHeldExclusively())
//                throw new IllegalMonitorStateException();
//            Node t = lastWaiter;
//            // If lastWaiter is cancelled, clean out.
//            if (t != null && t.waitStatus != Node.CONDITION) {
//                unlinkCancelledWaiters();
//                t = lastWaiter;
//            }
//
//            Node node = new Node(Node.CONDITION);
//
//            if (t == null)
//                firstWaiter = node;
//            else
//                t.nextWaiter = node;
//            lastWaiter = node;
//            return node;
//        }
//
//        /**
//         * Removes and transfers nodes until hit non-cancelled one or
//         * null. Split out from signal in part to encourage compilers
//         * to inline the case of no waiters.
//         *
//         * @param first (non-null) the first node on condition queue
//         */
//        private void doSignal(Node first) {
//            do {
//                if ((firstWaiter = first.nextWaiter) == null)
//                    lastWaiter = null;
//                first.nextWaiter = null;
//            } while (!transferForSignal(first) &&
//                    (first = firstWaiter) != null);
//        }
//
//        // public methods
//
//        /**
//         * Removes and transfers all nodes.
//         *
//         * @param first (non-null) the first node on condition queue
//         */
//        private void doSignalAll(Node first) {
//            lastWaiter = firstWaiter = null;
//            do {
//                Node next = first.nextWaiter;
//                first.nextWaiter = null;
//                transferForSignal(first);
//                first = next;
//            } while (first != null);
//        }
//
//        /**
//         * Unlinks cancelled waiter nodes from condition queue.
//         * Called only while holding lock. This is called when
//         * cancellation occurred during condition wait, and upon
//         * insertion of a new waiter when lastWaiter is seen to have
//         * been cancelled. This method is needed to avoid garbage
//         * retention in the absence of signals. So even though it may
//         * require a full traversal, it comes into play only when
//         * timeouts or cancellations occur in the absence of
//         * signals. It traverses all nodes rather than stopping at a
//         * particular target to unlink all pointers to garbage nodes
//         * without requiring many re-traversals during cancellation
//         * storms.
//         */
//        private void unlinkCancelledWaiters() {
//            Node t = firstWaiter;
//            Node trail = null;
//            while (t != null) {
//                Node next = t.nextWaiter;
//                if (t.waitStatus != Node.CONDITION) {
//                    t.nextWaiter = null;
//                    if (trail == null)
//                        firstWaiter = next;
//                    else
//                        trail.nextWaiter = next;
//                    if (next == null)
//                        lastWaiter = trail;
//                } else
//                    trail = t;
//                t = next;
//            }
//        }
//
//        /**
//         * Moves the longest-waiting thread, if one exists, from the
//         * wait queue for this condition to the wait queue for the
//         * owning lock.
//         *
//         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
//         *                                      returns {@code false}
//         */
//        public final void signal() {
//            if (!isHeldExclusively())
//                throw new IllegalMonitorStateException();
//            Node first = firstWaiter;
//            if (first != null)
//                doSignal(first);
//        }
//
//        /*
//         * For interruptible waits, we need to track whether to throw
//         * InterruptedException, if interrupted while blocked on
//         * condition, versus reinterrupt current thread, if
//         * interrupted while blocked waiting to re-acquire.
//         */
//
//        /**
//         * Moves all threads from the wait queue for this condition to
//         * the wait queue for the owning lock.
//         *
//         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
//         *                                      returns {@code false}
//         */
//        public final void signalAll() {
//            if (!isHeldExclusively())
//                throw new IllegalMonitorStateException();
//            Node first = firstWaiter;
//            if (first != null)
//                doSignalAll(first);
//        }
//
//        /**
//         * Implements uninterruptible condition wait.
//         * <ol>
//         * <li>Save lock state returned by {@link #getState}.
//         * <li>Invoke {@link #release} with saved state as argument,
//         * throwing IllegalMonitorStateException if it fails.
//         * <li>Block until signalled.
//         * <li>Reacquire by invoking specialized version of
//         * {@link #acquire} with saved state as argument.
//         * </ol>
//         */
//        public final void awaitUninterruptibly() {
//            Node node = addConditionWaiter();
//            int savedState = fullyRelease(node);
//            boolean interrupted = false;
//            while (!isOnSyncQueue(node)) {
//                LockSupport.park(this);
//                if (Thread.interrupted())
//                    interrupted = true;
//            }
//            if (acquireQueued(node, savedState) || interrupted)
//                selfInterrupt();
//        }
//
//        /**
//         * Checks for interrupt, returning THROW_IE if interrupted
//         * before signalled, REINTERRUPT if after signalled, or
//         * 0 if not interrupted.
//         */
//        private int checkInterruptWhileWaiting(Node node) {
//            return Thread.interrupted() ?
//                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
//                    0;
//        }
//
//        /**
//         * Throws InterruptedException, reinterrupts current thread, or
//         * does nothing, depending on mode.
//         */
//        private void reportInterruptAfterWait(int interruptMode)
//                throws InterruptedException {
//            if (interruptMode == THROW_IE)
//                throw new InterruptedException();
//            else if (interruptMode == REINTERRUPT)
//                selfInterrupt();
//        }
//
//        /**
//         * Implements interruptible condition wait.
//         * <ol>
//         * <li>If current thread is interrupted, throw InterruptedException.
//         * <li>Save lock state returned by {@link #getState}.
//         * <li>Invoke {@link #release} with saved state as argument,
//         * throwing IllegalMonitorStateException if it fails.
//         * <li>Block until signalled or interrupted.
//         * <li>Reacquire by invoking specialized version of
//         * {@link #acquire} with saved state as argument.
//         * <li>If interrupted while blocked in step 4, throw InterruptedException.
//         * </ol>
//         */
//        @Override
//        public final void await() throws InterruptedException {
//            if (Thread.interrupted())
//                throw new InterruptedException();
//            Node node = addConditionWaiter();
//            int savedState = fullyRelease(node);
//            int interruptMode = 0;
//            while (!isOnSyncQueue(node)) {
//                LockSupport.park(this);
//                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
//                    break;
//            }
//            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
//                interruptMode = REINTERRUPT;
//            if (node.nextWaiter != null) // clean up if cancelled
//                unlinkCancelledWaiters();
//            if (interruptMode != 0)
//                reportInterruptAfterWait(interruptMode);
//        }
//
//        /**
//         * 实现定时条件等待。
//         *  1、如果当前线程被中断，则抛出 InterruptedException。
//         *  2、保存由getState返回的锁状态。
//         *  3、以保存的状态作为参数调用release ，如果失败则抛出 IllegalMonitorStateException。
//         *  4、阻塞直到发出信号、中断或超时。
//         *  5、通过以保存的状态作为参数调用特定版本的acquire 。
//         *  6、如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException。
//         */
//        @Override
//        public final long awaitNanos(long nanosTimeout)
//                throws InterruptedException {
//            if (Thread.interrupted())
//                throw new InterruptedException();
//            // We don't check for nanosTimeout <= 0L here, to allow
//            // awaitNanos(0) as a way to "yield the lock".
//            final long deadline = System.nanoTime() + nanosTimeout;
//            long initialNanos = nanosTimeout;
//            Node node = addConditionWaiter();
//            int savedState = fullyRelease(node);
//            int interruptMode = 0;
//            while (!isOnSyncQueue(node)) {
//                if (nanosTimeout <= 0L) {
//                    transferAfterCancelledWait(node);
//                    break;
//                }
//                if (nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD)
//                    LockSupport.parkNanos(this, nanosTimeout);
//                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
//                    break;
//                nanosTimeout = deadline - System.nanoTime();
//            }
//            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
//                interruptMode = REINTERRUPT;
//            if (node.nextWaiter != null)
//                unlinkCancelledWaiters();
//            if (interruptMode != 0)
//                reportInterruptAfterWait(interruptMode);
//            long remaining = deadline - System.nanoTime(); // avoid overflow
//            return (remaining <= initialNanos) ? remaining : Long.MIN_VALUE;
//        }
//
//        /**
//         * 实现绝对定时条件等待。
//         *  1、如果当前线程被中断，则抛出 InterruptedException。
//         *  2、保存由getState返回的锁状态。
//         *  3、以保存的状态作为参数调用release ，如果失败则抛出 IllegalMonitorStateException。
//         *  4、阻塞直到发出信号、中断或超时。
//         *  5、通过以保存的状态作为参数调用特定版本的acquire 。
//         *  6、如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException。
//         *  7、如果在步骤 4 中阻塞时超时，则返回 false，否则返回 true。
//         */
//        @Override
//        public final boolean awaitUntil(Date deadline)
//                throws InterruptedException {
//            long abstime = deadline.getTime();
//            if (Thread.interrupted())
//                throw new InterruptedException();
//            Node node = addConditionWaiter();
//            int savedState = fullyRelease(node);
//            boolean timedout = false;
//            int interruptMode = 0;
//            while (!isOnSyncQueue(node)) {
//                if (System.currentTimeMillis() >= abstime) {
//                    timedout = transferAfterCancelledWait(node);
//                    break;
//                }
//                LockSupport.parkUntil(this, abstime);
//                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
//                    break;
//            }
//            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
//                interruptMode = REINTERRUPT;
//            if (node.nextWaiter != null)
//                unlinkCancelledWaiters();
//            if (interruptMode != 0)
//                reportInterruptAfterWait(interruptMode);
//            return !timedout;
//        }
//
//        /**
//         * 实现定时条件等待。
//         *  1、如果当前线程被中断，则抛出 InterruptedException。
//         *  2、保存由getState返回的锁状态。
//         *  3、以保存的状态作为参数调用release ，如果失败则抛出 IllegalMonitorStateException。
//         *  4、阻塞直到发出信号、中断或超时。
//         *  5、通过以保存的状态作为参数调用特定版本的acquire 。
//         *  6、如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException。
//         *  7、如果在步骤 4 中阻塞时超时，则返回 false，否则返回 true。
//         */
//        @Override
//        public final boolean await(long time, TimeUnit unit)
//                throws InterruptedException {
//            long nanosTimeout = unit.toNanos(time);
//            if (Thread.interrupted())
//                throw new InterruptedException();
//            // We don't check for nanosTimeout <= 0L here, to allow
//            // await(0, unit) as a way to "yield the lock".
//            final long deadline = System.nanoTime() + nanosTimeout;
//            Node node = addConditionWaiter();
//            int savedState = fullyRelease(node);
//            boolean timedout = false;
//            int interruptMode = 0;
//            while (!isOnSyncQueue(node)) {
//                if (nanosTimeout <= 0L) {
//                    timedout = transferAfterCancelledWait(node);
//                    break;
//                }
//                if (nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD)
//                    LockSupport.parkNanos(this, nanosTimeout);
//                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
//                    break;
//                nanosTimeout = deadline - System.nanoTime();
//            }
//            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
//                interruptMode = REINTERRUPT;
//            if (node.nextWaiter != null)
//                unlinkCancelledWaiters();
//            if (interruptMode != 0)
//                reportInterruptAfterWait(interruptMode);
//            return !timedout;
//        }
//
//        //  support for instrumentation
//
//        /**
//         * 如果此条件是由给定的同步对象创建的，则返回 true。
//         *
//         * @return {@code true} if owned
//         */
//        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
//            return sync == AbstractQueuedSynchronizer.this;
//        }
//
//        /**
//         * 查询是否有线程在此条件下等待。
//         * 实现hasWaiters(AbstractQueuedSynchronizer.ConditionObject) 。
//         *
//         * @return {@code true} if there are any waiting threads
//         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
//         *                                      returns {@code false}
//         */
//        protected final boolean hasWaiters() {
//            if (!isHeldExclusively())
//                throw new IllegalMonitorStateException();
//            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
//                if (w.waitStatus == Node.CONDITION)
//                    return true;
//            }
//            return false;
//        }
//
//        /**
//         * 返回等待此条件的线程数的估计值。
//         * 实现getWaitQueueLength(AbstractQueuedSynchronizer.ConditionObject) 。
//         *
//         * @return the estimated number of waiting threads
//         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
//         *                                      returns {@code false}
//         */
//        protected final int getWaitQueueLength() {
//            if (!isHeldExclusively())
//                throw new IllegalMonitorStateException();
//            int n = 0;
//            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
//                if (w.waitStatus == Node.CONDITION)
//                    ++n;
//            }
//            return n;
//        }
//
//        /**
//         * 返回一个包含可能正在等待此 Condition 的线程的集合。
//         * 实现getWaitingThreads(AbstractQueuedSynchronizer.ConditionObject) 。
//         *
//         * @return the collection of threads
//         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
//         *                                      returns {@code false}
//         */
//        protected final Collection<Thread> getWaitingThreads() {
//            if (!isHeldExclusively())
//                throw new IllegalMonitorStateException();
//            ArrayList<Thread> list = new ArrayList<>();
//            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
//                if (w.waitStatus == Node.CONDITION) {
//                    Thread t = w.thread;
//                    if (t != null)
//                        list.add(t);
//                }
//            }
//            return list;
//        }
//    }
//
//    static final class Node {
//        static final Node SHARED = new Node();
//        static final Node EXCLUSIVE = null;
//
//        static final int CANCELLED = 1;
//        static final int SIGNAL = -1;
//        static final int CONDITION = -2;
//        static final int PROPAGATE = -3;
//        // VarHandle mechanics
//        private static final VarHandle NEXT;
//        private static final VarHandle PREV;
//        private static final VarHandle THREAD;
//        private static final VarHandle WAITSTATUS;
//
//        static {
//            try {
//                MethodHandles.Lookup l = MethodHandles.lookup();
//                NEXT = l.findVarHandle(Node.class, "next", Node.class);
//                PREV = l.findVarHandle(Node.class, "prev", Node.class);
//                THREAD = l.findVarHandle(Node.class, "thread", Thread.class);
//                WAITSTATUS = l.findVarHandle(Node.class, "waitStatus", int.class);
//            } catch (ReflectiveOperationException e) {
//                throw new ExceptionInInitializerError(e);
//            }
//        }
//
//        /**
//         * 状态字段，仅取值：
//         *      SIGNAL：此节点的后继节点被（或即将）阻塞（通过公园），
//         *      因此当前节点在释放或取消时必须解除其后继节点的公园。
//         *      为了避免竞争，获取方法必须首先表明它们需要一个信号，然后重试原子获取，然后在失败时阻塞。
//         *
//         *      CANCELLED：由于超时或中断，该节点被取消。 节点永远不会离开这个状态。
//         *      特别是，取消节点的线程永远不会再次阻塞。
//         *
//         *      CONDITION：该节点当前在条件队列中。
//         *      它在传输之前不会用作同步队列节点，此时状态将设置为 0。
//         *      （此处使用此值与该字段的其他用途无关，但简化了机制。）
//         *
//         *      PROPAGATE：A releaseShared 应该传播到其他节点。
//         *      这在 doReleaseShared 中设置（仅适用于头节点）以确保传播继续，即使其他操作已经介入。
//         *      0：以上都不是 为了简化使用，数值按数字排列。 非负值意味着节点不需要发出信号。
//         *      因此，大多数代码不需要检查特定值，只需检查符号。
//         *      对于普通同步节点，该字段被初始化为 0，对于条件节点，该字段被初始化为 CONDITION。
//         *      它使用 CAS 修改（或在可能的情况下，无条件 volatile 写入）。
//         */
//        volatile int waitStatus;
//
//        /**
//         * 链接到当前节点/线程依赖于检查 waitStatus 的前驱节点。
//         * 在入队期间分配，并仅在出队时取消（为了 GC）。
//         * 此外，在取消前任时，我们在找到一个未取消的时进行短路，这将始终存在，因为头节点永远不会被取消：只有成功获取的结果，节点才成为头。
//         * 一个被取消的线程永远不会成功获取，并且一个线程只会取消自己，而不是任何其他节点。
//         */
//        volatile Node prev;
//        /**
//         * Link to the successor node that the current node/thread
//         * unparks upon release. Assigned during enqueuing, adjusted
//         * when bypassing cancelled predecessors, and nulled out (for
//         * sake of GC) when dequeued.  The enq operation does not
//         * assign next field of a predecessor until after attachment,
//         * so seeing a null next field does not necessarily mean that
//         * node is at end of queue. However, if a next field appears
//         * to be null, we can scan prev's from the tail to
//         * double-check.  The next field of cancelled nodes is set to
//         * point to the node itself instead of null, to make life
//         * easier for isOnSyncQueue.
//         */
//        volatile Node next;
//        /**
//         * The thread that enqueued this node.  Initialized on
//         * construction and nulled out after use.
//         */
//        volatile Thread thread;
//        /**
//         * Link to next node waiting on condition, or the special
//         * value SHARED.  Because condition queues are accessed only
//         * when holding in exclusive mode, we just need a simple
//         * linked queue to hold nodes while they are waiting on
//         * conditions. They are then transferred to the queue to
//         * re-acquire. And because conditions can only be exclusive,
//         * we save a field by using special value to indicate shared
//         * mode.
//         */
//        Node nextWaiter;
//
//        /**
//         * Establishes initial head or SHARED marker.
//         */
//        Node() {
//        }
//
//        /**
//         * Constructor used by addWaiter.
//         */
//        Node(Node nextWaiter) {
//            this.nextWaiter = nextWaiter;
//            THREAD.set(this, Thread.currentThread());
//        }
//
//        /**
//         * Constructor used by addConditionWaiter.
//         */
//        Node(int waitStatus) {
//            WAITSTATUS.set(this, waitStatus);
//            THREAD.set(this, Thread.currentThread());
//        }
//
//        /**
//         * Returns true if node is waiting in shared mode.
//         */
//        final boolean isShared() {
//            return nextWaiter == SHARED;
//        }
//
//        /**
//         * Returns previous node, or throws NullPointerException if null.
//         * Use when predecessor cannot be null.  The null check could
//         * be elided, but is present to help the VM.
//         *
//         * @return the predecessor of this node
//         */
//        final Node predecessor() {
//            Node p = prev;
//            if (p == null)
//                throw new NullPointerException();
//            else
//                return p;
//        }
//
//        /**
//         * CASes waitStatus field.
//         */
//        final boolean compareAndSetWaitStatus(int expect, int update) {
//            return WAITSTATUS.compareAndSet(this, expect, update);
//        }
//
//        /**
//         * CASes next field.
//         */
//        final boolean compareAndSetNext(Node expect, Node update) {
//            return NEXT.compareAndSet(this, expect, update);
//        }
//
//        final void setPrevRelaxed(Node p) {
//            PREV.set(this, p);
//        }
//    }
//}
