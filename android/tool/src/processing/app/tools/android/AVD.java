package processing.app.tools.android;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import processing.app.Base;

public class AVD {

  /** Name of this avd. */
  public final String name;

  /** "android-4" or "Google Inc.:Google APIs:4" */
  public final String target;

  /**
   * Default virtual device used by Processing, designed to be similar to a
   * device like the Moto Droid. Uses Android 2.0 APIs, and the screen is set to
   * WVGA854 (854x480), the same aspect ratio (with rounding), as 1920x1080, or
   * 16:9.
   */
  public static final AVD ECLAIR = new AVD("Processing-Eclair",
                                           "Google Inc.:Google APIs:5");

  public static boolean ensureEclairAVD(final AndroidSDK sdk) {
    try {
      if ((ECLAIR.exists(sdk) || ECLAIR.create(sdk))) {
        return true;
      }
      Base.showWarning("Android Error", AVD_CREATE_ERROR, null);
    } catch (final Exception e) {
      Base.showWarning("Android Error", AVD_CREATE_ERROR, e);
    }
    return false;
  }

  public AVD(final String name, final String target) {
    this.name = name;
    this.target = target;
  }

  private static final Pattern AVD_ROW = Pattern.compile("\\s+Name:\\s+(\\S+)");

  protected boolean exists(final AndroidSDK sdk) throws IOException {
    try {
      final ProcessResult listResult = new ProcessHelper(sdk.getAndroidTool()
          .getAbsolutePath(), "list", "avds").execute();
      if (listResult.succeeded()) {
        for (final String line : listResult) {
          final Matcher m = AVD_ROW.matcher(line);
          if (m.matches() && m.group(1).equals(name)) {
            return true;
          }
        }
      } else {
        System.err.println(listResult);
      }
    } catch (final InterruptedException ie) {
    }
    return false;
  }

  protected boolean create(final AndroidSDK sdk) throws IOException {
    final ProcessHelper p = new ProcessHelper(sdk.getAndroidTool()
        .getAbsolutePath(), "create", "avd", "-n", name, "-t", target, "-c",
                                              "64M");
    try {
      final ProcessResult createAvdResult = p.execute();
      if (createAvdResult.succeeded()) {
        return true;
      }
      System.err.println(createAvdResult);
    } catch (final InterruptedException ie) {
    }
    return false;
  }

  private static final String AVD_CREATE_ERROR = "An error occurred while running “android create avd”\n"
      + "to set up the default Android emulator. Make sure that the\n"
      + "Android SDK is installed properly, and that the Android\n"
      + "and Google APIs are installed for level 5.";

}
