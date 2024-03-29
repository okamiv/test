//package com.okami.test.aqs;
//
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.Lock;
//
///**
// * 独占锁
// */
//
//public class ExclusiveLock implements Lock {
//
//    private final Sync sync = new Sync();
//
//    @Override
//    public void lock() {
//        sync.acquire(1);
//    }
//
//    @Override
//    public void lockInterruptibly() throws InterruptedException {
//        sync.acquireInterruptibly(1);
//    }
//
//    @Override
//    public boolean tryLock() {
//        return sync.tryAcquire(1);
//    }
//
//    @Override
//    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
//        return sync.tryAcquireNanos(1, unit.toNanos(time));
//    }
//
//    @Override
//    public void unlock() {
//        sync.release(1);
//    }
//
//    @Override
//    public Condition newCondition() {
//        return sync.newCondition();
//    }
//
//    private final class Sync extends AbstractQueuedSynchronizer {
//        @Override
//        protected boolean tryAcquire(int arg) {
//            if (compareAndSetState(0, 1)) {
//                setExclusiveOwnerThread(Thread.currentThread());
//                return true;
//            }
//            return false;
//        }
//
//        @Override
//        protected boolean tryRelease(int arg) {
//            if (getState() == 0) {
//                throw new UnsupportedOperationException();
//            }
//
//            // 持有锁的线程设置为null
//            setExclusiveOwnerThread(null);
//            setState(0);
//            return true;
//        }
//
//        @Override
//        protected boolean isHeldExclusively() {
//            return getState() == 1;
//        }
//
//        Condition newCondition() {
//            return new ConditionObject();
//        }
//    }
//
//
//}
