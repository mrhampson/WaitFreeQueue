package com.marshallhampson.WFQueue;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A wait free queue implementation based on the paper 
 * "Wait-Free Queues With Multiple Enqueuers and Dequeuers" by Kogan and Petrank
 * @author Marshall Hampson
 */
public class WFQueue<T> {
  
  private final AtomicReference<Node<T>> head;
  private final AtomicReference<Node<T>> tail;
  private final AtomicReferenceArray<OpDescription<T>> stateArray;
  private final AtomicLongArray threadIdArray;

  /**
   * Constructs a new {@link WFQueue}
   * @param maxThreads the maximum number of uniqueue threads that may access this queue
   */
  public WFQueue(int maxThreads) {
    if (maxThreads < 1) {
      throw new IllegalArgumentException("Must have at least one thread");
    }
    Node<T> sentinel = new Node<>(null, -1);
    this.head = new AtomicReference<>(sentinel);
    this.tail = new AtomicReference<>(sentinel);
    this.threadIdArray = new AtomicLongArray(maxThreads);
    for (int i = 0; i < this.threadIdArray.length(); i++) {
      this.threadIdArray.set(i, -1);
    }
    this.stateArray = new AtomicReferenceArray<>(maxThreads);
    for (int i = 0; i < this.stateArray.length(); i++) {
      this.stateArray.set(i, new OpDescription<>(-1, false, true, null));
    }
  }

  /**
   * Allows threads to finish pending operations with phase less than or equal to the phase 
   * left behind by other threads
   * @param phase the phase to complete
   */
  private void help(long phase) {
    for (int i = 0; i < this.stateArray.length(); i++) {
      OpDescription<T> description = this.stateArray.get(i);
      if (description.isPending() && description.getPhase() <= phase) {
        if (description.isEnqueue()) {
          helpEnqueue(i, phase);
        } else {
          helpDequeue(i, phase);
        }
      }
    }
  }

  /**
   * Finds the maxPhase of all the operations in the stateArray
   * @return the maxPhase 
   */
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

  /**
   * Checks to see if there are any operations for the given thread and phase are still pending 
   * @param tid the thread id
   * @param phase the phase number
   * @return true if there's a pending operation in the state array for the given thread and phase, false otherwise
   */
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

  /**
   * Enqueues a value
   * @param value the value to enqueue
   * @throws MaxThreadsExceededException if too many unique threads have accessed the queue
   */
  public void enqueue(T value) throws MaxThreadsExceededException {
    long phase = maxPhase() + 1;
    int currentTid = mapJavaThreadToQueueThreadId(Thread.currentThread());
    this.stateArray.set(currentTid, new OpDescription<>(phase, true, true, new Node<T>(value, currentTid)));
    this.help(phase);
    finishEnqueue();
  }

  /**
   * Allows another thread to complete a pending enqueue operation left behind by another thread
   * @param tid the id of the thread that left this enqueue operation behind
   * @param phase the phase of the thread actually calling this method
   */
  private void helpEnqueue(int tid, long phase) {
    while(isStillPending(tid, phase)) {
      Node<T> last = this.tail.get();
      Node<T> next = last.getNext().get();
      if (last == this.tail.get()) {
        // Enqueue can be applied
        if (next == null) {
          if (isStillPending(tid, phase)) {
            if (last.getNext().compareAndSet(next, this.stateArray.get(tid).getNode())) {
              finishEnqueue();
              return;
            }
          }
        }
      // Some enqueue is in progress
      } else {
        // help it first then retry
        finishEnqueue();
      }
    }
  }

  /**
   * Method that completes the enqueue operation
   */
  private void finishEnqueue() {
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

  /**
   * Dequeues an element from the queue
   * @return the value of the element in the queue
   * @throws EmptyQueueException if we have tried to dequeue an empty queue
   * @throws MaxThreadsExceededException if we have accessed the queue from too many unique threads
   */
  public T dequeue() throws EmptyQueueException, MaxThreadsExceededException {
    long phase = maxPhase() + 1;
    int currentTid = mapJavaThreadToQueueThreadId(Thread.currentThread());
    this.stateArray.set(currentTid, new OpDescription<>(phase, true, false, null));
    this.help(phase);
    this.finishDequeue();
    Node<T> node = this.stateArray.get(currentTid).getNode();
    if (node == null) {
      throw new EmptyQueueException();
    }
    return node.getNext().get().getValue();
  }

  /**
   * Helps a thread finish a pending dequeue operation left behind by another thread
   * @param tid the thread id of the thread that left this dequeue operation behind
   * @param phase the phase of the calling thread
   */
  private void helpDequeue(int tid, long phase) {
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
            finishEnqueue();
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
          finishDequeue();
        }
      } 
    }
  }

  /**
   * Finishes a dequeue operation
   */
  private void finishDequeue() {
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
