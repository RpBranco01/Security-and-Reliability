package GroupDataBase;

import GroupDataBase.Data.*;
import ReadWrite.Message;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BaseDeDados implements Serializable {
    private static final String NOT_GROUP_OWNER = "NOT GROUP OWNER";
    private static final String INSUFFICIENT_BALANCE = "INSUFFICIENT BALANCE";
    private static final String GROUP_NOT_FOUND = "GROUP NOT FOUND";
    private static final String USER_NOT_FOUND = "USER NOT FOUND";
    private static final String REQ_NOT_FOUND = "REQUEST NOT FOUND";
    private static final String INCOMPATIBLE_USERID_REQ = "INCOMPATIBLE USERID REQUEST";
    private static final String INVALID_AMOUNT = "INVALID AMOUNT";

    private int reqCounter;
    private int groupReqCounter;
    private HashMap<String, User> userHashMap;
    private HashMap<String, Group> groupHashMap;
    private HashMap<String, Request> requestHashMap;
    private HashMap<String, GroupRequest> groupsRequestHashMap;
    private Message messageHandler;

    public BaseDeDados() {
        userHashMap = new HashMap<>();
        groupHashMap = new HashMap<>();
        requestHashMap = new HashMap<>();
        groupsRequestHashMap = new HashMap<>();
        messageHandler = new Message();
        reqCounter = 0;
        groupReqCounter = 0;
    }

    public Message getMessageHandler() {
        return this.messageHandler;
    }

    private synchronized void incReqCounter() {
        reqCounter++;
    }

    private synchronized void incGroupReqCounter() {
        groupReqCounter++;
    }

    public synchronized void addUserHashMap(User user) {
        if (!userHashMap.containsKey(user.getUserID())) {
            userHashMap.put(user.getUserID(), user);
        }
    }

    public User getUser(String id) {
        return userHashMap.get(id);
    }

    public synchronized void updateUserHashMap(User user) {
        userHashMap.put(user.getUserID(), user);
    }

    public synchronized String makePayment(User client, String userID, int amount) {
        if (hasUser(userID)) {
            String answer = client.takeAmount(amount);
            if (answer.equals(INSUFFICIENT_BALANCE)) {
                return answer;
            }
            User user = userHashMap.get(userID);
            user.putAmount(amount);
            updateUserHashMap(client);
            updateUserHashMap(user);
            return "PAYMENT DONE";
        }
        return USER_NOT_FOUND;
    }

    public boolean hasUser(String userID) {
        return userHashMap.containsKey(userID);
    }

    public synchronized String requestPayment(String clientID, String userID, int amount) {
        if (hasUser(userID) && !clientID.equals(userID)) {
            Request request = new Request(this.reqCounter, userID, clientID, amount, false);
            incReqCounter();
            User user = userHashMap.get(userID);
            user.addRequest(request);
            requestHashMap.put(String.valueOf(request.getReqID()), request);
            updateUserHashMap(user);
            return "requestPayment para " + userID + " feito com amount " + amount;
        }
        return USER_NOT_FOUND;
    }

    public String viewRequests(String user) {
        StringBuilder str = new StringBuilder();
        List<Request> requestList = userHashMap.get(user).getRequests();
        str.append("\n_____________________________________________________________________\n");
        for (Request request : requestList) {
            if ((!request.isDone())) {
                str.append(request).append(";\n_____________________________________________________________________\n");
            }
        }
        return str.toString();
    }

    public String getRequest(int reqID, String user){
        List<Request> requestList = userHashMap.get(user).getRequests();
        for (Request request : requestList) {
            if(request.getReqID() == reqID){
                return request.toString();
            }
        }
        return "NOT FOUND";
    }

    public String getQRCode(int reqID){
        for (Request request : requestHashMap.values()) {
            if(request.getReqID() == reqID){
                return request.toString();
            }
        }
        return "NOT FOUND";
    }


    public synchronized String payRequest(User client, String reqID) {
        if (requestHashMap.containsKey(reqID) && (!requestHashMap.get(reqID).isDone())) {
            Request req = requestHashMap.get(reqID);

            if (req.getReceiverID().equals(client.getUserID())) {
                int amount = req.getAmount();

                if (client.takeAmount(amount).equals(INSUFFICIENT_BALANCE)) {
                    return INSUFFICIENT_BALANCE;
                }

                User user = userHashMap.get(req.getSenderID());
                user.putAmount(amount);
                req.setIsDone();
                requestHashMap.put(String.valueOf(req.getReqID()), req);
                updateUserHashMap(client);
                updateUserHashMap(user);
                return "PAY REQUEST SUCCESSFUL";
            } else {
                return INCOMPATIBLE_USERID_REQ;
            }
        }
        return REQ_NOT_FOUND;
    }

    public synchronized String obtainQRCode(User client, int amount) {
        if (amount <= 0) {
            return INVALID_AMOUNT;
        }
        Request request = new Request(this.reqCounter, client.getUserID(), client.getUserID(), amount, true);
        incReqCounter();
        requestHashMap.put(String.valueOf(request.getReqID()), request);
        return request.getqRCode().getQRCodeFilePath() + ":" + String.valueOf(request.getReqID());
    }

    public synchronized String confirmQRCode(User client, String reqID) {
        if (requestHashMap.containsKey(reqID) && (!requestHashMap.get(reqID).isDone())) {
            Request req = requestHashMap.get(reqID);

            int amount = req.getAmount();

            if (client.takeAmount(amount).equals(INSUFFICIENT_BALANCE)) {
                req.setIsDone();
                requestHashMap.put(String.valueOf(req.getReqID()), req);
                return INSUFFICIENT_BALANCE;
            }
            User sender = userHashMap.get(req.getSenderID());
            sender.putAmount(amount);
            req.setIsDone();
            requestHashMap.put(String.valueOf(req.getReqID()), req);
            updateUserHashMap(client);
            updateUserHashMap(sender);
            return "PAY REQUEST SUCCESSFUL";
        }
        return REQ_NOT_FOUND;
    }

    public synchronized String newGroup(User client, String groupID) {
        if (!hasGroup(groupID)) {
            Group group = new Group(groupID, client.getUserID());
            groupHashMap.put(groupID, group);
            client.addGroup(group);
            updateUserHashMap(client);
            return "GROUP CREATED";
        }
        return "GROUP ALREADY EXISTS";
    }

    private boolean hasGroup(String groupID) {
        return groupHashMap.containsKey(groupID);
    }

    public synchronized String addU(User client, String userID, String groupID) {
        if (!(hasUser(userID))) {
            return USER_NOT_FOUND;
        }
        if (hasGroup(groupID)) {
            Group group = groupHashMap.get(groupID);
            if (group.getOwner().equals(client.getUserID())) {
                if (!group.hasUser(userID)) {
                    group.appendUser(userID);
                    User user = userHashMap.get(userID);
                    user.addGroup(group);
                    updateUserHashMap(user);
                    groupHashMap.put(group.getGroupID(), group);
                    return "USER ADDED TO GROUP";
                } else {
                    return "USER ALREADY IN GROUP";
                }
            } else {
                return NOT_GROUP_OWNER;
            }
        }
        return GROUP_NOT_FOUND;
    }

    public String groups(User client) {
        StringBuilder strOwner = new StringBuilder();
        StringBuilder strBelongs = new StringBuilder();
        boolean isOwner = false;
        boolean belongs = false;

        strOwner.append("OWNER:\n");
        strBelongs.append("BELONGS:\n");

        for (Group group : client.getBelongedGroups()) {
            if (group.isOwner(client)) {
                isOwner = true;
                strOwner.append(group).append("\n");
            } else {
                belongs = true;
                strBelongs.append(group).append("\n");
            }
        }
        if (!isOwner) {
            strOwner.append("Cliente: ").append(client.getUserID()).append(" não é Owner de nenhum grupo.\n");
        }
        if (!belongs) {
            strBelongs.append("Cliente: ").append(client.getUserID()).append(" não pretence a nenhum grupo\n");
        }

        return strOwner.append("\n").append(strBelongs).toString();
    }

    public synchronized String dividePayment(User client, String groupID, int amount) {
        if (hasGroup(groupID)) {
            Group group = groupHashMap.get(groupID);
            if (group.getOwner().equals(client.getUserID())) {
                int totalUsers = group.getUsersIDs().size();
                int dividedAmount = amount / totalUsers; // MUDAR PARA FLOAT
                GroupRequest groupRequest =
                        new GroupRequest(String.valueOf(this.groupReqCounter), groupHashMap.get(groupID), dividedAmount);
                incGroupReqCounter();
                groupsRequestHashMap.put(groupRequest.getGroupReqID(), groupRequest);
                for (String userID : group.getUsersIDs()) {
                    if (!userID.equals(group.getOwner())) {
                        Request request =
                                new Request(this.reqCounter, userID, client.getUserID(), dividedAmount, false);
                        incReqCounter();
                        User user = userHashMap.get(userID);

                        user.addRequest(request);
                        groupRequest.addReqID(String.valueOf(request.getReqID()));

                        requestHashMap.put(String.valueOf(request.getReqID()), request);
                        updateUserHashMap(user);
                    }
                }
                return "DIVIDE PAYMENT DONE";
            } else {
                return NOT_GROUP_OWNER;
            }
        }
        return GROUP_NOT_FOUND;
    }

    public String statusPayments(User client, String groupID) {
        StringBuilder strNotPayed = new StringBuilder();
        strNotPayed.append("NOT PAYED:\n");

        if (hasGroup(groupID)) {
            Group group = groupHashMap.get(groupID);
            if (group.getOwner().equals(client.getUserID())) {
                for (GroupRequest groupReq : groupsRequestHashMap.values()) {
                    if (groupReq.getGroupID().equals(groupID) && !(groupReq.isDone(requestHashMap))) {
                        strNotPayed.append("GROUP: ").append(groupReq).append("\n  Requests:\n");
                        for (String reqID : groupReq.getRequests()) {
                            Request request = requestHashMap.get(reqID);
                            if (!(request.isDone())) {
                                strNotPayed.append("   -> ").append(request.toString().split(";")[0]).append("\n");
                            }
                        }
                    }
                }
                return strNotPayed.toString();
            } else {
                return NOT_GROUP_OWNER;
            }
        }
        return GROUP_NOT_FOUND;
    }

    public String history(User client, String groupID) {
        StringBuilder strPayed = new StringBuilder();
        strPayed.append("PAYED:\n");
        if (hasGroup(groupID)) {
            Group group = groupHashMap.get(groupID);
            if (group.getOwner().equals(client.getUserID())) {
                for (GroupRequest groupReq : groupsRequestHashMap.values()) {
                    if (groupReq.getGroupID().equals(groupID) && groupReq.isDone(requestHashMap)) {
                        strPayed.append(groupReq).append("\n");
                    }
                }
                return strPayed.toString();
            } else {
                return NOT_GROUP_OWNER;
            }
        }
        return GROUP_NOT_FOUND;
    }
}
