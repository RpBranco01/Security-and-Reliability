package Trokos.app;

import GroupDataBase.Data.QRCode;
import ReadWrite.Message;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Trokos {

    private static final String ERROR_MESSAGE = "Authentication Error";
    private static final String SUCCESS_MESSAGE = "Authentication Successful";

    // Create a Logger
    private static final Logger logger
            = Logger.getLogger(
            Trokos.class.getName());


    public static String[] getHostPort(String hostAndPort) {
        return hostAndPort.split(":");

    }

    public static void main(String[] args) {
        logger.log(Level.INFO, "Initiating client");

        // Get host and port separately
        String[] hostPortDivided;
        if (args[0].contains(":")) {
            hostPortDivided = getHostPort(args[0]); //hostname:port
        } else {
            hostPortDivided = getHostPort(args[0] + ":45678");
        }


        //Creates a new client
        Trokos client = new Trokos();
        client.startClient(hostPortDivided, args[1], args[2], args[3], args[4]); // ([hostname, port], user, passwd)
    }


    public void startClient(String[] hostPort, String trustStore, String keyStore, String keyStorePassword, String user) {
        // Input and Output stream to send information throughout client-server
        ObjectInputStream in;
        ObjectOutputStream out;

        System.setProperty("javax.net.ssl.trustStore", "keystores/" + trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", "clientTruststore"); //password da trust store

        SocketFactory sf = SSLSocketFactory.getDefault();

        // Creates a new client socket with host
        try (SSLSocket cliSocket = (SSLSocket) sf.createSocket(hostPort[0], Integer.parseInt(hostPort[1]))) {
            in = new ObjectInputStream(cliSocket.getInputStream());
            out = new ObjectOutputStream(cliSocket.getOutputStream());

            out.writeObject(user);
            // Recebe nonce e uma flag
            byte[] nonce = (byte[]) in.readObject();
            boolean isNew = (boolean) in.readObject();

            final String PATH = "keystores/" + keyStore;
            FileInputStream kfile = new FileInputStream(PATH); //keystore
            KeyStore kstore = KeyStore.getInstance("JKS");
            kstore.load(kfile, keyStorePassword.toCharArray()); //password para aceder à keystore

            //Assinar nonce
            Signature s = Signature.getInstance("MD5withRSA");
            s.initSign((PrivateKey) kstore.getKey(user, keyStorePassword.toCharArray()));
            s.update(nonce);
            byte[] signNonce = s.sign();

            if (isNew) {
                Certificate cert = kstore.getCertificate(user);
                byte[] certByte = cert.getEncoded();
                out.writeObject(nonce);
                out.writeObject(signNonce);
                out.writeObject(certByte);
            } else {
                out.writeObject(nonce);
                out.writeObject(signNonce);
            }


            String fromServer = (String) in.readObject();
            logger.log(Level.INFO, fromServer);
            if (fromServer.equals(SUCCESS_MESSAGE)) {
                boolean[] b = new boolean[1];
                b[0] = true;

                // See how to end this TODO
                while (b[0]) {
                    printMenu();
                    selectOption(b, in, out, (PrivateKey) kstore.getKey(user, keyStorePassword.toCharArray()), user);
                }
            } else {
                System.exit(-1);
            }

        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
            logger.log(Level.WARNING, e.getMessage());
            System.exit(-1);
        } catch (UnrecoverableKeyException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

    }

    private static void printMenu() {
        System.out.println("Menu:");

        //DA PARA POR NUM FOR EACH
        System.out.println("  balance");

        System.out.println("  makepayment <userID> <amount>");

        System.out.println("  requestpayment <userID> <amount>");

        System.out.println("  viewrequests ");

        System.out.println("  payrequest <reqID>");

        System.out.println("  obtainQRcode <amount>");

        System.out.println("  confirmQRcode <QRcode>");

        System.out.println("  newgroup <groupID>");

        System.out.println("  addu <userID> <groupID>");

        System.out.println("  groups");

        System.out.println("  dividepayment <groupID> <amount>");

        System.out.println("  statuspayments <groupID>");

        System.out.println("  history <groupID>");

        System.out.println("  quit");
    }


    private static void selectOption(boolean[] b, ObjectInputStream in, ObjectOutputStream out, PrivateKey pk, String userID)
            throws IOException, ClassNotFoundException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        // Enter data using BufferReader
        System.out.print("\nSelect option: ");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));

        // Reading data using readLine
        String[] opt = reader.readLine().split(" ");
        Signature s;
        String trans;
        byte[] txByte;



        switch (opt[0]) {
            case ("b"):
            case ("balance"):
                System.out.println("Doing <balance>...");
                out.writeObject(opt);
                out.flush();
                String balance = (String) in.readObject();
                System.out.println("My balance is : " + balance);
                System.out.println();
                break;
            case ("m"):
            case ("makepayment"):
                System.out.println("Doing <makepayment>...");

                trans = opt[1] + "," + opt[2] + "," + userID;
                txByte = trans.getBytes();

                s = Signature.getInstance("MD5withRSA");
                s.initSign(pk);
                s.update(txByte);

                out.writeObject(opt);
                out.writeObject(txByte);
                out.writeObject(s.sign()); //txSignByte
                out.flush();

                String makePaymentAnswer = (String) in.readObject();
                System.out.println(makePaymentAnswer);
                System.out.println();
                break;
            case ("r"):
            case ("requestpayment"):
                System.out.println("Doing <requestpayment>...");
                out.writeObject(opt);
                out.flush();
                String requestPaymentAnswer = (String) in.readObject();
                System.out.println(requestPaymentAnswer);
                System.out.println();
                break;
            case ("v"):
            case ("viewrequests"):
                System.out.println("Doing <viewrequests>...");
                out.writeObject(opt);
                out.flush();
                String viewRequestsAnswer = (String) in.readObject();
                String[] split5 = viewRequestsAnswer.split(";");
                for (int i = 0; i < split5.length; i++) {
                    System.out.println(split5[i]);
                    i++;
                }
                System.out.println();
                break;
            case ("p"):
            case ("payrequest"):
                System.out.println("Doing <payrequest>...");

                out.writeObject(opt);
                String message = (String) in.readObject();
                if(message.equals("NOT FOUND")){
                    System.out.println(message);
                    System.out.println(in.readObject());
                    break;
                }
                String[] split1 = message.split(";");
                String[] split4 = split1[1].split(":");

                trans = split4[2] + "," + split4[1] + "," + userID;
                txByte = trans.getBytes();

                s = Signature.getInstance("MD5withRSA");
                s.initSign(pk);
                s.update(txByte);


                out.writeObject(txByte);
                out.writeObject(s.sign()); //txSignByte
                out.flush();

                String payRequestAnswer = (String) in.readObject();
                System.out.println(payRequestAnswer);
                System.out.println();
                break;
            case ("o"):
            case ("obtainQRcode"):
                System.out.println("Doing <obtainQRcode>...");
                out.writeObject(opt);
                out.flush();
                String obtainRequestAnswer = (String) in.readObject();
                if (!(obtainRequestAnswer.equals("INVALID AMOUNT"))) {
                    String filepath = "src/ProjectFiles/clientfiles/QRCode"+obtainRequestAnswer+".png";
                    File qRCodeFile = new File(filepath);
                    if (qRCodeFile.createNewFile()) {
                        System.out.println("QRCodeFile created");
                    } else {
                        PrintWriter writer = new PrintWriter(qRCodeFile);
                        writer.print("");
                        writer.close();
                        System.out.println("QRCode file already created");
                    }
                    Message.receiveFileMessage(qRCodeFile, in);
                    String data = QRCode.readQRCode(filepath);
                    String[] split3 = data.split(":");
                    System.out.println("QRCode Info:\n\n Request ID: "+split3[0]+ " | QRCode Creator: "+ split3[1]+ " | Amount: "+ split3[2]);
                } else {
                    System.out.println(obtainRequestAnswer);
                }
                System.out.println();
                break;
            case ("c"):
            case ("confirmQRcode"):
                System.out.println("Doing <confirmQRcode>...");

                out.writeObject(opt);
                String message2 = (String) in.readObject();
                if (message2.equals("NOT FOUND")){
                    System.out.println(message2);
                    System.out.println(in.readObject());
                    break;
                }
                String[] split2 = message2.split(";");
                String[] split6 = split2[1].split(":");

                trans = split6[2] + "," + split6[1] + "," + userID;
                txByte = trans.getBytes();

                s = Signature.getInstance("MD5withRSA");
                s.initSign(pk);
                s.update(txByte);


                out.writeObject(txByte);
                out.writeObject(s.sign()); //txSignByte
                out.flush();
                String confirmQRCodeAnswer = (String) in.readObject();
                System.out.println(confirmQRCodeAnswer);
                System.out.println();
                break;
            case ("n"):
            case ("newgroup"):
                System.out.println("Doing <newgroup>...");
                out.writeObject(opt);
                out.flush();
                String newGroupAnswer = (String) in.readObject();
                System.out.println(newGroupAnswer);
                System.out.println();
                break;
            case ("a"):
            case ("addu"):
                System.out.println("Doing <addu>...");
                out.writeObject(opt);
                out.flush();
                String addUAnswer = (String) in.readObject();
                System.out.println(addUAnswer);
                System.out.println();
                break;
            case ("g"):
            case ("groups"):
                System.out.println("Doing <groups>...");
                out.writeObject(opt);
                out.flush();
                String groupsAnswer = (String) in.readObject();
                System.out.println(groupsAnswer);
                System.out.println();
                break;
            case ("d"):
            case ("dividepayment"):
                System.out.println("Doing <dividepayment>...");
                out.writeObject(opt);
                out.flush();
                String dividePaymentAnswer = (String) in.readObject();
                System.out.println(dividePaymentAnswer);
                System.out.println();
                break;
            case ("s"):
            case ("statuspayments"):
                System.out.println("Doing <statuspayments>...");
                out.writeObject(opt);
                out.flush();
                String statusPaymentAnswer = (String) in.readObject();
                System.out.println(statusPaymentAnswer);
                System.out.println();
                break;
            case ("h"):
            case ("history"):
                System.out.println("Doing <history>...");
                out.writeObject(opt);
                out.flush();
                String historyAnswer = (String) in.readObject();
                System.out.println(historyAnswer);
                System.out.println();
                break;
            case ("q"):
            case ("quit"):
                System.out.println("Doing <quit>...");
                out.writeObject(opt);
                out.flush();
                b[0] = false;
                break;
            default:
                System.out.println("Option not available!");
        }
    }
}
