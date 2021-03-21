package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** Class structured as linked list that stores metadata of each commit.
 *  @author Amy Kwon
 */
public class Commit implements Serializable {

    /** Constructor for Commit class that
     * takes in commit MESSAGE, PARENT, and BLOBS.
     * Getting current timestamp (https://tinyurl.com/yasy9ytf). */
    public Commit(String message, Commit parent, HashMap<String, Blob> blobs) {
        _message = message;
        _parent = parent;
        _blobs = blobs;
        _timestamp = "Thu Jan 01 00:00:00 1970 -0800";
        setHash(_message + _timestamp);
    }

    /** A commit whose initial contents and state are copied from
     *  the PARENT commit. */
    Commit(Commit parent) {
        this(null, null, null);
        copyFrom(parent);
    }

    /** Set my state to a copy of the PARENT commit.
     * Referenced LOA's copyFrom method. */
    void copyFrom(Commit parent) {
        _message = parent.getMessage();
        _parent = parent.getParent();
        _blobs = parent.getBlobs();
        SimpleDateFormat time =
                new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
        _timestamp = time.format(new Date());
        _hash = parent.getHash();
    }

    /** Returns the timestamp of the commit. */
    public String getTimestamp() {
        return _timestamp;
    }

    /** Returns the commit message. */
    public String getMessage() {
        return _message;
    }

    /** Returns the parents of the commit. */
    public Commit getParent() {
        return _parent;
    }

    /** Returns the blob of the commit. */
    public HashMap<String, Blob> getBlobs() {
        return _blobs;
    }

    /** Returns the hash of the commit. */
    public String getHash() {
        return _hash;
    }

    /** Set the commit message to MESSAGE. */
    public void setMessage(String message) {
        _message = message;
    }

    /** Set the timestamp. */
    public void setTimestamp() {
        _timestamp = getTimestamp();
    }

    /** Set the commit blob with the given BLOBS. */
    public void setBlobs(HashMap<String, Blob> blobs) {
        _blobs = blobs;
    }

    /** Set the commit parent with the given PARENT. */
    public void setParent(Commit parent) {
        _parent = parent;
    }

    /** Set the commit hash with the given INPUT. */
    public void setHash(String input) {
        _hash = Utils.sha1(input);
    }

    /** Return the Blob of the FILENAME in the commit.
     * Referenced (https://tinyurl.com/yasy9ytf). */
    public Blob findBlob(String fileName) {
        if (_blobs == null) {
            return null;
        }
        for (Map.Entry<String, Blob> entry : _blobs.entrySet()) {
            String file = entry.getKey();
            Blob blob = entry.getValue();
            if (file.equals(fileName)) {
                return blob;
            }
        }
        return null;
    }

    /** Returns true is the REMOTEHEAD existed
     * in the past commits of this commit. */
    public boolean hasHistory(Commit remoteHead) {
        Commit pointer = this;
        while (pointer != null) {
            if (pointer.getHash().equals(remoteHead.getHash())) {
                return true;
            }
            pointer = pointer.getParent();
        }
        return false;
    }

    /** Commit message. */
    private String _message;
    /** Commit timestamp. */
    private String _timestamp;
    /** Commit parent. */
    private Commit _parent;
    /** Commit blobs. */
    private HashMap<String, Blob> _blobs;
    /** Commit hash. */
    private String _hash;

}
