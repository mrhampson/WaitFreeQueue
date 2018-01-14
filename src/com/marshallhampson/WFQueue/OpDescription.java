package com.marshallhampson.WFQueue;

/**
 * @author Marshall Hampson
 */
class OpDescription<T> {
  /** the phase at which the thread has performed its last operation on the queue */
  private final long phase;
  private final boolean pending;
  private final boolean enqueue;
  private final Node<T> node;
  
  public OpDescription(long phase, boolean pending, boolean enqueue, Node<T> node) {
    this.phase = phase;
    this.pending = pending;
    this.enqueue = enqueue;
    this.node = node;
  }

  public long getPhase() {
    return phase;
  }

  public boolean isPending() {
    return pending;
  }

  public boolean isEnqueue() {
    return enqueue;
  }

  public Node<T> getNode() {
    return node;
  }
}
