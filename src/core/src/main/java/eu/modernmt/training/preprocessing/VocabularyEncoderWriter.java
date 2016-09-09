package eu.modernmt.training.preprocessing;

import eu.modernmt.io.LineWriter;
import eu.modernmt.vocabulary.VocabularyBuilder;
import eu.modernmt.vocabulary.rocksdb.RocksDBVocabulary;

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 22/08/16.
 */
public class VocabularyEncoderWriter extends CorpusWriter {

    private final VocabularyBuilder vocabulary;

    public VocabularyEncoderWriter(File vocabularyOutput) {
        vocabulary = RocksDBVocabulary.newBuilder(vocabularyOutput);
    }

    @Override
    protected void doWrite(String[][] batch, LineWriter writer) throws IOException {
        int[][] encoded = vocabulary.addLines(batch);

        StringBuilder builder = new StringBuilder();
        for (int[] line : encoded) {
            for (int i = 0; i < line.length; i++) {
                if (i > 0)
                    builder.append(' ');
                builder.append(Integer.toUnsignedString(line[i]));
            }
            builder.append('\n');

            writer.writeLine(builder.toString());
            builder.setLength(0);
        }
    }

    @Override
    public void flush() throws IOException {
        vocabulary.build();
    }
}
