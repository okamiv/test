//package com.okami.test.aqs;
//
//import java.util.concurrent.locks.Condition;
//
///**
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