package GroupDataBase.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroupRequest implements Serializable {
    private final String groupReqID;
    private int amount;
    private String groupID;
    private List<String> reqIDs;

    public GroupRequest(String groupReqID,Group group, int amount){
        this.groupReqID = groupReqID;
        this.amount = amount;
        this.groupID = group.getGroupID();
        this.reqIDs = new ArrayList<>();
    }

    public String getGroupReqID() {
        return groupReqID;
    }

    public String getGroupID() {
        return groupID;
    }

    public List<String> getRequests() {
        return reqIDs;
    }

    public Boolean isDone(HashMap<String, Request> requestHashMap) {
        for (String reqID: reqIDs) {
            if(!(requestHashMap.get(reqID).isDone())){
                return false;
            }
        }
        return true;
    }

    public void addReqID(String reqID){
        if(!reqIDs.contains(reqID)){
            reqIDs.add(reqID);
        }
    }

    public String toString(){
        return "Group Request ID: " + groupReqID + " | Amount: " +amount+ " | Group ID: "+groupID; // TODO add previous stuff reqID:amount:senderID
    }
}
