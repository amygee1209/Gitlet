package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/** Class that stores the name and head commit of each branch.
 *  @author Amy Kwon
 */
public class Branch implements Serializable {

    /** Branch constructor that stores the NAME and HEAD commit. */
    public Branch(String name, Commit head) {
        _name = name;
        _head = head;
        _removed = new HashMap<String, Blob>();
    }

    /** Returns the name of the branch. */
    public String name() {
        return _name;
    }

    /** Returns the head commit of the branch. */
    public Commit head() {
        return _head;
    }

    /** Set the head commit of the branch with the given COMMIT. */
    public void setHead(Commit commit) {
        _head = commit;
    }

    /** Returns all the hashs the branch has. */
    public ArrayList<String> getAllHashs() {
        ArrayList<String> allHash = new ArrayList<String>();
        Commit pointer = _head;
        while (pointer != null) {
            String hash = pointer.getHash();
            allHash.add(hash);
            pointer = pointer.getParent();
        }
        return allHash;
    }

    /** Returns all the removed files in the branch. */
    public HashMap<String, Blob> removed() {
        return _removed;
    }

    /** Add the FILENAME and BLOB to the removed list. */
    public void addtoRemoved(String fileName, Blob blob) {
        _removed.put(fileName, blob);
    }

    /** Add the FILENAME to the removed list. */
    public void removefromRemove(String fileName) {
        _removed.remove(fileName);
    }

    /** Name of the branch. */
    private String _name;
    /** Head commit of the branch. */
    private Commit _head;
    /** All removed files within the branch. */
    private HashMap<String, Blob> _removed;
}
