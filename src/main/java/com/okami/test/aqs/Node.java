//package com.okami.test.aqs;
//
//import java.lang.invoke.MethodHandles;
//import java.lang.invoke.VarHandle;
//
//static final class Node {
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
//         *      特别是，取消节点的线程永远不会再次阻塞。 条件：该节点当前在条件队列中。
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
