package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private void captureStdout() {
        System.setOut(new PrintStream(out));
    }

    private String stdout() {
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
        out.reset();
    }

    private static String[][] samplePeople() {
        // N=4, 3 columns: first last email
        return new String[][]{
                {"John", "Smith", "john.smith@mail.com"},
                {"Jane", "Doe", "jane@mail.com"},
                {"John", "Doe", ""},                 // email empty
                {"Alice", "Johnson", null}           // email null
        };
    }

    @SafeVarargs
    private static <T> Set<T> setOf(T... items) {
        return new HashSet<>(Arrays.asList(items));
    }

    // -------------------------
    // buildIndex()
    // -------------------------

    @Test
    void buildIndex_indexesWordsAcrossAll3Fields_caseInsensitive() {
        String[][] inp = samplePeople();
        Map<String, Set<Integer>> index = App.buildIndex(inp, 4);

        // "john" appears in row 0 and row 2
        assertEquals(setOf(0, 2), index.get("john"));

        // "doe" appears in row 1 and row 2
        assertEquals(setOf(1, 2), index.get("doe"));

        // email words split by space only, so whole email token should be indexed as one word
        assertTrue(index.containsKey("john.smith@mail.com"));
        assertEquals(setOf(0), index.get("john.smith@mail.com"));
    }

    @Test
    void buildIndex_skipsNullOrEmptyFields() {
        String[][] inp = samplePeople();
        Map<String, Set<Integer>> index = App.buildIndex(inp, 4);

        // row 2 email is "", row 3 email is null: should not create empty-key
        assertFalse(index.containsKey(""));
        assertFalse(index.containsKey(null)); // map can't contain null key in our construction anyway

        // But row 3 names should still be indexed
        assertEquals(setOf(3), index.get("alice"));
        assertEquals(setOf(3), index.get("johnson"));
    }

    // -------------------------
    // getDataFileName()
    // -------------------------

    @Test
    void getDataFileName_returnsValueAfterDataFlag() {
        String[] args = {"--data", "people.txt"};
        assertEquals("people.txt", App.getDataFileName(args));
    }

    @Test
    void getDataFileName_returnsNullWhenMissingFlagOrValue() {
        assertNull(App.getDataFileName(new String[]{}));
        assertNull(App.getDataFileName(new String[]{"--other", "x"}));
        assertNull(App.getDataFileName(new String[]{"--data"})); // no filename after --data
    }

    // -------------------------
    // Strategy helpers: ANY / ALL / NONE
    // -------------------------

    private static Map<String, Set<Integer>> buildSampleIndex() {
        String[][] inp = samplePeople();
        return App.buildIndex(inp, 4);
    }

    @Test
    void getAnyMatches_returnsUnionOfWordMatches_ignoresExtraSpacesAndCase() {
        Map<String, Set<Integer>> index = buildSampleIndex();

        // "john" -> {0,2}, "doe" -> {1,2}, union => {0,1,2}
        Set<Integer> res = App.getAnyMatches("  JoHn   DoE  ", index);

        assertEquals(setOf(0, 1, 2), res);
    }

    @Test
    void getAnyMatches_returnsEmptyWhenNoWordsMatch() {
        Map<String, Set<Integer>> index = buildSampleIndex();
        assertEquals(setOf(), App.getAnyMatches("notfound", index));
    }

    @Test
    void getAllMatches_returnsIntersectionAcrossWords() {
        Map<String, Set<Integer>> index = buildSampleIndex();

        // "john" -> {0,2}, "doe" -> {1,2}, intersection => {2}
        Set<Integer> res = App.getAllMatches("john doe", index);

        assertEquals(setOf(2), res);
    }

    @Test
    void getAllMatches_returnsEmptyIfAnyWordMissing() {
        Map<String, Set<Integer>> index = buildSampleIndex();
        assertEquals(setOf(), App.getAllMatches("john missingword", index));
    }

    @Test
    void getAllMatches_returnsEmptyOnBlankQuery() {
        Map<String, Set<Integer>> index = buildSampleIndex();
        assertEquals(setOf(), App.getAllMatches("   ", index));
    }

    @Test
    void getNoneMatches_returnsComplementOfAnyMatches_over0toNminus1() {
        Map<String, Set<Integer>> index = buildSampleIndex();

        // ANY("john") -> {0,2} ; N=4 => NONE => {1,3}
        Set<Integer> res = App.getNoneMatches("john", index, 4);

        assertEquals(setOf(1, 3), res);
    }

    // -------------------------
    // printRow() / printPpl()
    // -------------------------

    @Test
    void printRow_printsWithoutEmail_whenEmailNullOrEmpty() {
        captureStdout();
        String[][] inp = samplePeople();

        App.printRow(2, inp); // email ""
        App.printRow(3, inp); // email null

        String outText = stdout();
        assertTrue(outText.contains("John Doe"));
        assertTrue(outText.contains("Alice Johnson"));

        // Ensure it didn't accidentally print "null"
        assertFalse(outText.contains("null"));
    }

    @Test
    void printRow_printsWithEmail_whenEmailPresent() {
        captureStdout();
        String[][] inp = samplePeople();

        App.printRow(0, inp);

        String outText = stdout().trim();
        assertEquals("John Smith john.smith@mail.com", outText);
    }

    @Test
    void printPpl_printsHeaderAndAllRows() {
        captureStdout();
        String[][] inp = samplePeople();

        App.printPpl(inp, 4);

        String outText = stdout();
        assertTrue(outText.contains("=== List of people ==="));

        // Rough checks that each person appears
        assertTrue(outText.contains("John Smith john.smith@mail.com"));
        assertTrue(outText.contains("Jane Doe jane@mail.com"));
        assertTrue(outText.contains("John Doe"));
        assertTrue(outText.contains("Alice Johnson"));
    }
}
