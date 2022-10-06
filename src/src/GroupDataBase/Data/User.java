package GroupDataBase.Data;

import java.io.Serializable;
import java.util.*;

public class User implements Serializable {
    private final String userID;
    private int balance;
    private List<Group> belongedGroups;
    private List<Request> requests;

    public User(String username) {
        this.userID = username;
        this.balance = 0;
        belongedGroups = new ArrayList<>();
        requests = new ArrayList<>();
    }


    public synchronized void updateBalance(int balance) {
        this.balance = balance;
    }

    public String getUserID() {
        return userID;
    }

    public int getBalance() {
        return balance;
    }

    public List<Group> getBelongedGroups() {
        return belongedGroups;
    }

    public List<Request> getRequests() {
        return requests;
    }

    public synchronized void addRequest(Request request){
        if(!this.requests.contains(request)){
            this.requests.add(request);
        }
    }

    public void setRequests(List<Request> requests){
        this.requests = requests;
    }

    public synchronized String takeAmount(int amount){
        if (this.getBalance() < amount){
            // Throws error
            return "INSUFFICIENT BALANCE";
        }
        this.updateBalance(this.getBalance()-amount);

        return String.valueOf(this.getBalance());
    }

    public synchronized void putAmount(int amount){
        this.updateBalance(this.getBalance()+amount);
    }

    public void addGroup(Group group) {
        this.belongedGroups.add(group);
    }

    @Override
    public String toString(){

        return userID + ":" + ":" +
                getBalance();
    }
}
