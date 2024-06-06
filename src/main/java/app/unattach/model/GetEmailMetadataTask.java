package app.unattach.model;

import app.unattach.controller.LongTask;
import app.unattach.controller.LongTaskException;
import app.unattach.model.service.GmailServiceException;
import app.unattach.utils.Clock;

import java.util.List;

public class GetEmailMetadataTask implements LongTask<GetEmailMetadataTask.Result> {
  interface Worker {
    void getEmailMetadata(int startIndexInclusive, int endIndexExclusive) throws GmailServiceException;
  }

  public record Result(int currentBatchNumber) {}

  private final Clock clock;
  private final List<String> emailIds;
  // (maximum batch size = 100)
  // batch size = 40 ==> batch quota units = 200 ==> 1 batch / second
  // https://cloud.google.com/api-keys/docs/quotas says 240 reads per min and 120
  // writes per min.
  // https://developers.google.com/gmail/api/reference/quota
  // https://console.cloud.google.com/apis/api/gmail.googleapis.com/quotas?project=walsh-unattach-gmail
  // https://console.cloud.google.com/iam-admin/quotas?project=walsh-unattach-gmail&pageState=(%22allQuotasTable%22:(%22s%22:%5B(%22i%22:%22currentUsage%22,%22s%22:%220%22),(%22i%22:%22currentPercent%22,%22s%22:%220%22),(%22i%22:%22sevenDayPeakPercent%22,%22s%22:%220%22),(%22i%22:%22sevenDayPeakUsage%22,%22s%22:%220%22),(%22i%22:%22serviceTitle%22,%22s%22:%220%22),(%22i%22:%22displayName%22,%22s%22:%220%22),(%22i%22:%22displayDimensions%22,%22s%22:%220%22)%5D,%22f%22:%22%255B%255D%22))
  private final int batchSize = 25;
  private final int numberOfBatches;
  private final Worker worker;
  private int currentBatchNumber;

  GetEmailMetadataTask(Clock clock, List<String> emailIds, Worker worker) {
    this.clock = clock;
    this.emailIds = emailIds;
    numberOfBatches = (emailIds.size() + batchSize - 1) / batchSize;
    this.worker = worker;
  }

  @Override
  public int getNumberOfSteps() {
    return numberOfBatches;
  }

  @Override
  public boolean hasMoreSteps() {
    return currentBatchNumber < numberOfBatches;
  }

  @Override
  public Result takeStep() throws LongTaskException {
    try {
      if (currentBatchNumber != 0) {
        clock.sleep(10_000);
      }
      final int startIndexInclusive = currentBatchNumber * batchSize;
      final int endIndexExclusive = Math.min(emailIds.size(), (currentBatchNumber + 1) * batchSize);
      worker.getEmailMetadata(startIndexInclusive, endIndexExclusive);
      ++currentBatchNumber;
      return new Result(currentBatchNumber);
    } catch (Throwable t) {
      throw new LongTaskException(t);
    }
  }
}
