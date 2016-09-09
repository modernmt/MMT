package eu.modernmt.model.corpus.impl.parallel;

import eu.modernmt.io.DefaultCharset;
import eu.modernmt.io.LineWriter;
import eu.modernmt.io.UnixLineReader;
import eu.modernmt.io.UnixLineWriter;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Locale;

/**
 * Created by davide on 24/02/16.
 */
public class ParallelFileCorpus implements BilingualCorpus {

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private final File source;
    private final File target;
    private final String name;
    private final Locale sourceLanguage;
    private final Locale targetLanguage;
    private int lineCount = -1;

    private final FileCorpus sourceCorpus;
    private final FileCorpus targetCorpus;

    public ParallelFileCorpus(File directory, String name, Locale sourceLanguage, Locale targetLanguage) {
        this(name, sourceLanguage, new File(directory, name + "." + sourceLanguage.toLanguageTag()), targetLanguage, new File(directory, name + "." + targetLanguage.toLanguageTag()));
    }

    public ParallelFileCorpus(Locale sourceLanguage, File source, Locale targetLanguage, File target) {
        this(FilenameUtils.removeExtension(source.getName()), sourceLanguage, source, targetLanguage, target);
    }

    public ParallelFileCorpus(String name, Locale sourceLanguage, File source, Locale targetLanguage, File target) {
        this.name = name;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.source = source;
        this.target = target;

        this.sourceCorpus = new FileCorpus(this.source, this.name, this.sourceLanguage);
        this.targetCorpus = new FileCorpus(this.target, this.name, this.targetLanguage);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Locale getSourceLanguage() {
        return sourceLanguage;
    }

    @Override
    public Locale getTargetLanguage() {
        return targetLanguage;
    }

    @Override
    public int getLineCount() throws IOException {
        if (lineCount < 0) {
            synchronized (this) {
                if (lineCount < 0) {
                    FileInputStream stream = null;

                    try {
                        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                        stream = new FileInputStream(this.source);

                        int count = 0;
                        int size;

                        while ((size = stream.read(buffer)) != -1) {
                            for (int i = 0; i < size; i++) {
                                if (buffer[i] == (byte) 0x0A)
                                    count++;
                            }
                        }

                        this.lineCount = count;
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                }
            }
        }

        return this.lineCount;
    }

    @Override
    public BilingualLineReader getContentReader() throws IOException {
        return new ParallelFileLineReader(source, target);
    }

    @Override
    public BilingualLineWriter getContentWriter(boolean append) throws IOException {
        return new ParallelFileLineWriter(append, source, target);
    }

    @Override
    public Corpus getSourceCorpus() {
        return sourceCorpus;
    }

    @Override
    public Corpus getTargetCorpus() {
        return targetCorpus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParallelFileCorpus that = (ParallelFileCorpus) o;

        if (!source.equals(that.source)) return false;
        return target.equals(that.target);

    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + target.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return name + '.' + sourceLanguage.toLanguageTag() + '|' + targetLanguage.toLanguageTag();
    }

    private static class ParallelFileLineReader implements BilingualLineReader {

        private UnixLineReader sourceReader;
        private UnixLineReader targetReader;

        private ParallelFileLineReader(File source, File target) throws FileNotFoundException {
            boolean success = false;

            try {
                this.sourceReader = new UnixLineReader(new InputStreamReader(new FileInputStream(source), DefaultCharset.get()));
                this.targetReader = new UnixLineReader(new InputStreamReader(new FileInputStream(target), DefaultCharset.get()));

                success = true;

            } finally {
                if (!success)
                    this.close();
            }
        }

        @Override
        public StringPair read() throws IOException {
            String source = sourceReader.readLine();
            String target = targetReader.readLine();

            if (source == null || target == null)
                return null;

            return new StringPair(source, target);
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(this.sourceReader);
            IOUtils.closeQuietly(this.targetReader);
        }
    }

    private static class ParallelFileLineWriter implements BilingualLineWriter {

        private LineWriter sourceWriter;
        private LineWriter targetWriter;

        private ParallelFileLineWriter(boolean append, File source, File target) throws IOException {
            boolean success = false;

            try {
                this.sourceWriter = new UnixLineWriter(new FileOutputStream(source, append), DefaultCharset.get());
                this.targetWriter = new UnixLineWriter(new FileOutputStream(target, append), DefaultCharset.get());

                success = true;
            } finally {
                if (!success)
                    this.close();
            }
        }

        @Override
        public void write(String source, String target) throws IOException {
            sourceWriter.writeLine(source);
            targetWriter.writeLine(target);
        }

        @Override
        public void write(StringPair pair) throws IOException {
            write(pair.source, pair.target);
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(this.sourceWriter);
            IOUtils.closeQuietly(this.targetWriter);
        }

    }
}
