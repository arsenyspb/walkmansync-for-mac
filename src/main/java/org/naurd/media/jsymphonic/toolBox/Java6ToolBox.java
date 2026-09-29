/*
 * Copyright (C) 2007, 2008, 2009 Patrick Balleux, Nicolas Cardoso De Castro
 * (nicolas_cardoso@users.sourceforge.net), Daniel Žalar (danizmax@gmail.com)
 *
 * This file is part of JSymphonic program.
 *
 * JSymphonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JSymphonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JSymphonic. If not, see <http://www.gnu.org/licenses/>.
 *
 *****
 *
 * Java6ToolBox.java
 *
 */

package org.naurd.media.jsymphonic.toolBox;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
Provides hopefully compatible functions on Java 5 for Java 6 features.
Where Java 6 is available, the Java 6 function provided is used.
Reflection is used to ensure that the code will compile and run on Java 5.
*/
public class Java6ToolBox {
    private static boolean java6Plus;
    private static Method normalizeMethod;
    private static Object normalizeMethodSecondArg; // NFC or COMPOSE
    private static Object normalizeMethodThirdArg; // null or 0
    private static Method getFreeSpaceMethod = null;
    private static Method getUsableSpaceMethod = null;
    private static Method getTotalSpaceMethod = null;
    private static boolean dfAvailable = true; // until proven otherwise

    //Other
    private static Logger logger = Logger.getLogger("org.naurd.media.jsymphonic.toolBox.Java6ToolBox");


    static {
        // In Java 6, Sun made their previously hidden Normalizer class
        // public, but changed the signature a little.
        // Look for the Java 6 class first.
        Class<?> normalizeClass;
        try {
            normalizeClass = Class.forName("java.text.Normalizer");
            java6Plus = true;
            // Signature: String normalize(String str, Normalizer.Form form)
            normalizeMethodSecondArg = Class.forName("java.text.Normalizer$Form").getField("NFC").get(null);
            normalizeMethodThirdArg = null;
            normalizeMethod = normalizeClass.getMethod("normalize", new Class<?>[] { CharSequence.class, normalizeMethodSecondArg.getClass() });

            // Also have File methods available
            getFreeSpaceMethod = File.class.getMethod("getFreeSpace", (Class[]) null);
            getUsableSpaceMethod = File.class.getMethod("getUsableSpace", (Class[]) null);
            getTotalSpaceMethod = File.class.getMethod("getTotalSpace", (Class[]) null);
        } catch (Exception je) {
            // Look for the hidden class instead, which is not guaranteed
            // to be found but almost certainly will be on Java 5.
            try {
                normalizeClass = Class.forName("sun.text.Normalizer");
                java6Plus = false;
                // Signature: String normalize(String str, Normalizer.Mode mode, int i)
                normalizeMethodSecondArg = normalizeClass.getField("COMPOSE").get(null);
                normalizeMethodThirdArg = new Integer(0);
                normalizeMethod = normalizeClass.getMethod("normalize", new Class<?>[] { String.class, Class.forName("sun.text.Normalizer$Mode"), int.class });
            } catch (Exception se) {
                handleException(se);
            }
        }
    }

    /**
    Normalize a string such that characters are composed.
    On Java 6 and above, equivalent to: <tt>java.text.Normalizer(str, java.text.Normalizer.Form.NFC)</tt>
    On Java 5 and lower, equivalent to: <tt>sun.text.Normalizer(str, sun.text.Normalizer.COMPOSE, 0)</tt>
    The sun Normalizer class has been around since Java 1.4 if not earlier,
    but was removed in Java 6 in favour of the new public class.
    */
    public static String normalizeNFC(String str) {
        try {
            Object[] args;
            if (java6Plus) {
                args = new Object[] { str, normalizeMethodSecondArg };
            } else {
                args = new Object[] { str, normalizeMethodSecondArg, normalizeMethodThirdArg };
            }
            return (String) normalizeMethod.invoke(null, args);
        } catch (Exception e) {
            handleException(e);
            return null; // not reached
        }
    }

    /** Holds freeSpace, usableSpace and totalSpace, usually for a filesystem. */
    public static class FileSpaceInfo {
        private long freeSpace = 0L;
        private long usableSpace = 0L;
        private long totalSpace = 0L;

        public long getFreeSpace() { return freeSpace; }
        public void setFreeSpace(long freeSpace) { this.freeSpace = freeSpace; }
        public long getUsableSpace() { return usableSpace; }
        public void setUsableSpace(long usableSpace) { this.usableSpace = usableSpace; }
        public long getTotalSpace() { return totalSpace; }
        public void setTotalSpace(long totalSpace) { this.totalSpace = totalSpace; }
        @Override public String toString() {
            return "freeSpace = " + freeSpace + ", usableSpace = " + usableSpace + ", totalSpace = " + totalSpace;
        }
    }

    /**
    Return free space, usable space and total space for the given File.
    On Java 6 and above, this is achieved by calling file.getFreeSpace(),
    file.getUsableSpace() and file.getTotalSpace().
    On Java 5 and below, the values are derived from shelling out to the
    "df" command, if possible. Clearly, this is unlikely to work on Windows,
    but Windows users should have little trouble obtaining Java 6.
    */
    public static FileSpaceInfo getFileSpaceInfo(File file) {
        try {
            if (java6Plus) {
                FileSpaceInfo info = new FileSpaceInfo();
                info.setFreeSpace(((Long) getFreeSpaceMethod.invoke(file, (Object[]) null)).longValue());
                info.setUsableSpace(((Long) getUsableSpaceMethod.invoke(file, (Object[]) null)).longValue());
                info.setTotalSpace(((Long) getTotalSpaceMethod.invoke(file, (Object[]) null)).longValue());
                return info;
            } else {
                return dfAvailable ? getFileSpaceInfoFromDF(file) : null;
            }
        } catch (Exception e) {
            handleException(e);
            return null; // not reached
        }
    }

    private static FileSpaceInfo getFileSpaceInfoFromDF(File file) throws Exception {
        // Shell out to "df".
        // Use -k so that we always get the numbers in 1K blocks.
        // The default format without -k varies between platforms.
        // We can live without the extra accuracy.
        // Output should be something like:
        //
        // Filesystem  1K-blocks  Used Available Use% Mounted on
        // /dev/sda1        1024   256       768  25% /media/disk
        //
        // but a long filesystem name could split the output onto
        // a third line, so just look for the numbers.
        Process proc;
        try {
            proc = Runtime.getRuntime().exec(new String[] { "df", "-k", file.getPath() });
        } catch (java.io.IOException ioe) {
            dfError("Could not execute \"df\" - " + ioe.getMessage());
            return null;
        }

        final StringBuffer outputBuffer = new StringBuffer();
        final StringBuffer errorBuffer = new StringBuffer();
        try {
            // Perform all the usual paranoia over a shell-out to make sure
            // we don't hang the process. STDIN shouldn't be a problem, but
            // we must exhaust both STDOUT and STDERR.
            final InputStream stdout = proc.getInputStream();
            final InputStream stderr = proc.getErrorStream();
            Thread stdoutThread = new Thread() {
                public void run() {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // Look for numeric percentage
                            if (line.matches("^.*\\d+%\\s+.*$")) {
                                outputBuffer.append(line);
                            }
                        }
                    } catch (Exception e) {
                        // Should probably log this somehow
                    }
                }
            };
            Thread stderrThread = new Thread() {
                public void run() {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stderr));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // Save everything, not that we expect anything.
                            errorBuffer.append(line);
                            errorBuffer.append('\n');
                        }
                    } catch (Exception e) {
                        // Should probably log this somehow
                    }
                }
            };
            stdoutThread.start();
            stderrThread.start();
            try {
                // should probably check return value against zero
                proc.waitFor();
            } catch (InterruptedException e) {
                proc.destroy();
                throw e;
            }
            stdoutThread.join();
            stderrThread.join();
        } finally {
            // Make absolutely sure we don't leave anything open
            try {
                proc.getInputStream().close();
            } finally {
                try {
                    proc.getOutputStream().close();
                } finally {
                    proc.getErrorStream().close();
                }
            }
        }
        // Assume that output to stderr implies an error
        if (errorBuffer.length() != 0) {
            dfError("Error while running \"df\" - " + errorBuffer);
            return null;
        }
        // Can now chew on the output. Capture the first three numbers.
        Matcher matcher = Pattern.compile("^.*\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+.*$").matcher(outputBuffer.toString());
        if (!matcher.matches()) {
            dfError("Unexpected output from \"df\" - " + outputBuffer);
            return null;
        }
        // $1 = total disk KB blocks, $2 = actual KB used, $3 = KB available
        // $1 does not necessarily equal $2 + $3
        FileSpaceInfo info = new FileSpaceInfo();
        info.setTotalSpace(1024L * Long.parseLong(matcher.group(1)));
        info.setFreeSpace(info.getTotalSpace() - 1024L * Long.parseLong(matcher.group(2)));
        info.setUsableSpace(1024L * Long.parseLong(matcher.group(3)));
        return info;
    }

    /**
    Write an error about "df" to stdout but show a simpler error in the GUI.
    Assume that the GUI user would prefer to install Java 6 than find a working df.
    */
    private static void dfError(String message) {
        dfAvailable = false; // We only try once
        logger.warning(message + " - use Java 6 or greater to avoid needing \"df\" at all");
        javax.swing.JOptionPane.showMessageDialog(
            null,
            "Cannot check disk space",
            "Warning",
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
    }

    /** Unravel exceptions as much as possible */
    private static void handleException(Throwable e) {
        if (e instanceof InvocationTargetException) {
            handleException(((InvocationTargetException) e).getTargetException());
        } else if (e instanceof Error) {
            throw (Error) e;
        } else if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new RuntimeException(e);
        }
    }

/*    public static void main(String[] args) {
        // Some brief tests
        System.out.println("FileSpaceInfo for current directory:");
        FileSpaceInfo info = getFileSpaceInfo(new File("."));
        System.out.println(info.toString());

        String decomp = "A\u0301"; // captial A acute, decomposed
        String comp = "\u00c1"; // captial A acute, composed
        compTest(decomp, comp);
        compTest(comp, comp);
    }
    private static void compTest(String decomp, String comp) {
        String calcComp = normalizeNFC(decomp);
        String matches = calcComp.equals(comp) ? "correct" : "wrong";
        System.out.println("\"" + decomp + "\" composes to \"" + calcComp + "\" (" + matches + ")");
    }*/
}

