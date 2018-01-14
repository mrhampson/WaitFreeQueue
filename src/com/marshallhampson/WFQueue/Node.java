package com.marshallhampson.WFQueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A linked-list node for the {@link WFQueue}
 * @author Marshall Hampson
 */
class Node<T> {
  private final T value;
  private final AtomicReference<Node<T>> next;
  /** the enqueueing thread id*/
  private final int enqTid;
  /** the dequeueing thread id*/
  private final AtomicInteger deqTid;

  /**
   * Constructs a new Node
   * @param value the value this node holds
   * @param enqTid the enqueueing thread id
   */
  Node(T value, int enqTid) {
    this.value = value;
    this.enqTid = enqTid;
    this.deqTid = new AtomicInteger(-1);
    this.next = new AtomicReference<>(null);
  }

  /**
   * Gets this nodes value
   * @return this nodes value
   */
  public T getValue() {
    return this.value;
  }

  /**
   * Gets the ref to the next node
   * @return the {@link AtomicReference} to the next node
   */
  public AtomicReference<Node<T>> getNext() {
    return this.next;
  }

  /**
   * Gets the enqueueing thread id
   * @return the enqueueing thread id
   */
  public int getEnqTid() {
    return this.enqTid;
  }

  /**
   * Gets the dequeueing thread id
   * @return the dequeueing thread id
   */
  public AtomicInteger getDeqTid() {
    return this.deqTid;
  }
}
