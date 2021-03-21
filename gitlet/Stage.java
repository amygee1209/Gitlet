package gitlet;

import java.io.Serializable;
import java.util.HashMap;

/** Class that stores the added and removed files before commit.
 *  @author Amy Kwon
 */
public class Stage implements Serializable {

    /** Stage constructor that is used to commit the changes. */
    public Stage() {
        _addition = new HashMap<String, Blob>();
        _removal = new HashMap<String, Blob>();
    }
    /** Puts the FILENAME and CHANGE to the corresponding Arraylist. */
    public void add(String fileName, Blob change) {
        _addition.put(fileName, change);
    }

    /** Removes the FILENAME from the corresponding Arraylist. */
    public void removeFromAdd(String fileName) {
        _addition.remove(fileName);
    }

    /** Puts the FILENAME and CHANGE to the corresponding Arraylist. */
    public void remove(String fileName, Blob change) {
        _removal.put(fileName, change);

    }

    /** Removes the FILENAME from the corresponding Arraylist. */
    public void removeFromRemove(String fileName) {
        _removal.remove(fileName);
    }

    /** Overwrites the list in the stage with the given
     * FILENAME and CHANGE. */
    public void overwrite(String fileName, Blob change) {
        if (_addition.containsKey(fileName)) {
            _addition.replace(fileName, change);
        }
    }

    /** Returns the added list. */
    public HashMap<String, Blob> getAddition() {
        return _addition;
    }

    /** Returns the removed list. */
    public HashMap<String, Blob> getRemoval() {
        return _removal;
    }

    /** Added list. */
    private HashMap<String, Blob> _addition;
    /** Removed list. */
    private HashMap<String, Blob> _removal;
}
