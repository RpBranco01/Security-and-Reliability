package GroupDataBase.Data;

import java.io.Serializable;

public class Request implements Serializable {
    private int reqID;
    private QRCode qRCode;
    private int amount;
    private String receiverID;
    private String senderID;
    private Boolean isDone = false;

    public Request(int reqID, String receiverID, String senderID ,int amount,boolean wantQRCode){
        this.reqID = reqID;
        this.amount = amount;
        this.senderID = senderID;
        this.receiverID = receiverID;
        if(wantQRCode){
            String data = reqID + ":" + receiverID + ":" + amount;
            this.qRCode = new QRCode(reqID,"src\\ProjectFiles\\serverfiles\\QRCodeFiles\\QRCode"+reqID+".png", data);
        }else{
            qRCode = null;
        }
    }

    public String getSenderID() {
        return senderID;
    }

    public QRCode getqRCode() {
        return qRCode;
    }

    public String getReceiverID() {
        return receiverID;
    }

    public int getAmount() {
        return amount;
    }

    public int getReqID() {
        return reqID;
    }


    public String toString(){
        return "Request ID: "+reqID + " | Amount: " + amount + " | Sender: " + senderID + " | Receiver: " + receiverID + ";" + reqID + ":" + amount + ":" + senderID;
    }

    public Boolean isDone() {
        return isDone;
    }

    public synchronized void setIsDone() { isDone = true; }
}
