package gitlet;

import java.io.File;
import java.io.Serializable;

/** Class that stores the hash id and the content of the file.
 *  @author Amy Kwon
 */
public class Blob implements Serializable {

    /** Blob constructor that takes in file
     * and stores hash id and content of the FILE.
     */
    public Blob(File file) {
        String content = Utils.readContentsAsString(file);
        _hash = Utils.sha1(file.getName() + content);
        _content = content;
    }

    /** Returns true if the OTHER blob
     * and this blob has the same blob hash. */
    public boolean sameContent(Blob other) {
        return this.hash().equals(other.hash());
    }

    /** Returns hash of the blob. */
    public String hash() {
        return _hash;
    }

    /** Returns content of the blob. */
    public String content() {
        return _content;
    }

    /** Hash of the blob. */
    private String _hash;
    /** Content of the blob. */
    private String _content;
}
