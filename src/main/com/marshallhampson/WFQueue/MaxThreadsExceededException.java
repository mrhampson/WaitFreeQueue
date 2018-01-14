package com.marshallhampson.WFQueue;

/**
 * An exception thrown by the {@link WFQueue} if the number of unique threads accessing it was more than the limit
 * set upon construction of the queue
 * @author Marshall Hampson
 */
public class MaxThreadsExceededException extends Exception {
}
