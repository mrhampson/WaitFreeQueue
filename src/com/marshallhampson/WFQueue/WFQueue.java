package com.marshallhampson.WFQueue;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * @author Marshall Hampson
 */
public class WFQueue<T> {
  
  private final int maxThreads;
  private final AtomicReference<Node<T>> head;
  private final AtomicReference<Node<T>> tail;
  private final AtomicReferenceArray<OpDescription<T>> stateArray;
  private final AtomicLongArray threadIdArray;

  public WFQueue(int maxThreads) {
    this.maxThreads = maxThreads;
    Node<T> sentinel = new Node<>(null, -1);
    this.head = new AtomicReference<>(sentinel);
    this.tail = new AtomicReference<>(sentinel);
    this.threadIdArray = new AtomicLongArray(this.maxThreads);
    for (int i = 0; i < this.threadIdArray.length(); i++) {
      this.threadIdArray.set(i, -1);
    }
    this.stateArray = new AtomicReferenceArray<>(this.maxThreads);
    for (int i = 0; i < this.stateArray.length(); i++) {
      this.stateArray.set(i, new OpDescription<>(-1, false, true, null));
    }
  }

  /**
   * Helps finish pending operations left by other threads
   * @param phase the current phase
   */
  private void help(long phase) {
    for (int i = 0; i < this.stateArray.length(); i++) {
      OpDescription<T> description = this.stateArray.get(i);
      if (description.isPending() && description.getPhase() <= phase) {
        if (description.isEnqueue()) {
          help_enq(i, phase);
        } else {
          help_deq(i, phase);
        }
      }
    }
  }

  private long maxPhase() {
    long maxPhase = -1;
    for (int i = 0; i < this.stateArray.length(); i++) {
      long phase = this.stateArray.get(i).getPhase();
      if (phase > maxPhase) {
        maxPhase = phase;
      }
    }
    return maxPhase;
  }

  private boolean isStillPending(int tid, long phase) {
    return this.stateArray.get(tid).isPending() && this.stateArray.get(tid).getPhase() <= phase;
  }

  /**
   * Maps the java thread id to the queue thread id aka the index in the state array
   * @param thread the thread to find a mapping for
   * @return the queue's thread id (index in state array) for this thread 
   * @throws MaxThreadsExceededException if we have no more room in the state array for another unique thread
   */
  private int mapJavaThreadToQueueThreadId(Thread thread) throws MaxThreadsExceededException {
    // Find the queue's thread id for the current thread
    long javaTid = thread.getId();
    int currentTid = -1;
    for (int i = 0; i < this.threadIdArray.length(); i++) {
      // We've found an empty slot in the threadId array
      if (this.threadIdArray.compareAndSet(i, -1, javaTid)) {
        currentTid = i;
        break;
      }
      // We found a matching id
      if (this.threadIdArray.get(i) == javaTid) {
        currentTid = i;
        break;
      }
    }
    // We didn't find the java thread id in the array and there were no more empty slots
    if (currentTid == -1) {
      throw new MaxThreadsExceededException();
    }
    return currentTid;
  }
  
  public void enqueue(T value) throws MaxThreadsExceededException {
    long phase = maxPhase() + 1;
    int currentTid = mapJavaThreadToQueueThreadId(Thread.currentThread());
    this.stateArray.set(currentTid, new OpDescription<T>(phase, true, true, new Node<T>(value, currentTid)));
    this.help(phase);
    help_finish_enqueue();
  }

  private void help_enq(int tid, long phase) {
    while(isStillPending(tid, phase)) {
      Node<T> last = this.tail.get();
      Node<T> next = last.getNext().get();
      if (last == this.tail.get()) {
        // Enqueue can be applied
        if (next == null) {
          if (isStillPending(tid, phase)) {
            if (last.getNext().compareAndSet(next, this.stateArray.get(tid).getNode())) {
              help_finish_enqueue();
              return;
            }
          }
        }
      // Some enqueue is in progress
      } else {
        // help it first then retry
        help_finish_enqueue();
      }
    }
  }

  private void help_finish_enqueue() {
    Node<T> last = this.tail.get();
    Node<T> next = last.getNext().get();
    if (next != null) {
      // Read enqueuer thread id of last element
      int tid = next.getEnqTid();
      OpDescription<T> currentDescription = this.stateArray.get(tid);
      if (last == this.tail.get() && this.stateArray.get(tid).getNode() == next) {
        OpDescription<T> newDescription = new OpDescription<>(
          this.stateArray.get(tid).getPhase(), false, true, next);
        this.stateArray.compareAndSet(tid, currentDescription, newDescription);
        this.tail.compareAndSet(last, next);
      }
    }
  }
  
  public T dequeue() throws EmptyQueueException, MaxThreadsExceededException {
    long phase = maxPhase() + 1;
    int currentTid = mapJavaThreadToQueueThreadId(Thread.currentThread());
    this.stateArray.set(currentTid, new OpDescription<>(phase, true, false, null));
    this.help(phase);
    this.help_finish_deq();
    Node<T> node = this.stateArray.get(currentTid).getNode();
    if (node == null) {
      throw new EmptyQueueException();
    }
    return node.getNext().get().getValue();
  }
  
  private void help_deq(int tid, long phase) {
    while (isStillPending(tid, phase)) {
      Node<T> first = this.head.get();
      Node<T> last = this.tail.get();
      Node<T> next = first.getNext().get();
      // queue might be empty
      if (first == this.head.get()) {
        // queue is empty
        if (first == last) { 
          if (next == null) {
            OpDescription<T> currentDescription = this.stateArray.get(tid);
            if (last == this.tail.get() && isStillPending(tid, phase)) {
              OpDescription<T> newDescription = 
                new OpDescription<>(this.stateArray.get(tid).getPhase(), false, false, null);
              this.stateArray.compareAndSet(tid, currentDescription, newDescription);
            }
          // Some enqueue is in progress
          } else {
            // help it first then retry
            help_finish_enqueue();
          }
        // Queue is not empty
        } else {
          OpDescription<T> currentDescription = this.stateArray.get(tid);
          Node<T> node = currentDescription.getNode();
          if (!isStillPending(tid, phase)) {
            break;
          }
          if (first == this.head.get() && node != first) {
            OpDescription<T> newDescription = 
              new OpDescription<>(this.stateArray.get(tid).getPhase(), true, false, first);
            if (!this.stateArray.compareAndSet(tid, currentDescription, newDescription)) {
              continue;
            }
          }
          first.getDeqTid().compareAndSet(-1, tid);
          help_finish_deq();
        }
      } 
    }
  }
  
  private void help_finish_deq() {
    Node<T> first = this.head.get();
    Node<T> next = first.getNext().get();
    int tid = first.getDeqTid().get();
    if (tid != -1) {
      OpDescription<T> currentDescription = this.stateArray.get(tid);
      if (first == this.head.get() && next != null) {
        OpDescription<T> newDescription = 
          new OpDescription<>(this.stateArray.get(tid).getPhase(), false, false, 
            this.stateArray.get(tid).getNode());
        this.stateArray.compareAndSet(tid, currentDescription, newDescription);
        this.head.compareAndSet(first, next);
      }
    }
  }
}
