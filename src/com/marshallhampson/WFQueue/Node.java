package com.marshallhampson.WFQueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Marshall Hampson
 */
class Node {
  private int value;
  private AtomicReference<Node> next;
  /** the enqueueing thread id*/
  private int enqTid;
  /** the dequeueing thread id*/
  private AtomicInteger deqTid;
  
  public Node(int value, int enqTid) {
    this.value = value;
    this.enqTid = enqTid;
    this.deqTid = new AtomicInteger(-1);
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public AtomicReference<Node> getNext() {
    return next;
  }

  public void setNext(AtomicReference<Node> next) {
    this.next = next;
  }

  /**
   * Gets the enqueueing thread id
   * @return the enqueueing thread id
   */
  public int getEnqTid() {
    return enqTid;
  }
  
  public void setEnqTid(int enqTid) {
    this.enqTid = enqTid;
  }

  /**
   * Gets the dequeueing thread id
   * @return the dequeueing thread id
   */
  public AtomicInteger getDeqTid() {
    return deqTid;
  }

  public void setDeqTid(AtomicInteger deqTid) {
    this.deqTid = deqTid;
  }
}
