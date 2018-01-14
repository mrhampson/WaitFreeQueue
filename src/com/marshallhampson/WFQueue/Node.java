package com.marshallhampson.WFQueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Marshall Hampson
 */
class Node<T> {
  private final T value;
  private final AtomicReference<Node<T>> next;
  /** the enqueueing thread id*/
  private final int enqTid;
  /** the dequeueing thread id*/
  private final AtomicInteger deqTid;
  
  Node(T value, int enqTid) {
    this.value = value;
    this.enqTid = enqTid;
    this.deqTid = new AtomicInteger(-1);
    this.next = new AtomicReference<>(null);
  }

  public T getValue() {
    return value;
  }

  public AtomicReference<Node<T>> getNext() {
    return next;
  }

  /**
   * Gets the enqueueing thread id
   * @return the enqueueing thread id
   */
  public int getEnqTid() {
    return enqTid;
  }

  /**
   * Gets the dequeueing thread id
   * @return the dequeueing thread id
   */
  public AtomicInteger getDeqTid() {
    return deqTid;
  }
}
