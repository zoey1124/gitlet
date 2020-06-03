package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import static gitlet.Utils.*;


/** Represent a Gitlet. After implementing git init in a dictionary,
 * we form a gitlet of that dictionary.
 * The main class should call gitlet class.
 * @author Mengzhu Sun, cooperate with Jiaxin, Zihan, Duowei, Paul */
public class Gitlet implements Serializable {

    /** Alist of all branch names. */
    private ArrayList<String> _allBranches;

    /** A pointer to the current branch. */
    private String _currBranch;

    /** Point to the current commit we are on. */
    private Commit _currCommit;

    /** w. */
    private File wkdir = new File(System.getProperty("user.dir"));
    /** g. */
    private File gitlet = new File(wkdir, ".gitlet");
    /** c. */
    private File commit = new File(gitlet, "commit");
    /** b. */
    private File branch = new File(gitlet, "branch");
    /** a. */
    private File addStaging = new File(gitlet, "addStaging");
    /** r. */
    private File removeStaging = new File(gitlet, "removeStaging");


    /** @return current commit. */
    Commit getCurrCommit() {
        return _currCommit;
    }

    /** Do add-remote.
     * @param name n
     * @param path p*/
    public void addRemote(String name, String path) {
        Remote r = deserializeRemote();
        HashMap<String, String> remoteM = r.getMap();
        if (remoteM.containsKey(name)) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }
        remoteM.put(name, path);
        Remote re = new Remote(remoteM);
        re.serializeRemote();
    }
    /** @return remote */
    Remote deserializeRemote() {
        File r = new File(gitlet, "remote");
        return readObject(r, Remote.class);
    }
    /** Do rm remote.
     * @param name n */
    public void rmRemote(String name) {
        Remote r = deserializeRemote();
        HashMap<String, String> rM = r.getMap();
        if (!rM.containsKey(name)) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }
        rM.remove(name);
        Remote re = new Remote(rM);
        re.serializeRemote();
    }
    /** Do push.
     * @param name n
     * @param branchName b*/
    public void push(String name, String branchName) {
        Remote r = deserializeRemote();
        HashMap<String, String> rM = r.getMap();
        if (!rM.containsKey(name)) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        String path = rM.get(name);
        File b = new File(path + "/branch/" + branchName);
        Commit rbh = readObject(b, Commit.class);
        Commit lbh = deserializeBranch(branchName);
        String remoteID = rbh.getCommitID();
        String localID = lbh.getCommitID();
        List<String> allC = plainFilenamesIn(commit);
        if (!allC.contains(remoteID)) {
            System.out.println("Please pull down"
                    + " remote changes before pushing.");
            System.exit(0);
        }

        writeObject(new File(path, "HEAD"), serialize(lbh));

    }
    /** Do fetch.
     * @param name n
     * @param branchName b */
    public void fetch(String name, String branchName) {

    }
    /** Do pull.
     * @param name n
     * @param branchName b*/
    public void pull(String name, String branchName) {

    }

    /** Do init. */
    public void init() {
        if (gitlet.exists()) {
            System.out.println("Gitlet version-control system"
                    + " already exists in the current directory.");
            System.exit(0);
        } else {
            gitlet.mkdir();
            commit.mkdir();
            branch.mkdir();
            addStaging.mkdir();
            removeStaging.mkdir();

            _currCommit = new Commit();
            _currBranch = "master";
            _allBranches = new ArrayList<>();
            _allBranches.add("master");

            updateHead();
            serializeBranch(_currBranch, getCurrCommit());
            Remote r = new Remote(new HashMap<>());
            r.serializeRemote();
        }
    }

    /** Do git add.
     * @param f f */
    public void add(String f) {
        File x = new File(f);
        if (!x.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        _currCommit = deserializeHead();

        File checkRemove = new File(removeStaging, f);
        if (checkRemove.exists()) {
            checkRemove.delete();
            System.exit(0);
        }

        HashMap<String, Blob> m = _currCommit.getMap();

        for (String name : m.keySet()) {
            if (name.equals(f)
                    && readContentsAsString(x).equals(m.get(f).getContent())) {
                File checkAdd = new File(addStaging, f);
                if (checkAdd.exists()) {
                    checkAdd.delete();
                }
                System.exit(0);
            }
        }

        Blob blob = new Blob(f);
        blobToAddingStage(blob);
    }
    /** @param blob */
    void blobToAddingStage(Blob blob) {
        File f = new File(addStaging, blob.getName());
        writeContents(f, serialize(blob));
    }

    /** Do git commit.
     * @param message m */
    public void commit(String message) {
        if (message.length() == 0 || message.equals(" ")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        if (addStaging.list().length == 0
            && removeStaging.list().length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        _currCommit = deserializeHead();
        _currBranch = getCurrCommit().getBranch();
        ArrayList<Blob> blobList = new ArrayList<>();
        HashMap<String, Blob> map = _currCommit.getMap();

        List<String> checkRemove = plainFilenamesIn(removeStaging);
        for (String name : checkRemove) {
            map.remove(name);
        }

        for (Blob b : map.values()) {
            blobList.add(b);
        }

        for (File file : addStaging.listFiles()) {
            Blob b = deserializeBlob(file);
            blobList.add(b);
        }
        String[] checkMerge = message.split("\\s+");
        if (checkMerge[0].equals("Merged")) {
            String branchName = checkMerge[1];
            Commit b = deserializeBranch(branchName);
            _currCommit = new Commit(message,
                    _currCommit.getCommitID(), b.getCommitID(),
                    _currCommit.getBranch(), blobList);
        } else {
            _currCommit = new Commit(message, _currCommit.getCommitID(), null,
                    _currCommit.getBranch(), blobList);
        }
        _currCommit.serializeCommit();

        updateHead();
        serializeBranch(_currBranch, getCurrCommit());

        clearDir(addStaging);
        clearDir(removeStaging);

    }

    /** @param branchName bn
     * @return commit */
    Commit deserializeBranch(String branchName) {
        File f = new File(branch, branchName);
        return readObject(f, Commit.class);
    }

    /** Do git rm.
     * @param filename f */
    public void rm(String filename) {
        _currCommit = deserializeHead();

        File file = new File(filename);
        if (!file.exists()
                && !_currCommit.getMap().keySet().contains(filename)) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        List<String> addFiles = plainFilenamesIn(addStaging);
        if (!addFiles.contains(filename)
                && !_currCommit.getMap().keySet().contains(filename)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        if (addFiles.contains(filename)) {
            File checkAdd = new File(addStaging, filename);
            checkAdd.delete();
            System.exit(0);
        }

        if (_currCommit.getMap() != null) {
            Blob b = _currCommit.getMap().get(filename);
            blobToRemoveStage(b);
        }

        restrictedDelete(new File(wkdir, filename));
    }

    /** @param blob b */
    void blobToRemoveStage(Blob blob) {
        File f = new File(removeStaging, blob.getName());
        writeContents(f, serialize(blob));
    }

    /** Do git log. */
    public void log() {
        _currCommit = deserializeHead();
        String commitID = getCurrCommit().getCommitID();
        while (commitID != null) {
            logHelper(commitID);
            Commit currCommit = deserializeCommit(commitID);
            commitID = currCommit.getParent1ID();
        }
    }

    /** Display the information of one commit.
     * @param commitID c */
    public void logHelper(String commitID) {
        Commit currCommit = deserializeCommit(commitID);
        System.out.println("==="
                + "\n" + "commit " + currCommit.getCommitID()
                + "\n" + "Date: " + currCommit.getTimeStamp()
                + "\n" + currCommit.getLogMsg()
                + "\n");
    }

    /** Display the information of one commit.
     * @param c c */
    public void logHelper(Commit c) {
        System.out.println("==="
                + "\n" + "commit " + c.getCommitID()
                + "\n" + "Date: " + c.getTimeStamp()
                + "\n" + c.getLogMsg()
                + "\n");
    }

    /** Do global log. */
    public void globalLog() {
        for (File f : commit.listFiles()) {
            if (!f.isDirectory()) {
                Commit cmt = deserializeCommit(f);
                logHelper(cmt);
            }
        }
    }

    /** Do git find.
     * @param commitMessage c */
    public void find(String commitMessage) {
        boolean exists = false;
        for (File f : commit.listFiles()) {
            Commit c = deserializeCommit(f);
            if (c.getLogMsg().equals(commitMessage)) {
                System.out.println(c.getCommitID());
                exists = true;
            }
        }
        if (!exists) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }

    }

    /** Do git status. */
    public void status() {
        _currCommit = deserializeHead();
        System.out.println("=== Branches ===");
        _currBranch = _currCommit.getBranch();
        for (String filename : plainFilenamesIn(branch)) {
            if (filename.equals(_currBranch)) {
                System.out.println("*" + filename);
            } else {
                System.out.println(filename);
            }
        }
        System.out.println(" ");

        System.out.println("=== Staged Files ===");
        List<String> addFiles = plainFilenamesIn(addStaging);
        for (String name : addFiles) {
            System.out.println(name);
        }
        System.out.println(" ");

        System.out.println("=== Removed Files ===");
        List<String> removeFiles = plainFilenamesIn(removeStaging);
        for (String name : removeFiles) {
            System.out.println(name);
        }
        System.out.println(" ");

        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String s : modify(_currCommit)) {
            System.out.println(s);
        }
        System.out.println(del(_currCommit));
        System.out.println("=== Untracked Files ===");
        for (String name : getUntrackFiles(_currCommit)) {
            System.out.println(name);
        }
        System.out.println(" ");
    }

    /** @param c c
     * @return str */
    private String del(Commit c) {
        String o = "";
        List<String> local = plainFilenamesIn(wkdir);
        List<String> rev = plainFilenamesIn(removeStaging);
        HashMap<String, Blob> map = c.getMap();
        for (String s : map.keySet()) {
            if (!local.contains(s) && !rev.contains(s)) {
                o += s + " (deleted)";
            }
        }
        return o;
    }

    /** @param currCommit c
     * @return list */
    private ArrayList<String> modify(Commit currCommit) {
        ArrayList<String> m = new ArrayList<>();
        HashMap<String, Blob> map = currCommit.getMap();
        for (File f : wkdir.listFiles()) {
            if (!f.isDirectory()
                    && !f.getName().equals(".DS_Store")
                    && !f.getName().equals("Makefile")
                    && !f.getName().equals("proj3.iml")
                    && !f.getName().equals(".gitignore")
                    && !f.getName().equals("gitlet-design.txt")) {
                if (map.containsKey(f.getName())) {
                    String c1 = map.get(f.getName()).getContent();
                    if (!c1.equals(readContentsAsString(f))) {
                        m.add(f.getName() + " (modified)");
                    }
                }
            }
        }
        return m;
    }


    /** @return untracked file name list.
     * @param c c */
    public ArrayList<String> getUntrackFiles(Commit c) {
        ArrayList<String> untrack = new ArrayList<>();
        for (File f : wkdir.listFiles()) {
            if (!f.isDirectory()
                && !f.getName().equals(".DS_Store")
                && !f.getName().equals("Makefile")
                && !f.getName().equals("proj3.iml")
                && !f.getName().equals(".gitignore")
                && !f.getName().equals("gitlet-design.txt")) {
                String name = f.getName();
                File t = new File(addStaging, name);
                if (!c.getMap().keySet().contains(name) && !t.exists()) {
                    untrack.add(name);
                }
            }
        }
        return untrack;
    }

    /** Do git checkout with the input of just file name.
     * Take the version of the file as it exists in the head commit.
     * @param fileName f */
    public void checkoutFileName(String fileName) {
        _currCommit = deserializeHead();
        if (!getCurrCommit().getMap().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File f = new File(fileName);
        String contents = getCurrCommit().getMap().get(fileName).getContent();
        writeContents(f, contents);
    }

    /**
     * @param commitID c
     * @param fileName f */
    public void checkoutID(String commitID, String fileName) {
        File givenCommit = new File(commit, commitID);
        if (!givenCommit.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit c = deserializeCommit(givenCommit);
        HashMap<String, Blob> givenMap = c.getMap();
        if (!givenMap.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        File f = new File(fileName);
        String contents = givenMap.get(fileName).getContent();
        writeContents(f, contents);
    }

    /** @param commitID c
     * @param filename f
     * Take care of cases that the commit ID is the short version. */
    public void checkoutShortID(String commitID, String filename) {
        List<String> allCommits = plainFilenamesIn(commit);
        for (String id : allCommits) {
            if (id.contains(commitID)) {
                checkoutID(id, filename);
                System.exit(0);
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
    }

    /** Takes all files in the commit at the head of the given
     * branch, and put them in the working directory, overwriting the
     * versions of the files that are already there if they exists.
     * Change the head to the current branch head.
     * The Staging area are cleared.
     * @param branchName b */
    public void checkoutBranch(String branchName) {
        File f = new File(branch, branchName);
        if (!f.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        _currCommit = deserializeHead();
        if (_currCommit.getBranch().equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        ArrayList<String> untrack = getUntrackFiles(_currCommit);
        if (untrack.size() != 0) {
            System.out.println("There is an untracked "
                    + "file in the way; delete it or add it first.");
            System.exit(0);
        }
        Commit branchHead = readObject(f, Commit.class);
        HashMap<String, Blob> map = _currCommit.getMap();
        List<String> local = plainFilenamesIn(wkdir);
        for (String filename : map.keySet()) {
            if (local.contains(filename)) {
                restrictedDelete(filename);
            }
        }

        HashMap<String, Blob> branchMap = branchHead.getMap();
        for (String filename : branchMap.keySet()) {
            File file = new File(filename);
            String contents = branchMap.get(filename).getContent();
            writeContents(file, contents);
        }

        writeContents(new File(".gitlet/HEAD"),
                Utils.serialize(branchHead));

        clearDir(addStaging);
        clearDir(removeStaging);
    }

    /** Create a new branch with the given name,
     *  and point it at the current node.
     * @param branchName b */
    public void branch(String branchName) {
        File newBranch = new File(branch, branchName);
        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        _currCommit = deserializeHead();
        _currCommit.setBranch(branchName);
        writeContents(newBranch, serialize(_currCommit));
    }

    /** Deletes the branch with the given name.
     * @param branchName b */
    public void rmBranch(String branchName) {
        File removeB = new File(branch, branchName);
        if (!removeB.exists()) {
            System.out.println("A branch with that name does not exist.");
        }
        _currCommit = deserializeHead();
        if (_currCommit.getBranch().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        removeB.delete();
    }

    /** Checks out all the files tracked by the given commit.
     * @param commitID c */
    public void reset(String commitID) {
        File file = new File(commit, commitID);
        if (!file.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        ArrayList<String> untrack = getUntrackFiles(deserializeHead());
        if (untrack.size() != 0) {
            System.out.println("There is an untracked file in "
                    + "the way; delete it or add it first.");
            System.exit(0);
        }
        Commit givenCommit = deserializeCommit(commitID);

        _currCommit = deserializeHead();
        HashMap<String, Blob> map = _currCommit.getMap();
        List<String> local = plainFilenamesIn(wkdir);
        for (String filename : map.keySet()) {
            if (local.contains(filename)) {
                restrictedDelete(filename);
            }
        }

        List<String> addNames = plainFilenamesIn(addStaging);
        for (String filename : addNames) {
            if (local.contains(filename)) {
                restrictedDelete(filename);
            }
        }
        HashMap<String, Blob> givenMap = givenCommit.getMap();
        for (String filename : givenMap.keySet()) {
            File f = new File(wkdir, filename);
            writeContents(f, givenMap.get(filename).getContent());
        }
        writeContents(new File(".gitlet/HEAD"),
                serialize(givenCommit));
        String givenBranch = givenCommit.getBranch();
        writeContents(new File(branch, givenBranch),
                serialize(givenCommit));
        clearDir(addStaging);
        clearDir(removeStaging);
    }

    /** Take care if the reset ID is the short version.
     * @param commitID c */
    public void resetShortID(String commitID) {
        List<String> allCommits = plainFilenamesIn(commit);
        for (String id : allCommits) {
            if (id.contains(commitID)) {
                reset(id);
                System.exit(0);
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
    }

    /** mfc.
     * @param branchName b */
    private void failMerge(String branchName) {
        if (addStaging.list().length != 0 || removeStaging.list().length != 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        List<String> nameList = plainFilenamesIn(branch);
        if (!nameList.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
    }

    /** @param branchName b */
    private void failMerge2(String branchName) {
        _currCommit = deserializeHead();
        _currBranch  = _currCommit.getBranch();
        if (branchName.equals(_currBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        ArrayList<String> untrack = getUntrackFiles(_currCommit);
        if (untrack.size() != 0) {
            System.out.println("There is an untracked "
                    + "file in the way; delete it or add it first.");
            System.exit(0);
        }
    }

    /** Merge files from the given branch into the current branch.
     * @param branchName b */
    public void merge(String branchName) {
        failMerge(branchName);
        failMerge2(branchName);
        _currCommit = deserializeHead();
        _currBranch  = _currCommit.getBranch();
        Commit givenBranch =
                readObject(new File(branch, branchName), Commit.class);
        Commit splitPoint = splitPoint(givenBranch, _currCommit);
        if (splitPoint.getCommitID().equals(givenBranch.getCommitID())) {
            System.out.println(" Given branch is an"
                    + " ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPoint.getCommitID().equals(_currCommit.getCommitID())) {
            checkoutBranch(givenBranch.getBranch());
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        boolean hasConflict = false;
        HashMap<String, Blob> split = splitPoint.getMap();
        HashMap<String, Blob> given = givenBranch.getMap();
        HashMap<String, Blob> current = _currCommit.getMap();
        HashSet<String> allFileNames = allIn(split, current, given);
        for (String name : allFileNames) {
            if (h1(split, given, current, name)) {
                rm(name);
            } else if (h2(split, given, current, name)) {
                writeContents(new File(name), given.get(name).getContent());
                add(name);
            } else if (h3(split, given, current, name)) {
                writeContents(new File(name), given.get(name).getContent());
                add(name);
            } else if (h4(split, given, current, name)) {
                c1(current, given, name);
                hasConflict = true;
            } else if (h5(split, given, current, name)) {
                c1(current, given, name);
                hasConflict = true;
            } else if (h6(split, given, current, name)) {
                c2(given, name);
                hasConflict = true;
            } else if (h7(split, given, current, name)) {
                String content = "<<<<<<< HEAD\n";
                content += current.get(name).getContent();
                content += "=======\n";
                content += ">>>>>>>\n";
                writeContents(new File(name), content);
                hasConflict = true;
                add(name);
            }
        }
        commit("Merged " + branchName
                + " into " + _currCommit.getBranch() + ".");
        if (hasConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** @return b
     * @param split s
     * @param g g
     * @param c c
     * @param n n */
    private boolean h7(HashMap<String, Blob> split, HashMap<String, Blob> g,
                       HashMap<String, Blob> c, String n) {
        return split.containsKey(n)
                && c.containsKey(n)
                && !g.containsKey(n)
                && !split.get(n).getContent().equals(c.get(n).getContent());
    }

    /** @return b
     * @param split s
     * @param g g
     * @param current c
     * @param n n */
    private boolean h6(HashMap<String, Blob> split, HashMap<String, Blob> g,
                       HashMap<String, Blob> current, String n) {
        return split.containsKey(n)
                && !current.containsKey(n)
                && g.containsKey(n)
                && !split.get(n).getContent().equals(g.get(n).getContent());
    }
    /** @return b
     * @param split s
     * @param g g
     * @param c c
     * @param name n */
    private boolean h5(HashMap<String, Blob> split, HashMap<String, Blob> g,
                       HashMap<String, Blob> c, String name) {
        return !split.containsKey(name) && c.containsKey(name)
                && g.containsKey(name)
                && !c.get(name).getContent().equals(g.get(name).getContent());
    }
    /** @return b
     * @param s s
     * @param given g
     * @param c c
     * @param name n */
    private boolean h1(HashMap<String, Blob> s, HashMap<String, Blob> given,
                       HashMap<String, Blob> c, String name) {
        return s.containsKey(name)
                && c.containsKey(name)
                && !given.containsKey(name)
                && s.get(name).getContent().equals(c.get(name).getContent());
    }
    /** @return b
     * @param split s
     * @param given g
     * @param current c
     * @param name n */
    private boolean h2(HashMap<String, Blob> split, HashMap<String, Blob> given,
                       HashMap<String, Blob> current, String name) {
        return !split.containsKey(name)
                && !current.containsKey(name)
                && given.containsKey(name);
    }

    /** @return b
     * @param split s
     * @param g g
     * @param c c
     * @param n n */
    private boolean h3(HashMap<String, Blob> split, HashMap<String, Blob> g,
                       HashMap<String, Blob> c, String n) {
        return split.containsKey(n)
                && c.containsKey(n)
                && g.containsKey(n)
                && split.get(n).getContent().equals(c.get(n).getContent())
                && !split.get(n).getContent().equals(g.get(n).getContent());
    }
    /** @return b
     * @param split s
     * @param g g
     * @param c c
     * @param n n */
    private boolean h4(HashMap<String, Blob> split, HashMap<String, Blob> g,
                       HashMap<String, Blob> c, String n) {
        return split.containsKey(n)
                && c.containsKey(n)
                && g.containsKey(n)
                && !c.get(n).getContent().equals(g.get(n).getContent())
                && !c.get(n).getContent().equals(split.get(n).getContent())
                && !g.get(n).getContent().equals(split.get(n).getContent());
    }
    /** @param s s
     * @param c c
     * @param g g
     * @return hashSet */
    private HashSet<String> allIn(HashMap<String, Blob> s,
                                  HashMap<String, Blob> c,
                                  HashMap<String, Blob> g) {
        HashSet<String> allFileNames = new HashSet<>();
        for (String filename : s.keySet()) {
            allFileNames.add(filename);
        }
        for (String filename : g.keySet()) {
            allFileNames.add(filename);
        }
        for (String filename : c.keySet()) {
            allFileNames.add(filename);
        }
        return allFileNames;
    }

    /** @param curr c
     * @param g g
     * @param name n */
    private void c1(HashMap<String, Blob> curr,
                    HashMap<String, Blob> g, String name) {
        String content = "<<<<<<< HEAD\n";
        content += curr.get(name).getContent();
        content += "=======\n";
        content += g.get(name).getContent();
        content += ">>>>>>>\n";

        writeContents(new File(name), content);
        add(name);
    }

    /** @param g g
     * @param name n*/
    private void c2(HashMap<String, Blob> g, String name) {
        String content = "<<<<<<< HEAD\n";
        content += "=======\n";
        content += g.get(name).getContent();
        content += ">>>>>>>\n";
        writeContents(new File(name), content);
        add(name);
    }

    /** @return the commit of the latest common ancestor of two given branches.
     * @param branch1 b1
     * @param branch2 b2 */
    Commit splitPoint(Commit branch1, Commit branch2) {
        HashSet<String> branch1AllCommits = new HashSet<>();
        String commitID1 = branch1.getCommitID();
        while (commitID1 != null) {
            branch1AllCommits.add(commitID1);
            Commit currCommit = deserializeCommit(commitID1);
            if (currCommit.getParent2ID() != null) {
                branch1AllCommits.add(currCommit.getParent2ID());
            }
            commitID1 = currCommit.getParent1ID();
        }
        String commitID2 = branch2.getCommitID();
        while (commitID2 != null) {
            if (branch1AllCommits.contains(commitID2)) {
                Commit c = deserializeCommit(commitID2);
                return c;
            }
            Commit currCommit = deserializeCommit(commitID2);
            if (currCommit.getParent2ID() != null
                && branch1AllCommits.contains(currCommit.getParent2ID())) {
                return deserializeCommit(currCommit.getParent2ID());
            }
            commitID2 = currCommit.getParent1ID();
        }
        return null;
    }

    /** Serialize the branch into branch directory.
     * @param branchName b
     * @param c c */
    void serializeBranch(String branchName, Commit c) {
        File f = new File(branch, branchName);
        writeContents(f, serialize(c));
    }

    /** Deserialize one commit.
     * @param commitFile c
     * @return commit */
    Commit deserializeCommit(File commitFile) {
        return readObject(commitFile, Commit.class);
    }

    /** Deserialize the blob.
     * @param blob b
     * @return blob */
    Blob deserializeBlob(File blob) {
        return readObject(blob, Blob.class);
    }

    /** Clear all the files in the given directory.
     * Used for commit clearing stages.
     * @param directory d */
    void clearDir(File directory) {
        for (File f : directory.listFiles()) {
            if (!f.isDirectory()) {
                f.delete();
            }
        }
    }

    /** Update HEAD file to be the current commit. */
    void updateHead() {
        File head = new File(gitlet, "HEAD");
        writeContents(head, serialize(getCurrCommit()));
    }

    /** Deserialize this commit.
     * @return head */
    Commit deserializeHead() {
        File commitFile = new File(".gitlet/HEAD");
        return readObject(commitFile, Commit.class);
    }

    /** Convert a commit to a commit object.
     * The commit object has to be in the commit directory.
     * @param commitID c
     * @return commit */
    public Commit deserializeCommit(String commitID) {
        File f = new File(commit, commitID);
        if (f.exists()) {
            return readObject(f, Commit.class);
        } else {
            message("No commit with that id exists.");
            throw new GitletException();
        }
    }

}


























