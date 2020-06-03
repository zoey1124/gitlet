package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/** @author Mengzhu*/
public class Remote implements Serializable {
    /** w. */
    private File wkdir = new File(System.getProperty("user.dir"));
    /** g. */
    private File gitlet = new File(wkdir, ".gitlet");

    /** A hashMap from remote name to remote path to the
     * directory. */
    private HashMap<String, String> remoteMap;

    /** @return hashmap */
    HashMap<String, String> getMap() {
        return remoteMap;
    }
    /** @param m m */
    Remote(HashMap<String, String> m) {
        remoteMap = m;
    }
    /** d. */
    public void serializeRemote() {
        File r = new File(gitlet, "remote");
        Utils.writeObject(r, this);
    }
}
