package eu.modernmt.xml;

import eu.modernmt.model.*;
import eu.modernmt.processing.xml.XMLTagProjector;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class XMLTagProjectorTest {

    @Test
    public void testOpeningNotEmptyMonotone() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", " "),
                new Word("world", null),
                new Word("!", null),
        }, new Tag[]{
                Tag.fromText("<b>", true, null, 1),
                Tag.fromText("</b>", false, null, 2),
        });

        Translation translation = new Translation(new Word[]{
                new Word("ciao", " "),
                new Word("mondo", null),
                new Word("!", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
        }));

        new XMLTagProjector().project(translation);

        assertEquals("ciao <b>mondo</b>!", translation.toString());
        assertEquals("ciao mondo !", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<b>", true, null, 1),
                Tag.fromText("</b>", false, null, 2),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNotEmptyNonMonotone() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", " "),
                new Word("world", null),
                new Word("!", null),
        }, new Tag[]{
                Tag.fromText("<b>", true, null, 1),
                Tag.fromText("</b>", false, null, 2),
        });

        Translation translation = new Translation(new Word[]{
                new Word("mondo", " "),
                new Word("ciao", null),
                new Word("!", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 1},
                {1, 0},
                {2, 2},
        }));

        new XMLTagProjector().project(translation);

        assertEquals("<b>mondo</b> ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<b>", false, null, 0),
                Tag.fromText("</b>", false, " ", 1),
        }, translation.getTags());
    }

    @Test
    @Ignore
    public void testEmptyTag() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", " "),
                new Word("with", " "),
                new Word("an", " "),
                new Word("empty", " "),
                new Word("tag", " "),
        }, new Tag[]{
                Tag.fromText("<empty/>", true, null, 3),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", " "),
                new Word("con", " "),
                new Word("un", " "),
                new Word("tag", " "),
                new Word("empty", " "),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 1},
                {3, 4},
                {4, 3},
        }));

        new XMLTagProjector().project(translation);

        assertEquals("Esempio con un tag <empty/>empty", translation.toString());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<empty/>", true, null, 4),
        }, translation.getTags());
        assertEquals("Esempio con un tag empty", translation.getStrippedString(false));
    }

    @Test
    public void testOpeningEmptyMonotone() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", " "),
                new Word("world", null),
                new Word("!", null),
        }, new Tag[]{
                Tag.fromText("<g>", true, null, 1),
                Tag.fromText("</g>", false, null, 1),
        });

        Translation translation = new Translation(new Word[]{
                new Word("ciao", " "),
                new Word("mondo", null),
                new Word("!", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
        }));

        new XMLTagProjector().project(translation);

        assertEquals("ciao <g></g>mondo!", translation.toString());
        assertEquals("ciao mondo!", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<g>", true, null, 1),
                Tag.fromText("</g>", false, null, 1),
        }, translation.getTags());
    }

    @Test
    @Ignore
    public void testOpeningEmptyNonMonotone() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", " "),
                new Word("world", null),
                new Word("!", null),
        }, new Tag[]{
                Tag.fromText("<g>", true, null, 1),
                Tag.fromText("</g>", false, null, 1),
        });

        Translation translation = new Translation(new Word[]{
                new Word("mondo", " "),
                new Word("ciao", null),
                new Word("!", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 1},
                {1, 0},
                {2, 2},
        }));

        new XMLTagProjector().project(translation);
        //System.out.println(translation.getSource().toString());
        assertEquals("<g></g>mondo ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<g>", false, null, 0),
                Tag.fromText("</g>", false, null, 0),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNonClosing() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", " "),
                new Word("with", " "),
                new Word("a", " "),
                new Word("malformed", " "),
                new Word("tag", " "),
        }, new Tag[]{
                Tag.fromText("<open>", true, null, 2),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", " "),
                new Word("con", " "),
                new Word("un", " "),
                new Word("tag", " "),
                new Word("malformato", " "),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 4},
                {4, 3},
        }));

        new XMLTagProjector().project(translation);

        assertEquals("Esempio con <open>un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<open>", true, null, 2),
        }, translation.getTags());
    }

    @Test
    public void testClosingNonOpening() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", " "),
                new Word("with", " "),
                new Word("a", " "),
                new Word("malformed", " "),
                new Word("tag", " "),
        }, new Tag[]{
                Tag.fromText("</close>", false, " ", 2),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", " "),
                new Word("con", " "),
                new Word("un", " "),
                new Word("tag", " "),
                new Word("malformato", " "),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 4},
                {4, 3},
        }));

        new XMLTagProjector().project(translation);

        assertEquals("Esempio con</close> un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("</close>", false, " ", 2),
        }, translation.getTags());
    }

    @Test
    public void testEmbeddedTags() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", " "),
                new Word("with", " "),
                new Word("nested", " "),
                new Word("tag", null),
        }, new Tag[]{
                Tag.fromText("<a>", true, null, 1),
                Tag.fromText("<b>", true, null, 3),
                Tag.fromText("</b>", false, " ", 4),
                Tag.fromText("</a>", false, " ", 4),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", " "),
                new Word("con", " "),
                new Word("tag", " "),
                new Word("innestati", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        new XMLTagProjector().project(translation);

        assertEquals("Esempio <a>con <b>tag</b> innestati</a>", translation.toString());
        assertEquals("Esempio con tag innestati", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", true, null, 1),
                Tag.fromText("<b>", true, null, 2),
                Tag.fromText("</b>", false, " ", 3),
                Tag.fromText("</a>", false, null, 4),
        }, translation.getTags());
    }

    @Test
    public void testSpacedXMLCommentTags() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", " "),
                new Word("with", " "),
                new Word("XML", " "),
                new Word("comment", null),
        }, new Tag[]{
                Tag.fromText("<!--", true, " ", 2),
                Tag.fromText("-->", true, null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Esempio", " "),
                new Word("con", " "),
                new Word("commenti", " "),
                new Word("XML", " "),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        new XMLTagProjector().project(translation);

        assertEquals("Esempio con <!-- commenti XML -->", translation.toString());
        assertEquals("Esempio con commenti XML", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!--", true, " ", 2),
                Tag.fromText("-->", true, null, 4),
        }, translation.getTags());
    }

    @Test
    public void testNotSpacedXMLCommentTags() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", " "),
                new Word("with", " "),
                new Word("XML", " "),
                new Word("comment", null),
        }, new Tag[]{
                Tag.fromText("<!--", true, null, 2),
                Tag.fromText("-->", false, null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Esempio", " "),
                new Word("con", " "),
                new Word("commenti", " "),
                new Word("XML", " "),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        new XMLTagProjector().project(translation);

        assertEquals("Esempio con <!--commenti XML-->", translation.toString());
        assertEquals("Esempio con commenti XML", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!--", true, null, 2),
                Tag.fromText("-->", false, null, 4),
        }, translation.getTags());
    }

    @Test
    public void testSingleXMLComment() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("This", " "),
                new Word("is", " "),
                new Word("a", " "),
                new Word("test", null),
        }, new Tag[]{
                Tag.fromText("<!--", false, null, 0),
                Tag.fromText("-->", false, null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Questo", " "),
                new Word("è", " "),
                new Word("un", " "),
                new Word("esempio", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 3},
        }));

        new XMLTagProjector().project(translation);

        assertEquals("<!--Questo è un esempio-->", translation.toString());
        assertEquals("Questo è un esempio", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!--", false, null, 0),
                Tag.fromText("-->", false, null, 4),
        }, translation.getTags());
    }

    @Test
    public void testDTDTags() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Test", null),
        }, new Tag[]{
                Tag.fromText("<!ENTITY key=\"value\">", false, " ", 0),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Prova", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0}
        }));

        new XMLTagProjector().project(translation);

        assertEquals("<!ENTITY key=\"value\"> Prova", translation.toString());
        assertEquals("Prova", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!ENTITY key=\"value\">", false, " ", 0),
        }, translation.getTags());
    }

    @Test
    public void testOnlyTags() throws Throwable {
        Sentence source = new Sentence(null, new Tag[]{
                Tag.fromText("<a>", false, null, 0),
                Tag.fromText("</a>", false, null, 0),
        });

        Translation translation = new Translation(null, source, null);

        new XMLTagProjector().project(translation);

        assertEquals("<a></a>", translation.toString());
        assertTrue(translation.getStrippedString(false).isEmpty());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", false, null, 0),
                Tag.fromText("</a>", false, null, 0),
        }, translation.getTags());
    }

}
