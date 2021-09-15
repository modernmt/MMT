package eu.modernmt.memory;

import eu.modernmt.data.LogDataListener;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by davide on 23/05/17.
 */
public interface TranslationMemory extends Closeable, LogDataListener {

    class Entry {

        public final String tuid;
        public final long memory;
        public final LanguageDirection language;
        public final String sentence;
        public final String translation;

        public Entry(String tuid, long memory, LanguageDirection language, String sentence, String translation) {
            this.tuid = tuid;
            this.memory = memory;
            this.language = language;
            this.sentence = sentence;
            this.translation = translation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return memory == entry.memory &&
                    Objects.equals(tuid, entry.tuid) &&
                    language.equals(entry.language) &&
                    sentence.equals(entry.sentence) &&
                    translation.equals(entry.translation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tuid, memory, language, sentence, translation);
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "tuid='" + tuid + '\'' +
                    ", memory=" + memory +
                    ", language=" + language +
                    ", sentence='" + sentence + '\'' +
                    ", translation='" + translation + '\'' +
                    '}';
        }
    }

    ScoreEntry[] search(UUID user, LanguageDirection direction, Sentence source, ContextVector contextVector, int limit) throws IOException;

    void optimize() throws IOException;

    long size();

    void dumpAll(Consumer<Entry> consumer) throws IOException;

    void dump(UUID owner, long memory, Consumer<Entry> consumer) throws IOException;

}
