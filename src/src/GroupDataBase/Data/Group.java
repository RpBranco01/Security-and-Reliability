package GroupDataBase.Data;

import java.io.Serializable;
import java.util.*;

public class Group implements Serializable {
    private final String groupID;
    private String owner;
    private List<String> usersIDs;

    public Group(String id, String owner) {
        usersIDs = new ArrayList<>();
        this.groupID = id;
        this.owner = owner;
        usersIDs.add(owner);
    }

    public synchronized void appendUser(String user) {
        usersIDs.add(user);
    }

    public String getGroupID() {
        return groupID;
    }

    public String getOwner() {
        return owner;
    }

    public List<String> getUsersIDs() {
        return usersIDs;
    }

    public boolean hasUser(String userID) {
        return (usersIDs.contains(userID));
    }

    public boolean isOwner(User client) {
        return this.getOwner().equals(client.getUserID());
    }

    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(groupID).append(":");
        for (String userID: usersIDs) {
            stringBuilder.append(userID).append(",");
        }
        stringBuilder.deleteCharAt(stringBuilder.length()-1);
        return stringBuilder.toString();
    }
}
