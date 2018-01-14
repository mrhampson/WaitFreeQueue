import com.marshallhampson.WFQueue.EmptyQueueException;
import com.marshallhampson.WFQueue.WFQueue;

/**
 * @author Marshall Hampson
 */
public class WFQueueMain {
  
  public static void main(String... args) {
    try {
      WFQueue<Integer> wfQueue = new WFQueue<>();
      wfQueue.enqueue(1);

      wfQueue.dequeue();
    }
    catch (EmptyQueueException e) {
      System.out.println("Empty queue exception occurred");
      printStackTrace(e.getStackTrace());
    }
  }
  
  private static void printStackTrace(StackTraceElement[] stackTraceElements) {
    if (stackTraceElements != null) {
      for (int i = 0; i < stackTraceElements.length; i++) {
        StackTraceElement element = stackTraceElements[i];
        if (element != null) {
          System.out.println('\t' + element.toString());
        }
      }
    }
  }
}
