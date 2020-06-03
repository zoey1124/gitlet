package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Represent a commit object. A commit is a snapshot of the
 * whole project in repository.
 * A commit is a combination of log messages and metadata,
 * a reference to a tree, and reference to parent commits.
 *  @author Mengzhu Sun
 */
public class Commit implements Serializable {

    /** wkdir. */
    private File wkdir = new File(System.getProperty("user.dir"));
    /** gitlet. */
    private File gitlet = new File(wkdir, ".gitlet");
    /** commit. */
    private File commit = new File(gitlet, "commit");

    /** The log message. */
    private String _logMsg;

    /** The log time fot this commit. */
    private String _timeStamp;

    /** A mapping of files names (key) to blob reference ID (values). */
    private HashMap<String, Blob> _map;

    /** A reference to parent commit. The default parent. */
    private String _parent1;

    /** Parent2. */
    private String _parent2;

    /** The commit ID of this commit. */
    private String _commitID;

    /** The branch name if this commit. */
    private String _branch;

    /** The default constructor when we first init the repository. */
    Commit() {
        _logMsg = "initial commit";
        _timeStamp = "Thu Jan 1 00:00:00 1970 -0800";
        _parent1 = null;
        _parent2 = null;
        _map = new HashMap<>();
        _branch = "master";
        _commitID = setCommitID();
        serializeCommit();
    }

    /** @param msg msg
     * @param parent1 p2
     * @param parent2 p2
     * @param branch b
     * @param blobs b */
    Commit(String msg, String parent1,
           String parent2, String branch,
           ArrayList<Blob> blobs) {
        _logMsg = msg;
        _parent1 = parent1;
        _parent2 = parent2;
        _timeStamp = setTimeStamp();
        _map = setMap(blobs);
        _branch = branch;
        _commitID = setCommitID();
        serializeCommit();
    }

    /** Set (generate) the commit ID using the SHA-1 hashing.
     * @return commitID **/
    String setCommitID() {
        String allParts = "";
        allParts += getLogMsg();
        allParts += getTimeStamp();
        if (getBranch() == null) {
            allParts += "";
        } else {
            allParts += getBranch();
        }
        if (getParent1ID() == null) {
            allParts += "";
        } else {
            allParts += getParent1ID();
        }
        return Utils.sha1(allParts);
    }

    /** @return current time string. */
    String setTimeStamp() {
        ZonedDateTime curr = ZonedDateTime.now();
        DateTimeFormatter pattern =
                DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss YYYY Z");

        return curr.format(pattern);
    }

    /** This method should take care of the addition stage
     * and the removal stage. First inherit the HashMap from the parent commit,
     * compare the hashMap with the stage blobs.
     * After adding and removing proper blobs, generate a new HashMap.
     * @return hashMap
     * @param blobs b */
    HashMap<String, Blob> setMap(ArrayList<Blob> blobs) {
        _map = new HashMap<>();
        if (blobs.size() != 0) {
            for (Blob b : blobs) {
                _map.put(b.getName(), b);
            }
        }
        return _map;
    }

    /** Set the branch.
     * @param branch b */
    void setBranch(String branch) {
        _branch = branch;
    }

    /** @return log message. */
    String getLogMsg() {
        return _logMsg;
    }

    /** @return the time stamp for the commit. */
    String getTimeStamp() {
        return _timeStamp;
    }

    /** @return the hash map of this commit.  */
    HashMap<String, Blob> getMap() {
        return _map;
    }

    /** @return parent 1 commit ID. */
    String getParent1ID() {
        return _parent1;
    }

    /** @return parent 2 ID. */
    String getParent2ID() {
        return _parent2;
    }

    /** @return commit ID. */
    String getCommitID() {
        return _commitID;
    }

    /** @return branch name. */
    String getBranch() {
        return _branch;
    }

    /** Serialize this commit for the history record. */
    void serializeCommit() {
        File firstCmt = new File(commit, getCommitID());
        Utils.writeContents(firstCmt, Utils.serialize(this));
    }
}
