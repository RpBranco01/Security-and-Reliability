package ReadWrite;

import java.io.*;
import java.nio.file.Files;

public class Message implements Serializable{

    /* With the given file and the ObjectInputStream receives the file size in bytes and the file in bytes*/
    public static void receiveFileMessage(File file, ObjectInputStream in) {
        try {
            long len = (long) in.readObject();
            System.out.println("Reading a file with " + len + "kbs");
            byte[] messageContent = new byte[(int) len];
            int received = 0;
            while (received < (int) len) {
                received += in.read(messageContent, received, (int) len - received);
                System.out.println("Receiving " + received + "kbs");
            }
            Files.write(file.toPath(), messageContent);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    /* With the given file and the ObjectOutputStream send the file size in bytes and the file in bytes*/
    public void sendFileMessage(File message, ObjectOutputStream out) {
        try {
            long len = Files.size(message.toPath());
            out.writeObject(len);

            out.write(Files.readAllBytes(message.toPath()));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    /*Gets the user credentials*/
    public String getUserCredentials(ObjectInputStream inStream) {
        String myUser = null;
        try {
            myUser = (String) inStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return myUser;
    }
}
