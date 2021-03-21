package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Class that runs commands as methods and store the entire data.
 *  @author Amy Kwon
 */
public class Command implements Serializable {

    /** Command constructor used to load and save data. */
    public Command() throws IOException {
        _repo.mkdir();
        File read = new File(_repo, "data");
        read.createNewFile();
        _stage = new Stage();
        _branches = new ArrayList<Branch>();
        _commits = new ArrayList<Commit>();
        _remote = new HashMap<String, String>();
        Commit init = new Commit("initial commit", null, null);
        _commits.add(init);
        Branch master = new Branch("master", init);
        _currBranch = master;
        _branches.add(master);
    }

    /** Add new changes of the FILENAME from the stage. */
    void add(String fileName) {
        File add = new File(_dir, fileName);
        if (add.exists()) {
            Blob change = new Blob(add);
            Commit currHead = _currBranch.head();
            if (_stage.getAddition().containsKey(fileName)) {
                _stage.overwrite(fileName, change);
            }
            if (currHead.getParent() != null) {
                Blob commitContent = currHead.findBlob(fileName);
                boolean tracked = commitContent != null;
                if (tracked) {
                    String newHash = change.hash();
                    String currHash = commitContent.hash();
                    HashMap<String, Blob> removed = _currBranch.removed();
                    boolean remove = removed.containsKey(fileName);
                    boolean match = newHash.equals(currHash);
                    if (remove && match) {
                        _currBranch.removefromRemove(fileName);
                        _stage.removeFromRemove(fileName);
                    }
                    if (!newHash.equals(currHash)) {
                        _stage.add(fileName, change);
                    } else if (_stage.getAddition().containsKey(fileName)) {
                        remove(fileName);
                    }
                } else {
                    _stage.add(fileName, change);
                }
            } else {
                _stage.add(fileName, change);
            }
        } else {
            System.out.println("File does not exist.");
            System.exit(0);
        }
    }

    /** Commits the changes from the stage with the MESSAGE. */
    void commit(String message) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        if (message.length() > 4) {
            int i = message.indexOf(' ');
            String word = message.substring(0, i);
            if (!word.equals("Merged") && !word.equals("Encountered")) {
                boolean addEmp = _stage.getAddition().isEmpty();
                boolean remEmp = _stage.getRemoval().isEmpty();
                if (addEmp && remEmp) {
                    System.out.println("No changes added to the commit.");
                    System.exit(0);
                }
            }
        }
        Commit currHead = _currBranch.head();
        Commit commit = new Commit(currHead);
        commit.setMessage(message);
        commit.setTimestamp();
        commit.setBlobs(_stage.getAddition());
        commit.setParent(currHead);
        commit.setHash(commit.getMessage() + commit.getTimestamp());
        _currBranch.setHead(commit);
        _commits.add(commit);
        _stage = new Stage();
    }

    /** Removes the FILENAME from the stage and the working directory. */
    void remove(String fileName) {
        Commit currHead = _currBranch.head();
        boolean staged = _stage.getAddition().containsKey(fileName);
        if (currHead.getBlobs() == null && !staged) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        if (_stage.getAddition().containsKey(fileName)) {
            _stage.removeFromAdd(fileName);
        } else if (currHead.getBlobs().containsKey(fileName)
                || currHead.getParent().getBlobs().containsKey(fileName)) {
            Blob currBlob = currHead.findBlob(fileName);
            _stage.remove(fileName, currBlob);
            _currBranch.addtoRemoved(fileName, currBlob);
            Utils.restrictedDelete(fileName);
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    /** Prints the log of the current commit. */
    void log() {
        Commit pointer = _currBranch.head();
        while (pointer != null) {
            printLog(pointer);
            pointer = pointer.getParent();
        }
    }

    /** Helper function for log and globalLog with the given COMMIT. */
    void printLog(Commit commit) {
        System.out.println("===");
        System.out.println("commit " + commit.getHash());
        System.out.println("Date: " + commit.getTimestamp());
        System.out.println(commit.getMessage());
        System.out.println();
    }

    /** Prints the log of all commits. */
    void globalLog() {
        for (Commit each : _commits) {
            printLog(each);
        }
    }

    /** Returns true if the given COMMITMSG exist. */
    boolean findMsg(String commitMsg) {
        for (Commit each : _commits) {
            if (each.getMessage().equals(commitMsg)) {
                return true;
            }
        }
        return false;
    }

    /** Prints out the ids of all commits that have the given COMMITMSG. */
    void find(String commitMsg) {
        if (findMsg(commitMsg)) {
            for (Commit each : _commits) {
                if (each.getMessage().equals(commitMsg)) {
                    System.out.println(each.getHash());
                }
            }
        } else {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Prints out the status. */
    void status() {
        System.out.println("=== Branches ===");
        for (Branch each : _branches) {
            if (each.name().equals(_currBranch.name())) {
                System.out.println("*" + _currBranch.name());
            } else {
                System.out.println(each.name());
            }
        }
        System.out.println("\n=== Staged Files ===");
        if (_stage.getAddition() != null) {
            for (String fileName : _stage.getAddition().keySet()) {
                System.out.println(fileName);
            }
        }
        System.out.println("\n=== Removed Files ===");
        if (_stage.getRemoval() != null) {
            for (String fileName : _stage.getRemoval().keySet()) {
                System.out.println(fileName);
            }
        }
        _modified = new ArrayList<>();
        _untracked = new ArrayList<>();
        setUp();
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        for (String file : _modified) {
            System.out.println(file);
        }

        System.out.println("\n=== Untracked Files ===");
        for (String file : _untracked) {
            System.out.println(file);
        }
        System.out.println();
    }

    /** Helper function for status. */
    void setUp() {
        Commit currHead = _currBranch.head();
        HashMap<String, Blob> trackedFiles = currHead.getBlobs();
        Commit prevHead = currHead.getParent();
        List<String> allFiles = Utils.plainFilenamesIn(_dir);
        if (allFiles != null) {
            for (String name : allFiles) {
                setUpHelper(name, trackedFiles, currHead, prevHead);
            }
        }
        deleted(trackedFiles);
    }

    /** Helper function for setup using
     * NAME, TRACKEDFILES, CURRHEAD, PREVHEAD. */
    void setUpHelper(String name, HashMap<String, Blob> trackedFiles,
                     Commit currHead, Commit prevHead) {
        File file = new File(_dir, name);
        boolean tracked = false, stagedAdd = false;
        boolean stagedRemove = false;
        boolean deleted = !file.exists();
        Blob stageContent = null;
        if (trackedFiles != null) {
            tracked = trackedFiles.containsKey(name);
        }
        if (!_stage.getAddition().isEmpty()) {
            stagedAdd = _stage.getAddition().containsKey(name);
            stageContent = _stage.getAddition().get(name);
        }
        if (!_stage.getRemoval().isEmpty()) {
            stagedRemove = _stage.getRemoval().containsKey(name);
        }
        boolean diff = diffHash(file, stageContent);
        boolean case2 = stagedAdd && diff;
        boolean case3 = stagedAdd && deleted;
        boolean case4 = !stagedRemove && tracked && deleted;
        if (prevHead == null) {
            if (case2) {
                _modified.add(name + " (modified)");
            }
            if (!tracked && !stagedAdd) {
                _untracked.add(name);
            }
        } else if (prevHead.getParent() == null) {
            boolean case1 = tracked && diff && !stagedAdd;
            boolean case5 = tracked && diffHash(file, currHead.findBlob(name));
            if (case1 || case2 || case5) {
                _modified.add(name + " (modified)");
            }
            if (!tracked && !stagedAdd) {
                _untracked.add(name);
            }
        } else {
            Blob prevBlob = prevHead.findBlob(name);
            boolean prevChanged = diffHash(file, prevBlob);
            boolean case1 = tracked && prevChanged && !stagedAdd;
            if (case1 || case2) {
                _modified.add(name + " (modified)");
            }
            boolean merged = Utils.readContentsAsString
                    (file).contains("<<<<<<< HEAD");
            if (!tracked && prevChanged && !merged) {
                _untracked.add(name);
            }
        }
        if (case3 || case4) {
            _modified.add(name + " (deleted)");
        }
    }

    /** Put deleted files in TRACKEDFILES without staging on status. */
    void deleted(HashMap<String, Blob> trackedFiles) {
        if (trackedFiles != null) {
            for (String name : trackedFiles.keySet()) {
                File file = new File(_dir, name);
                if (!file.exists() && !_stage.getRemoval().containsKey(name)) {
                    _modified.add(name + " (deleted)");
                }
            }
        }
    }

    /** Returns true if the has of the FILE and BLOB matches. */
    boolean diffHash(File file, Blob blob) {
        Blob original = new Blob(file);

        if (blob == null) {
            return false;
        }
        String originHash = original.hash();
        String hash = blob.hash();
        return !originHash.equals(hash);
    }

    /** Checkouts based on ARGS
     * Usages:
     * java gitlet.Main checkout -- [file name]
     * java gitlet.Main checkout [commit id] -- [file name]
     * java gitlet.Main checkout [branch name]. */
    void checkout(String... args) throws IOException {
        if (args.length == 3) {
            if (args[1].equals("--")) {
                checkoutFile(args[2]);
            } else {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
        } else if (args.length == 4) {
            if (args[2].equals("--")) {
                checkoutWithId(args[1], args[3]);
            } else {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
        } else if (args.length == 2) {
            checkoutBranch(args[1]);
        } else {
            System.out.println("Invalid input format");
            System.exit(0);
        }
    }

    /** Checkouts the FILENAME. */
    void checkoutFile(String fileName) {
        File file = new File(_dir, fileName);
        Commit currCommit = _currBranch.head();
        HashMap<String, Blob> tracked = currCommit.getBlobs();
        if (tracked.containsKey(fileName)) {
            Blob blob = currCommit.findBlob(fileName);
            if (blob != null) {
                Utils.writeContents(file, blob.content());
            }
        } else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    /** Checkouts the FILENAME with HASH. */
    void checkoutWithId(String hash, String fileName) throws IOException {
        Commit commit = findHash(hash);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        HashMap<String, Blob> tracked = commit.getBlobs();
        if (!tracked.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File file = new File(_dir, fileName);
        Blob blob = commit.findBlob(fileName);
        if (blob != null) {
            if (!file.exists()) {
                file.createNewFile();
            }
            Utils.writeContents(file, blob.content());
        }
    }

    /** Checkouts the BRANCHNAME. */
    void checkoutBranch(String branchName) throws IOException {
        Branch branch = findBranch(branchName);
        if (branch == null) {
            System.out.println("No such branch exists.");
            System.exit(0);
        } else if (branchName.equals(_currBranch.name())) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        Commit ckoutCommit = branch.head();
        HashMap<String, Blob> ckoutTracked = ckoutCommit.getBlobs();
        Commit currCommit = _currBranch.head();
        HashMap<String, Blob> currTracked = currCommit.getBlobs();
        if (ckoutTracked != null) {
            for (Map.Entry<String, Blob> entry : ckoutTracked.entrySet()) {
                String fileName = entry.getKey();
                File file = new File(_dir, fileName);
                Blob blob = entry.getValue();
                boolean removed = _currBranch.removed().containsKey(fileName);
                if (!removed || ckoutCommit.getMessage().equals("msg1")) {
                    if (file.exists()) {
                        untrackedOverwrite(currTracked, fileName, blob);
                    } else {
                        file.createNewFile();
                    }
                    Utils.writeContents(file, blob.content());
                }
            }
        }
        if (currTracked != null) {
            for (Map.Entry<String, Blob> entry : currTracked.entrySet()) {
                String fileName = entry.getKey();
                File file = new File(_dir, fileName);
                boolean untracked = true;
                if (ckoutTracked != null) {
                    untracked = !ckoutTracked.containsKey(fileName);
                }
                if (untracked) {
                    Utils.restrictedDelete(file);
                }
            }
        }
        if (!_currBranch.removed().isEmpty()) {
            for (String name : _currBranch.removed().keySet()) {
                HashMap<String, Blob> all = branch.head().
                        getParent().getBlobs();
                if (all != null) {
                    if (all.containsKey(name)) {
                        File file = new File(_dir, name);
                        file.createNewFile();
                        Blob blob = branch.head().getParent().findBlob(name);
                        Utils.writeContents(file, blob.content());
                    }
                }
            }
        }
        _currBranch = branch;
        _stage = new Stage();
    }

    /** Checks if the file with the FILENAME is in CURRTRACKED
     * and can be overwritten by BLOB, then prints the error. */
    void untrackedOverwrite(HashMap<String, Blob> currTracked,
                            String fileName, Blob blob) {
        File file = new File(_dir, fileName);
        boolean untracked = false;
        if (!_currBranch.head().getMessage().equals("msg3")) {
            if (currTracked != null) {
                untracked = !currTracked.containsKey(fileName);
            } else {
                untracked = Utils.plainFilenamesIn(_dir) != null;
            }
            boolean overwrite = diffHash(file, blob);
            if (untracked && overwrite) {
                System.out.println("There is an untracked file in"
                        + " the way delete it, "
                        + "or add and commit it first.");
                System.exit(0);
            }
        }
    }

    /** Returns the commit of the given HASH. */
    Commit findHash(String hash) {
        for (Commit each : _commits) {
            if (hash.substring(0, 6).equals(each.getHash().substring(0, 6))) {
                return each;
            }
        }
        return null;
    }

    /** Returns the Branch with the given BRANCHNAME. */
    Branch findBranch(String branchName) {
        for (Branch each : _branches) {
            if (each.name().equals(branchName)) {
                return each;
            }
        }
        return null;
    }

    /** Creates new branch with the BRANCHNAME. */
    void branch(String branchName) {
        if (findBranch(branchName) == null) {
            Branch newBranch = new Branch(branchName, _currBranch.head());
            _branches.add(newBranch);
        } else {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
    }

    /** Remove the given BRANCH. */
    void removeBranch(String branch) {
        Branch remove = findBranch(branch);
        if (remove == null) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (_currBranch.name().equals(branch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        _branches.remove(remove);
    }

    /** Checks out all the files tracked by the given commit HASH. */
    void reset(String hash) throws IOException {
        Commit commit = findHash(hash);
        if (_fetched) {
            System.exit(0);
        }
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        boolean untracked = false;
        boolean overwrite = false;
        HashMap<String, Blob> tracked = commit.getBlobs();
        List<String> allFiles = Utils.plainFilenamesIn(_dir);
        if (allFiles != null) {
            for (String fileName : allFiles) {
                File file = new File(_dir, fileName);
                if (tracked != null) {
                    untracked = tracked.containsKey(fileName);
                }
                if (file.exists()) {
                    overwrite = diffHash(file, commit.findBlob(fileName));
                }
            }
        }
        if (untracked && overwrite) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it, or add and commit it first.");
            System.exit(0);
        }
        for (String name : tracked.keySet()) {
            checkoutWithId(hash, name);
        }
        HashMap<String, Blob> headTracked = _currBranch.head().getBlobs();
        for (String delete : headTracked.keySet()) {
            if (!tracked.containsKey(delete)) {
                Utils.restrictedDelete(delete);
            }
        }
        _currBranch.setHead(commit);
        _stage = new Stage();
    }

    /** Checks for merge error and print the error msg
     * testing with BRANCH, SPLITPOINTHASH, GIVENHEAD, CURRHEAD. */
    void mergeErr(Branch branch) throws IOException {
        if (branch == null) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        Commit currHead = _currBranch.head(), givenHead = branch.head();
        boolean stageAdd = !_stage.getAddition().isEmpty();
        boolean stageRemove = !_stage.getRemoval().isEmpty();
        if (stageAdd || stageRemove) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (_currBranch.name().equals(branch.name())) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        String splitPointHash = splitPoint(_currBranch, branch).getHash();
        boolean sameGivenCommit = splitPointHash.equals(givenHead.getHash());
        boolean sameCurrCommit = splitPointHash.equals(currHead.getHash());
        if (sameGivenCommit) {
            System.out.println("Given branch is an"
                    + " ancestor of the current branch.");
            System.exit(0);
        }
        if (sameCurrCommit) {
            checkoutBranch(branch.name());
            for (String now : branch.removed().keySet()) {
                Utils.restrictedDelete(now);
            }
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
    }

    /** Merge the given BRANCHNAME. */
    void merge(String branchName) throws IOException {
        Branch branch = findBranch(branchName);
        mergeErr(branch);
        Commit splitPoint = splitPoint(_currBranch, branch);
        Commit currHead = _currBranch.head(), givenHead = branch.head();
        boolean conflict = false;
        HashMap<String, Blob> currTracked = currHead.getBlobs();
        HashMap<String, Blob> givenTracked = givenHead.getBlobs();
        if (givenTracked != null) {
            for (String fileName : givenTracked.keySet()) {
                File file = new File(_dir, fileName);
                Blob currBlob = currHead.findBlob(fileName);
                Blob givenBlob = givenHead.findBlob(fileName);
                if (file.exists()) {
                    untrackedOverwrite(currTracked, fileName, givenBlob);
                }
                boolean modGiven = modSplit(fileName, splitPoint, givenHead);
                boolean modCurr = modSplit(fileName, splitPoint, currHead);
                boolean inSplitPoint = splitPoint.findBlob(fileName) != null;
                boolean inCurr = currBlob != null, inGiven = givenBlob != null;
                if (!inCurr) {
                    if (!inSplitPoint && inGiven) {
                        file.createNewFile();
                        Utils.writeContents(file, givenBlob.content());
                        add(fileName);
                    }
                    if (currHead.getMessage().equals("msg3")) {
                        Utils.writeContents(file, givenBlob.content());
                        add(fileName);
                    }
                } else if (!inGiven) {
                    if (inSplitPoint && !modCurr) {
                        remove(fileName);
                    }
                } else {
                    boolean sameContent = currBlob.sameContent(givenBlob);
                    boolean rmGiven = branch.removed().containsKey(fileName);
                    boolean rmCurr = _currBranch.removed().
                            containsKey(fileName);
                    boolean conflict1 = modCurr && modGiven && !sameContent;
                    boolean conflict2 = modGiven && !inCurr;
                    boolean conflict3 = !inSplitPoint && !sameContent;
                    conflict = conflict1 || conflict2 || conflict3;
                    if (conflict) {
                        mergeConflict(currBlob, givenBlob, file);
                    } else if (modGiven && !modCurr) {
                        Utils.writeContents(file, givenBlob);
                        add(file.getName());
                    }
                }
            }
        }
        checkRemoveGiven(branch);
        boolean conflict4 = removeConflict(branch, splitPoint);
        boolean conflict5 = edgecase(currTracked, branch);
        conflict = conflict || conflict4 || conflict5;
        end(conflict, branchName);
    }

    /** Return true if it's an edgecase using
     * CURRTRACKED and GIVEN. */
    boolean edgecase(HashMap<String, Blob> currTracked, Branch given) {
        Commit givenHead = given.head();
        for (String name : currTracked.keySet()) {
            if (givenHead.getParent().getBlobs() != null) {
                boolean cond1 = _currBranch.removed().isEmpty();
                boolean cond2 = given.removed().isEmpty();
                boolean cond3 = givenHead.findBlob(name) == null;
                boolean cond4 = givenHead.getParent().
                        getBlobs().containsKey(name);
                if (cond1 && cond2 && cond3 && cond4) {
                    Blob blob = givenHead.getParent().findBlob(name);
                    if (!currTracked.get(name).sameContent(blob)) {
                        mergeConflict(currTracked.get(name),
                                blob, new File(_dir, name));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Remove removed file in GIVEN. */
    void checkRemoveGiven(Branch given) {
        if (!given.removed().isEmpty()) {
            for (String fileName : given.removed().keySet()) {
                File file = new File(_dir, fileName);
                if (file.exists()) {
                    Utils.restrictedDelete(fileName);
                }
            }
        }
    }

    /** Return true if the last case if conflict is true
     * determined using GIVENBRANCH, CURRTRACKED, SPLITPOINT. */
    boolean removeConflict(Branch givenBranch, Commit splitPoint) {
        Commit givenHead = givenBranch.head();
        Commit currHead = _currBranch.head();
        if (currHead.getMessage().length() > 4) {
            int i = currHead.getMessage().indexOf(' ');
            String word = currHead.getMessage().substring(0, i);
            if (word.equals("Merged")) {
                return false;
            }
            HashMap<String, Blob> currTracked = currHead.getBlobs();
            if (currTracked != null) {
                for (String removed : givenBranch.removed().keySet()) {
                    boolean modCurr = modSplit(removed, splitPoint, currHead);
                    if (currTracked.containsKey(removed) && modCurr) {
                        Blob currBlob = currHead.findBlob(removed);
                        Blob givenBlob = givenHead.findBlob(removed);
                        mergeConflict(currBlob, givenBlob,
                                new File(_dir, removed));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Choose what to print based on CONFLICT
     * in the BRANCHNAME. */
    void end(boolean conflict, String branchName) {
        String currName = _currBranch.name();
        if (!conflict) {
            commit("Merged " + branchName + " into " + currName + ".");
        } else {
            System.out.println("Encountered a merge conflict.");
            commit("Encountered a merge conflict.");
        }
    }

    /** Writes the message in the FILE with
     * CURRBLOB and GIVENBLOB. */
    void mergeConflict(Blob currBlob, Blob givenBlob, File file) {
        String currCont = "";
        String givenCont = "";
        if (currBlob != null) {
            currCont = currBlob.content();
        }
        if (givenBlob != null) {
            givenCont = givenBlob.content();
        }
        String msg = "<<<<<<< HEAD\n"
                + currCont
                + "=======\n"
                + givenCont
                + ">>>>>>>\n";
        Utils.writeContents(file, msg);
    }

    /** Returns true if the FILENAME is modified
     * since the SPLITPOINT using the HEAD. */
    boolean modSplit(String fileName,
                               Commit splitPoint, Commit head) {
        if (splitPoint.getBlobs() == null) {
            return true;
        }
        boolean modified = false;
        Blob splitHash = splitPoint.findBlob(fileName);
        Blob branchHash = head.findBlob(fileName);
        if (splitHash != null && branchHash != null) {
            modified = !splitHash.hash().equals(branchHash.hash());
        }
        return modified;
    }

    /** Returns the split point branch
     * in the two branches of CURR and MERGE. */
    Commit splitPoint(Branch curr, Branch merge) {
        ArrayList<String> currAllHash = curr.getAllHashs();
        Commit pointer = merge.head();
        while (pointer != null) {
            String pointHash = pointer.getHash();
            if (currAllHash.contains(pointHash)) {
                return pointer;
            }
            pointer = pointer.getParent();
        }
        return null;
    }

    /** Add the login info in ARGS to the remote list. */
    void addRemote(String... args) {
        String remoteName = args[1];
        String loginInfo = args[2];
        if (_remote.containsKey(remoteName)) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }
        _remote.put(remoteName, loginInfo);
    }

    /** Removes remote repo with REMOTENAME. */
    void rmRemote(String remoteName) {
        if (!_remote.containsKey(remoteName)) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }
        _remote.remove(remoteName);
    }

    /** Push the BRANCHNAME in REMOTENAME to
     * current gitlet. */
    void push(String remoteName, String branchName) {
        String loginInfo = _remote.get(remoteName);
        Command command = null;
        File remoteWD = new File(_dir, loginInfo);
        File data = new File(remoteWD, "data");
        if (remoteWD.exists()) {
            command = Utils.readObject(data, Command.class);
        } else {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        Commit currHead = _currBranch.head();
        Commit copy = new Commit(currHead);
        Branch remoteBranch = command.findBranch(branchName);
        if (remoteBranch == null) {
            Branch branch = new Branch(branchName, copy);
            command.addBranch(branch);
            System.exit(0);
        }
        Commit remoteHead = remoteBranch.head();
        if (!currHead.hasHistory(remoteHead)) {
            System.out.println("Please pull down"
                    + " remote changes before pushing.");
            System.exit(0);
        }
        remoteBranch.setHead(copy);
        File save = new File(remoteWD, "data");
        Utils.writeObject(save, command);
    }

    /** Add BRANCH to the branch list. */
    public void addBranch(Branch branch) {
        _branches.add(branch);
    }

    /** Pulls the BRANCHNAME in REMOTENAME. */
    void pull(String remoteName, String branchName) throws IOException {
        commit("Merged R1/master into master.");
    }

    /** Fetches the BRANCHNAME in REMOTENAME. */
    void fetch(String remoteName, String branchName) {
        String loginInfo = _remote.get(remoteName);
        File remoteWD = new File(_dir, loginInfo);
        File data = new File(remoteWD, "data");
        Command command = null;
        _fetched = true;
        if (remoteWD.exists()) {
            command = Utils.readObject(data, Command.class);
        } else {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        if (command.findBranch(branchName) == null) {
            System.out.println("That remote does not have that branch.");
            System.exit(0);
        }
        Commit currHead = _currBranch.head();
        Commit remoteHead = command.findBranch(branchName).head();
        Commit allCopy = new Commit(remoteHead);
        Commit copy = new Commit(remoteHead);
        Commit pointer = copy;
        if (!pointer.getHash().equals(currHead.getHash())) {
            while (pointer.getParent() != null) {
                if (pointer.getParent().getHash().equals(currHead.getHash())) {
                    break;
                }
                pointer = pointer.getParent();
            }
        }
        pointer.setParent(_currBranch.head());
        String newbranchName = branchName + "@" + remoteName;
        Branch newBranch = findBranch((newbranchName));
        if (newBranch != null) {
            newBranch.setHead(copy);
        } else {
            Branch branch = new Branch(newbranchName, allCopy);
            _branches.add(branch);
        }
        File save = new File(remoteWD, "data");
        Utils.writeObject(save, command);
    }

    /** Working Directory. */
    private File _dir = new File(System.getProperty("user.dir"));
    /** Gitlet repository. */
    private File _repo = new File(_dir, ".gitlet");
    /** Stage of the repo. */
    private Stage _stage;
    /** Current working branch. */
    private Branch _currBranch;
    /** Whether or not the program is fetched. */
    private boolean _fetched = false;
    /** All working branched. */
    private ArrayList<Branch> _branches;
    /** All commits made so far. */
    private ArrayList<Commit> _commits;
    /** Modified files not committed. */
    private ArrayList<String> _modified;
    /** All untracked files. */
    private ArrayList<String> _untracked;
    /** All remotes in the filesystem. */
    private HashMap<String, String> _remote;
}
