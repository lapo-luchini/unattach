package app.unattach.controller;

import app.unattach.model.Config;
import app.unattach.model.FileConfig;
import app.unattach.model.attachmentstorage.UserStorage;
import app.unattach.model.attachmentstorage.FileUserStorage;
import app.unattach.model.service.GmailServiceManager;
import app.unattach.model.service.LiveGmailServiceManager;
import app.unattach.model.LiveModel;
import app.unattach.model.Model;
import app.unattach.utils.Clock;
import app.unattach.utils.LiveClock;

public class ControllerFactory {
  private static Controller defaultController;

  public static synchronized Controller getDefaultController() {
    if (defaultController == null) {
      Clock clock = new LiveClock();
      Config config = new FileConfig();
      UserStorage userStorage = new FileUserStorage();
      GmailServiceManager gmailServiceManager = new LiveGmailServiceManager(clock);
      Model model = new LiveModel(clock, config, userStorage, gmailServiceManager);
      defaultController = new DefaultController(model);
    }
    return defaultController;
  }
}
