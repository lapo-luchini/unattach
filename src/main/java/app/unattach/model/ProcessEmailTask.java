package app.unattach.model;

import app.unattach.controller.LongTask;
import app.unattach.controller.LongTaskException;
import app.unattach.utils.Clock;

interface EmailProcessorFunctor {
  ProcessEmailResult processEmail(Email email) throws Exception;
}

record ProcessEmailTask(Clock clock, Email email, EmailProcessorFunctor processEmailFunction)
    implements LongTask<ProcessEmailResult> {
  @Override
  public int getNumberOfSteps() {
    return 1;
  }

  @Override
  public boolean hasMoreSteps() {
    return email.getStatus() == EmailStatus.TO_PROCESS;
  }

  @Override
  public ProcessEmailResult takeStep() throws LongTaskException {
    try {
      ProcessEmailResult result = processEmailFunction.processEmail(email);
      email.setStatus(EmailStatus.PROCESSED);
      // Given the 250 quota units / user / second limit, and where each request uses
      // around 40, sleeping for 160ms is minimal, but better to sleep for longer.
      clock.sleep(500);
      return result;
    } catch (Throwable t) {
      throw new LongTaskException(t);
    }
  }
}
