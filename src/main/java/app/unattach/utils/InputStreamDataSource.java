package app.unattach.utils;

import org.apache.commons.lang3.NotImplementedException;

import javax.activation.DataSource;
import java.io.InputStream;
import java.io.OutputStream;

public record InputStreamDataSource(InputStream inputStream) implements DataSource {
  @Override
  public String getContentType() {
    return "*/*";
  }

  @Override
  public InputStream getInputStream() {
    return inputStream;
  }

  @Override
  public String getName() {
    return "InputStreamDataSource";
  }

  @Override
  public OutputStream getOutputStream() {
    throw new NotImplementedException();
  }
}
