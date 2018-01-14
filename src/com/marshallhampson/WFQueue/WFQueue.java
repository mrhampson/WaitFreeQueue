package com.marshallhampson.WFQueue;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * @author Marshall Hampson
 */
public class WFQueue {
  private static final int NUM_THREADS = 4;
  
  private AtomicReference<Node> head;
  private AtomicReference<Node> tail;
  private AtomicReferenceArray<OpDescription> stateArray;

  public WFQueue() {
    this.head = new AtomicReference<>(new Node(-1, -1));
    this.tail = new AtomicReference<>(new Node(-1, -1));
    this.stateArray = new AtomicReferenceArray<>(NUM_THREADS);
    
    for (int i = 0; i < this.stateArray.length(); i++) {
      this.stateArray.set(i, new OpDescription(-1, false, true, null));
    }
  }

  /**
   * Helps finish pending operations left by other threads
   * @param phase the current phase
   */
  private void help(long phase) {
    for (int i = 0; i < this.stateArray.length(); i++) {
      OpDescription description = this.stateArray.get(i);
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
  
  public void enqueue(int value) {
    long phase = maxPhase() + 1;
    int currentTid = 1; // TODO how to get thread id from current thread and map to array index
    this.stateArray.set(currentTid, new OpDescription(phase, true, true, new Node(value, currentTid)));
    this.help(phase);
    help_finish_enqueue();
  }

  private void help_enq(int tid, long phase) {
    while(isStillPending(tid, phase)) {
      Node last = this.tail.get();
      Node next = last.getNext().get();
      if (last == this.tail.get()) {
        // Enqueue can be applied
        if (next == null) {
          if (isStillPending(tid, phase)) {
            if (last.getNext().compareAndSet(next, this.stateArray.get(tid).getNode())) {
              help_finish_enqueue();
            }
          }
        }
      }
    }
  }

  private void help_finish_enqueue() {
    Node last = this.tail.get();
    Node next = last.getNext().get();
    if (next != null) {
      // Read enqueuer thread id of last element
      int tid = next.getEnqTid();
      OpDescription currentDescription = this.stateArray.get(tid);
      if (last == this.tail.get() && this.stateArray.get(tid).getNode() == next) {
        OpDescription newDescription = new OpDescription(
          this.stateArray.get(tid).getPhase(), false, true, next);
        this.stateArray.compareAndSet(tid, currentDescription, newDescription);
        this.tail.compareAndSet(last, next);
      }
    }
  }
  
  public int dequeue() throws Exception {
    long phase = maxPhase() + 1;
    int currentTid = 1; // TODO how to get thread id from current thread and map to array index
    this.stateArray.set(currentTid, new OpDescription(phase, true, false, null));
    this.help(phase);
    // this.help_finish_dequeue
    Node node = this.stateArray.get(currentTid).getNode();
    if (node == null) {
      throw new EmptyQueueException();
    }
    return node.getNext().get().getValue();
  }
  
  private void help_deq(int tid, long phase) {
    while (isStillPending(tid, phase)) {
      Node first = this.head.get();
      Node last = this.tail.get();
      Node next = first.getNext().get();
      // queue might be empty
      if (first == this.head.get()) {
        // queue is empty
        if (first == last) { 
          if (next == null) {
            OpDescription currentDescription = this.stateArray.get(tid);
            if (last == this.tail.get() && isStillPending(tid, phase)) {
              OpDescription newDescription = 
                new OpDescription(this.stateArray.get(tid).getPhase(), false, false, null);
            }
          // Some enqueue is in progress
          } else {
            // help it first theen retry
            help_finish_enqueue();
          }
        }
        // Queue is not empty
        else {
          OpDescription currentDescription = this.stateArray.get(tid);
          Node node = currentDescription.getNode();
          if (!isStillPending(tid, phase))
            break;
          if (first == this.head.get() && node != first) {
            OpDescription newDescription = 
              new OpDescription(this.stateArray.get(tid).getPhase(), true, false, first);
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
    Node first = this.head.get();
    Node next = first.getNext().get();
    int tid = first.getDeqTid().get();
    if (tid != -1) {
      OpDescription currentDescription = this.stateArray.get(tid);
      if (first == this.head.get() && next != null) {
        OpDescription newDescription = 
          new OpDescription(this.stateArray.get(tid).getPhase(), false, false, 
            this.stateArray.get(tid).getNode());
        this.stateArray.compareAndSet(tid, currentDescription, newDescription);
        this.head.compareAndSet(first, next);
      }
    }
  }
}
