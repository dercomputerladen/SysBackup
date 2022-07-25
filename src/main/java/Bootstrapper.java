import net.horizoncode.sysbackup.SysBackup;

import java.net.URISyntaxException;

public class Bootstrapper {

  public static void main(String[] args) throws URISyntaxException {
    new SysBackup().start(args);
  }
}
