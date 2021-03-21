package gitlet;

import ucb.junit.textui;
import org.junit.Test;
import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Amy Kwon
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(UnitTest.class));
    }

    @Test
    public void timeStampTest() {
        Commit example1 = new Commit("start", null, null);
        System.out.println(example1.getTimestamp());
        Commit example2 = new Commit("add", example1, null);
        System.out.println(example2.getTimestamp());
    }

}

