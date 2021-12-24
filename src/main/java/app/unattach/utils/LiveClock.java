package app.unattach.utils;

public class LiveClock implements Clock {
  private static final Logger logger = Logger.get();

  @Override
  public void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      logger.error("Failed to sleep.", e);
    }
  }
}
