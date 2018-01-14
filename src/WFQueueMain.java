import com.marshallhampson.WFQueue.EmptyQueueException;
import com.marshallhampson.WFQueue.MaxThreadsExceededException;
import com.marshallhampson.WFQueue.WFQueue;

/**
 * @author Marshall Hampson
 */
public class WFQueueMain {
  
  public static void main(String... args) {
    try {
      WFQueue<Integer> wfQueue = new WFQueue<>(1);
      wfQueue.enqueue(1);
      Integer deqInt = wfQueue.dequeue();
      assert deqInt == 2;
    }
    catch (MaxThreadsExceededException | EmptyQueueException e) {
      System.out.println(e.getClass().getSimpleName() + " exception occurred");
      printStackTrace(e.getStackTrace());
    }
  }
  
  private static void printStackTrace(StackTraceElement[] stackTraceElements) {
    if (stackTraceElements != null) {
      for (StackTraceElement element : stackTraceElements) {
        if (element != null) {
          System.out.println('\t' + element.toString());
        }
      }
    }
  }
}
