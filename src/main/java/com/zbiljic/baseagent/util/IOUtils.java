package com.zbiljic.baseagent.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

  private IOUtils() { /* No instance methods */ }

  private static final int EOF = -1;
  private static final int BUFFER_SIZE = 4096;

  public static void copy(InputStream input, OutputStream output) throws IOException {
    int n;
    final byte[] buffer = new byte[BUFFER_SIZE];
    while (EOF != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
    }
  }

}
