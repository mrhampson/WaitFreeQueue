package com.marshallhampson.WFQueue;

/**
 * Represents the state of an operation that a thread is performing in the {@link WFQueue}
 * @author Marshall Hampson
 */
class OpDescription<T> {
  /** the phase at which the thread has performed its last operation on the queue */
  private final long phase;
  private final boolean pending;
  private final boolean enqueue;
  private final Node<T> node;

  /**
   * Constructs a new {@link OpDescription}
   * @param phase the phase that this operation takes place in
   * @param pending whether or not this operation is still pending
   * @param enqueue true if enqueue operation, false if dequeue
   * @param node the new node this operation is trying to enqueue or null if this is a dequeue operation
   */
  OpDescription(long phase, boolean pending, boolean enqueue, Node<T> node) {
    this.phase = phase;
    this.pending = pending;
    this.enqueue = enqueue;
    this.node = node;
  }

  /**
   * Gets the phase this operation takes place in. The phase number is unique across all operations on any thread
   * @return the phase this operation takes place in
   */
  public long getPhase() {
    return this.phase;
  }

  /**
   * Gets whether or not the task is pending still
   * @return true if still pending, false otherwise
   */
  public boolean isPending() {
    return this.pending;
  }

  /**
   * Gets whether or not this is an enqueue operation
   * @return true if this is an enqueue, false if this is a dequeue
   */
  public boolean isEnqueue() {
    return this.enqueue;
  }

  /**
   * Gets the node that this operation will be putting into the queue, null if a deqeue
   * @return the node to enqueue, null if a dequeue operation
   */
  public Node<T> getNode() {
    return this.node;
  }
}
