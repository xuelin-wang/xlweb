package xl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class CmdLauncher {
  private static class StreamConsumer extends Thread {
    private InputStream _in;
    private StringBuilder _sb;
    private int _maxLine = -1;

    StreamConsumer(InputStream in) {
      this(in, -1);
    }
    
    StreamConsumer(InputStream in, int maxLine) {
        this._in = in;
        _sb = new StringBuilder();
        _maxLine = maxLine;
    }

    @Override
    public void run() {
      BufferedReader br = null;
        try {
          br = new BufferedReader(new InputStreamReader(_in));
          String line;
          int lineCount = 0;
          while ((line = br.readLine()) != null) {
            if (_maxLine > 0 && lineCount < _maxLine)
              _sb.append(line);
            lineCount ++;
          }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
          if (br != null)
            try {
              br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public String getResult() {
      return _sb.toString();
    }
  }

  public static String[] run(String workDir, Map<String, String> env, List<String> cmd, int maxLineCount)
      throws IOException, InterruptedException {
    ProcessBuilder builder = new ProcessBuilder(cmd);
    if (workDir != null)
      builder.directory(new File(workDir));
    if (env != null) {
      builder.environment().putAll(env);
    }
    Process process = builder.start();
    StreamConsumer outConsumer = new StreamConsumer(process.getInputStream(), maxLineCount);
    StreamConsumer errConsumer = new StreamConsumer(process.getErrorStream(), maxLineCount);
    outConsumer.start();
    errConsumer.start();
    int exitCode = process.waitFor();
    outConsumer.join();
    errConsumer.join();
    return new String[]{
        String.valueOf(exitCode),
        outConsumer.getResult(),
        errConsumer.getResult()
    };
  }
}
