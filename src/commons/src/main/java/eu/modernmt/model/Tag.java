package eu.modernmt.model;

abstract public class Tag extends Token implements Comparable<Tag>, Cloneable {

    /*
        Tag description:
        - an opening tag is expected to have a corresponding closing tag (ex. "<a>")
        - a closing tag is expected to have a corresponding opening tag (ex. "</a>")
        - an empty tag is self-closing (ex. "<a />"), i.e. it is like an atomic opening/closing tag pair
        - a separator tag is an ad-hoc closing tag (ex. "\n"), which has a "virtual" corresponding opening tag at the most-left compliant position in the text

        Note that tags have to satisfy xml requirement that
        - an opening/closing tag pair can contain an other pair or an empty tag (ex. "<a> x y <b> <c/> z </b> </a>" is correct)
        - an empty tag cannot contain any tag
        - two opening/closing tags cannot overlap (ex. "<a> x y <b> z </a> </b>" is not correct)
        - a separator tag could be transformed into a pair:
          ex. "<a> x y \n z</a>" is equivalent to "<a><c> x y </c> z</a>"
          where "<c>" is an hidden string put on the left most position inside the container tag ("<a>" "</a>") and "</c>" is actually "\n"
    */
    public enum Type {
        OPENING_TAG,
        CLOSING_TAG,
        EMPTY_TAG,
        SEPARATOR_TAG,
    }

    protected Type type; /* tag type */
    protected final String name; /* tag name */
    /* position of the word after which the tag is placed; indexes of words start from 0
    e.g. a tag at the beginning of the sentence has position=0
    e.g. a tag at the end of the sentence (of Length words) has position=Length
    */
    protected int position;

    protected Tag(String name, String text, String leftSpace, String rightSpace, int position, Type type) {
        super(text, text, leftSpace, rightSpace);
        this.position = position;
        this.type = type;
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public boolean isSeparatorTag() {
        return this.type == Type.SEPARATOR_TAG;
    }

    public boolean isEmptyTag() {
        return this.type == Type.EMPTY_TAG;
    }

    public boolean isOpeningTag() {
        return this.type == Type.OPENING_TAG;
    }

    public boolean isClosingTag() {
        return this.type == Type.CLOSING_TAG;
    }

    public boolean closes(Tag other) {
        return this.isClosingTag() && other.isOpeningTag() && nameEquals(this.name, other.name);
    }

    public boolean opens(Tag other) {
        return this.isOpeningTag() && other.isClosingTag() && nameEquals(this.name, other.name);
    }

    private static boolean nameEquals(String n1, String n2) {
        if (n1 == null)
            return n2 == null;
        else
            return n1.equals(n2);
    }

    @Override
    public int compareTo(Tag other) {
        return Integer.compare(this.position, other.getPosition());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Tag tag = (Tag) o;

        if (position != tag.position) return false;
        if (type != tag.type) return false;
        return name.equals(tag.name);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + position;
        return result;
    }

    @Override
    public abstract Tag clone();

}

