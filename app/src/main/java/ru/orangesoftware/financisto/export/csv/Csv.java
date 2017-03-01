/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.export.csv;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.Flushable;
import java.util.ArrayList;
import java.util.List;

/**
 * These Csv.Reader and Csv.Writer implementations are based on the following description
 * (since there is no formal specification of CSV format yet):
 *  - http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm
 *  - http://tools.ietf.org/html/rfc4180
 *  - http://en.wikipedia.org/wiki/Comma-separated_values
 *  - http://www.csvreader.com/csv_format.php
 *
 * As far as CSV files are generally used by Excel, the default behavior of Csv.Reader and Csv.Writer
 * corresponds to the Excel behaviour.
 * In order to switch to the proper behaviour :) please use the following properties:
 *  - for Csv.Writer: delimiter will set the custom delimiter (default: semicolon)
 *  - for Csv.Reader: delimiter will set the custom delimiter (default: semicolon);
 *                    preserveSpaces will preserve spaces from being trimmed (default: true);
 *                    ignoreEmptyLines will ignore reading of empty lines (default: false);
 *                    ignoreComments will ignore reading of comments (default: false).
 *
 * Examples of usage:
 *
 * Csv.Writer writer = new Csv.Writer("filename").delimiter(',');
 * writer.comment("example of csv").value("a").value("b").newLine().value("c").close();
 *
 * A piece of code shown above will generate the following CSV file:
 * #example of csv
 * a,b
 * c
 *
 * Csv.Reader reader = new Csv.Reader(new FileReader("filename")).delimiter(',').ignoreComments(true);
 * System.out.println(reader.readLine());
 *
 * A piece of code shown above will print to console the following text (using the CSV file generated earlier):
 * [a, b]
 *
 * @author Konstantin Chapyuk aka c0nst
 * @url http://www.javenue.info
 * @version 1.0
 * @see javenue.csv.CsvTestCase
 */
public class Csv {
    public static class Writer {
        private Appendable appendable;

        private char delimiter = ';';

        private boolean first = true;

        public Writer(String fileName) { this(new File(fileName)); }
        public Writer(File file) {
            try {
                appendable = new FileWriter(file);
            } catch (java.io.IOException e) { throw new IOException(e); }
        }
        public Writer(Appendable appendable) { this.appendable = appendable; }

        public Writer value(String value) {
            if (!first) string("" + delimiter);
            string(escape(value));
            first = false;
            return this;
        }

        public Writer newLine() {
            first = true;
            return string("\n");
        }

        public Writer comment(String comment) {
            if (!first) throw new FormatException("invalid csv: misplaced comment");
            return string("#").string(comment).newLine();
        }

        public Writer flush() {
            try {
                if (appendable instanceof Flushable) {
                    Flushable flushable = (Flushable) appendable;
                    flushable.flush();
                }
            } catch (java.io.IOException e) { throw new IOException(e); }
            return this;
        }

        public void close() {
            try {
                if (appendable instanceof Closeable) {
                    Closeable closeable = (Closeable) appendable;
                    closeable.close();
                }
            } catch (java.io.IOException e) { throw new IOException(e); }
        }

        private Writer string(String s) {
            try {
                appendable.append(s);
            } catch (java.io.IOException e) { throw new IOException(e); }
            return this;
        }

        private String escape(String value) {
            if (value == null) return "";
            if (value.length() == 0) return "\"\"";

            boolean needQuoting = value.startsWith(" ") || value.endsWith(" ") || (value.startsWith("#") && first);
            if (!needQuoting) {
                for (char ch : new char[]{'\"', '\\', '\r', '\n', '\t', delimiter}) {
                    if (value.indexOf(ch) != -1) {
                        needQuoting = true;
                        break;
                    }
                }
            }

            String result = value.replace("\"", "\"\"");
            if (needQuoting) result = "\"" + result + "\"";
            return result;
        }

        public Writer delimiter(char delimiter) { this.delimiter = delimiter; return this; }
    }


    public static class Reader {
        private static final String impossibleString = "$#%^&*!xyxb$#%&*!^";
        private BufferedReader reader;

        private char delimiter = ';';
        private boolean preserveSpaces = true;
        private boolean ignoreEmptyLines = false;
        private boolean ignoreComments = false;

        public Reader(java.io.Reader reader) { this.reader = new BufferedReader(reader); }

        public List<String> readLine() {
            String line;
            try {
                line = reader.readLine();
            } catch (java.io.IOException e) { throw new IOException(e); }
            if (line == null) return null;
            if (!preserveSpaces) line = removeLeadingSpaces(line);
            if (ignoreComments && line.startsWith("#")) return readLine();
            if (ignoreEmptyLines && line.length() == 0) return readLine();

            List<String> result = new ArrayList<String>();

            while (line != null) {
                String token = "";
                int nextDelimiterIndex = line.indexOf(delimiter);
                int openQuoteIndex = line.indexOf("\"");

                if ((nextDelimiterIndex > openQuoteIndex || nextDelimiterIndex == -1) && openQuoteIndex != -1) {
                    token = line.substring(0, openQuoteIndex + 1);
                    line = markDoubleQuotes(line.substring(openQuoteIndex + 1));

                    int closeQuoteIndex = line.indexOf("\"");

                    while (closeQuoteIndex == -1) {
                        token += line + "\n";
                        try {
                            line = reader.readLine();
                        } catch (java.io.IOException e) { throw new IOException(e); }
                        if (line == null) throw new FormatException("invalid csv: premature end of csv");
                        closeQuoteIndex = line.indexOf("\"");
                    }

                    nextDelimiterIndex = line.indexOf(delimiter, closeQuoteIndex);
                }

                if (nextDelimiterIndex == -1) {
                    token += line;
                    line = null;
                } else {
                    token += line.substring(0, nextDelimiterIndex);
                    line = unmarkDoubleQuotes(line.substring(nextDelimiterIndex + 1, line.length()));
                }

                result.add(unescape(token));
            }

            return result;
        }

        public void close() {
            try {
                reader.close();
            } catch (java.io.IOException e) { throw new IOException(e); }
        }

        private String unescape(String s) {
            String result = s;
            if (!preserveSpaces || result.contains("\"")) result = result.trim();
            if (result.startsWith("\"") ^ result.endsWith("\"")) throw new FormatException("invalid csv: misplaced quote");
            if (result.startsWith("\"")) result = result.substring(1, result.length() - 1);
            result = markDoubleQuotes(result);
            if (result.contains("\"")) throw new FormatException("invalid csv: misplaced quote"); // could this ever happen at all?
            result = unmarkDoubleQuotes(result);
            return result;
        }

        private String unmarkDoubleQuotes(String s) { return s.replace(impossibleString, "\"\""); }
        private String markDoubleQuotes(String s) { return s.replace("\"\"", impossibleString); }

        private String removeLeadingSpaces(String s) { return s.replaceFirst(" +", ""); }

        public Reader delimiter(char delimiter) { this.delimiter = delimiter; return this; }
        public Reader preserveSpaces(boolean preserveSpaces) { this.preserveSpaces = preserveSpaces; return this; }
        public Reader ignoreEmptyLines(boolean ignoreEmptyLines) { this.ignoreEmptyLines = ignoreEmptyLines; return this; }
        public Reader ignoreComments(boolean ignoreComments) { this.ignoreComments = ignoreComments; return this; }
    }


    public static class Exception extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public Exception() { }
        public Exception(String message) { super(message); }
        public Exception(String message, Throwable cause) { super(message, cause); }
        public Exception(Throwable cause) { super(cause); }
    }

    public static class IOException extends Exception {
		private static final long serialVersionUID = 1L;
        public IOException() { }
        public IOException(String message) { super(message); }
        public IOException(String message, Throwable cause) { super(message, cause); }
        public IOException(Throwable cause) { super(cause); }
    }

    public static class FormatException extends Exception {
		private static final long serialVersionUID = 1L;
        public FormatException() { }
        public FormatException(String message) { super(message); }
        public FormatException(String message, Throwable cause) { super(message, cause); }
        public FormatException(Throwable cause) { super(cause); }
    }
}
