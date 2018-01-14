package com.marshallhampson.WFQueue;

/**
 * @author Marshall Hampson
 */
class OpDescription {
  /** the phase at which the thread has performed its last operation on the queue */
  private long phase;
  private boolean pending;
  private boolean enqueue;
  private Node node;
  
  public OpDescription(long phase, boolean pending, boolean enqueue, Node node) {
    this.phase = phase;
    this.pending = pending;
    this.enqueue = enqueue;
    this.node = node;
  }

  public long getPhase() {
    return phase;
  }

  public void setPhase(long phase) {
    this.phase = phase;
  }

  public boolean isPending() {
    return pending;
  }

  public void setPending(boolean pending) {
    this.pending = pending;
  }

  public boolean isEnqueue() {
    return enqueue;
  }

  public void setEnqueue(boolean enqueue) {
    this.enqueue = enqueue;
  }

  public Node getNode() {
    return node;
  }

  public void setNode(Node node) {
    this.node = node;
  }
}
