package eu.modernmt.processing.string;

/**
 * Created by davide on 04/04/16.
 */
public class TokenHook {

    public enum TokenType {
        Word,
        XML
    }

    protected int startIndex;
    protected int length;
    protected TokenType tokenType;
    protected String processedString;

    public TokenHook(int startIndex, int length, TokenType tokenType) {
        this.startIndex = startIndex;
        this.length = length;
        this.tokenType = tokenType;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getLength() {
        return length;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public String getProcessedString() {
        return processedString;
    }

    @Override
    public String toString() {
        return "TokenHook{" +
                "startIndex=" + startIndex +
                ", length=" + length +
                ", tokenType=" + tokenType +
                ", processedString=" + processedString +
                '}';
    }
}
