package eu.modernmt.context.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by davide on 22/09/15.
 */
public class IDFTable {

    private ConcurrentHashMap<String, Float> cache;
    private IndexReader indexReader;
    private String fieldId;
    private int numDocs;

    public IDFTable(IndexReader indexReader, String fieldId) {
        this.indexReader = indexReader;
        this.fieldId = fieldId;
        this.numDocs = indexReader.numDocs();
        this.cache = new ConcurrentHashMap<>();
    }

    public float getTFIDF(String term, int tf) throws IOException {
        return getTFIDF(new Term(fieldId, term), tf);
    }

    public float getTFIDF(BytesRef term, int tf) throws IOException {
        return getTFIDF(new Term(fieldId, term), tf);
    }

    public float getTFIDF(Term term, int tf) throws IOException {
        String text = term.text();
        Float idf = this.cache.get(text);

        if (idf == null) {
            synchronized (this) {
                idf = this.cache.get(text);

                if (idf == null) {
                    long df = this.indexReader.docFreq(term);
                    idf = idf(numDocs, df);
                    this.cache.putIfAbsent(text, idf);
                }
            }
        }

        return tf(tf) * idf;
    }

    private static float tf(int freq) {
        return (float) Math.sqrt(freq);
    }

    private static float idf(long numDocs, long docFreq) {
        return (float) (Math.log(numDocs / (double) (docFreq + 1)) + 1.0);
    }
}
