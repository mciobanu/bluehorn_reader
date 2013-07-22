package net.bluehornreader.misc;


import org.apache.log4j.*;
import org.apache.log4j.spi.*;

import java.io.*;

/**
 * Appender for log4j that renames the previous log sequentially, so each run has its own log
 */
public class SequentialFileAppender extends FileAppender {

    public SequentialFileAppender() {
    }

    public SequentialFileAppender(Layout layout, String filename, boolean append, boolean bufferedIO, int bufferSize) throws IOException {
        super(layout, filename, append, bufferedIO, bufferSize);
    }

    public SequentialFileAppender(Layout layout, String filename, boolean append) throws IOException {
        super(layout, filename, append);
    }

    public SequentialFileAppender(Layout layout, String filename) throws IOException {
        super(layout, filename);
    }

    @Override
    public void activateOptions() {
        if (fileName != null) {
            try {
                renameOldLog();
                setFile(fileName, fileAppend, bufferedIO, bufferSize);
            } catch (Exception e) {
                errorHandler.error("Error while activating log options", e, ErrorCode.FILE_OPEN_FAILURE);
            }
        }
    }

    private void renameOldLog() {
        if (fileName != null) {
            File f = new File(fileName);
            if (!f.exists()) {
                return;
            }

            for (int i = 0;; ++i) {
                File s = new File(String.format("%s.%03d", fileName, i));
                if (!s.exists()) {
                    if (!f.renameTo(s)) {
                        System.err.printf("Failed to rename %s as %s%n", f.getName(), s.getName());
                    }
                    break;
                }
            }
        }
    }
}