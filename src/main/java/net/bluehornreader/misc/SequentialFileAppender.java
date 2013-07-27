/*
Copyright (c) 2013 Marian Ciobanu

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

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