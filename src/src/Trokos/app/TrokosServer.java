package Trokos.app;

import GroupDataBase.BaseDeDados;
import GroupDataBase.Data.BDFactory;
import GroupDataBase.Data.Cifra;
import GroupDataBase.Data.User;
import ReadWrite.Message;
import javafx.util.Pair;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//Servidor myServer

public class TrokosServer {


    private static final String AUTHENTICATION_ERROR = "Authentication Error";
    private static final String AUTHENTICATION_SUCCESSFUL = "Authentication Successful";

    private static final String INVALID_AMOUNT = "INVALID AMOUNT";
    private static final String PAYMENT_DONE = "PAYMENT DONE";
    private static final String PAY_REQUEST_SUCCESSFUL = "PAY REQUEST SUCCESSFUL";

    private static final File USERS_FILE = new File("users.cif");

    private BaseDeDados bd;
    private Cifra cifra;
    private BDFactory bdFactory;

    // Create a Logger
    private static final Logger logger = Logger.getLogger(TrokosServer.class.getName());

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, ClassNotFoundException, UnrecoverableKeyException, CertificateException, KeyStoreException, InvalidKeyException {
        int port;
        String cipherPassword;
        String keyStore;
        String keyStorePassword;
        TrokosServer server = new TrokosServer();
        if (args.length == 3) {
            port = 45678;
            cipherPassword = args[0];
            keyStore = args[1];
            keyStorePassword = args[2];
        } else {
            port = Integer.parseInt(args[0]);
            cipherPassword = args[1];
            keyStore = args[2];
            keyStorePassword = args[3];
        }
        logger.log(Level.INFO, "Server initiated");
        server.startServer(port, cipherPassword, keyStore, keyStorePassword);
    }

    public void startServer(int port, String cipherPassword, String keyStore, String keyStorePassword)
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException, CertificateException, InvalidKeyException {

        final String PATH = "keystores/" + keyStore;
        System.setProperty("javax.net.ssl.keyStore", PATH);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);


        this.cifra = new Cifra(cipherPassword);
        if(Files.exists(Paths.get("bd.cif"))){
            this.bd = cifra.deCipherBD();
        }else{
            bd = new BaseDeDados();
        }
        bdFactory = new BDFactory(PATH, keyStorePassword);
        String firstBlockChain = "blockchain/block_" + 1 + ".blk";
        if (Files.exists(Paths.get(firstBlockChain)) && bdFactory.initializeBD(bd, cifra)) {
            logger.log(Level.INFO, "DataBase generated");
            bdFactory.initWriteBlock();
            bdFactory.setWriteBlock();
        } else {
            bdFactory.initWriteBlock();
            bdFactory.setWriteBlock();
        }

        ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
        try (SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(port)) {
            boolean endCondition = true;
            while (endCondition) {
                logger.log(Level.INFO, "Waiting for client");
                try {
                    Socket cliSocket = serverSocket.accept();
                    ServerThread newServerThread = new ServerThread(cliSocket);
                    newServerThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    endCondition = false;
                }
            }
        }
    }

    //Threads utilizadas para comunicacao com os clientes
    class ServerThread extends Thread {

        private final Socket socket;

        ServerThread(Socket inSoc) {
            socket = inSoc;
            logger.log(Level.INFO, "thread from server to each client");
        }


        /* checks the given message and, depending on its value, performs a certain action, returns a tuple
         * with the message and a possible qrcode if client wants it */
        private Pair<String, File> interpretsMessageServer(String[] message, File qRCode, User client, ObjectInputStream inStream, ObjectOutputStream out) throws IOException, ClassNotFoundException {
            String finalMessage = "";
            byte[] txByte;
            byte[] txSignByte;
            switch (message[0]) {
                case ("b"):
                case ("balance"):
                    int balance = client.getBalance();
                    finalMessage = String.valueOf(balance);
                    logger.log(Level.INFO, "Done Balance");
                    break;
                case ("m"):
                case ("makepayment"):
                    txByte = (byte[]) inStream.readObject();
                    txSignByte = (byte[]) inStream.readObject();
                    finalMessage = bd.makePayment(client, message[1], Integer.parseInt(message[2]));
                    if (finalMessage.equals(PAYMENT_DONE)) {
                        bdFactory.addTransaction(txByte, txSignByte);
                    }
                    logger.log(Level.INFO, "Done MakePayment");
                    break;
                case ("r"):
                case ("requestpayment"):
                    finalMessage = bd.requestPayment(client.getUserID(), message[1], Integer.parseInt(message[2]));
                    logger.log(Level.INFO, "Done RequestPayment");
                    break;
                case ("v"):
                case ("viewrequests"):
                    finalMessage = bd.viewRequests(client.getUserID());
                    logger.log(Level.INFO, "Done ViewRequests");
                    break;
                case ("p"):
                case ("payrequest"):
                    String getRequestMessage = bd.getRequest(Integer.parseInt(message[1]), client.getUserID());
                    if (getRequestMessage.equals("NOT FOUND")){
                        out.writeObject(getRequestMessage);
                        break;
                    }
                    out.writeObject(getRequestMessage);

                    txByte = (byte[]) inStream.readObject();
                    txSignByte = (byte[]) inStream.readObject();
                    // Passar txByte e txSignByte como argumentos fazer addTransaction
                    finalMessage = bd.payRequest(client, message[1]);

                    if (finalMessage.equals(PAY_REQUEST_SUCCESSFUL)) {
                        logger.log(Level.INFO, "Add Transaction");
                        bdFactory.addTransaction(txByte, txSignByte);
                    }
                    logger.log(Level.INFO, "Done PayRequest");
                    break;
                case ("o"):
                case ("obtainQRcode"):
                    finalMessage = bd.obtainQRCode(client, Integer.parseInt(message[1]));
                    if (!(finalMessage.equals(INVALID_AMOUNT))) {
                        String[] split = finalMessage.split(":");
                        qRCode = new File(split[0]);
                    }
                    logger.log(Level.INFO, "Done ObtainQRCode");
                    break;
                case ("c"):
                case ("confirmQRcode"):
                    String getQRCodeMessage = bd.getQRCode(Integer.parseInt(message[1]));
                    if(getQRCodeMessage.equals("NOT FOUND")){
                        out.writeObject(getQRCodeMessage);
                        break;
                    }
                    out.writeObject(getQRCodeMessage);

                    txByte = (byte[]) inStream.readObject();
                    txSignByte = (byte[]) inStream.readObject();
                    finalMessage = bd.confirmQRCode(client, message[1]);
                    if (finalMessage.equals(PAY_REQUEST_SUCCESSFUL)) {
                        bdFactory.addTransaction(txByte, txSignByte);
                    }
                    logger.log(Level.INFO, "Done ConfirmQRCode");
                    break;
                case ("n"):
                case ("newgroup"):
                    finalMessage = bd.newGroup(client, message[1]);
                    logger.log(Level.INFO, "Done NewGroup");
                    break;
                case ("a"):
                case ("addu"):
                    finalMessage = bd.addU(client, message[1], message[2]);
                    logger.log(Level.INFO, "Done AddUser");
                    break;
                case ("g"):
                case ("groups"):
                    finalMessage = bd.groups(client);
                    logger.log(Level.INFO, "Done Groups");
                    break;
                case ("d"):
                case ("dividepayment"):
                    finalMessage = bd.dividePayment(client, message[1], Integer.parseInt(message[2]));
                    logger.log(Level.INFO, "Done DividePayment");
                    break;
                case ("s"):
                case ("statuspayments"):
                    finalMessage = bd.statusPayments(client, message[1]);
                    logger.log(Level.INFO, "Done StatusPayment");
                    break;
                case ("h"):
                case ("history"):
                    finalMessage = bd.history(client, message[1]);
                    logger.log(Level.INFO, "Done History");
                    break;
            }
            return new Pair<>(finalMessage, qRCode);
        }

        /* A task that the server do and sends de value to the client, returns true if complete, false otherwise */
        private boolean doTask(User user, Message messageHandler, ObjectOutputStream out, ObjectInputStream in) {
            Pair<String, File> tuple = new Pair<>(null, null);
            try {
                String[] message = (String[]) in.readObject();
                if (message[0].equals("q") || message[0].equals("quit")) {
                    socket.close();
                    out.close();
                    in.close();
                    return false;
                } else {
                    tuple = interpretsMessageServer(message, tuple.getValue(), user, in, out);
                    if (tuple.getValue() == null) {
                        out.writeObject(tuple.getKey());
                        out.flush();
                    } else {
                        String[] split = tuple.getKey().split(":");
                        out.writeObject(split[1]);
                        out.flush();
                        messageHandler.sendFileMessage(tuple.getValue(), out);
                        if (tuple.getValue().delete()) {
                            logger.log(Level.INFO, "Deleted the file: {0}", tuple.getValue().getName());
                        }
                        out.flush();
                    }
                    cifra.cipherBD(bd);
                    logger.log(Level.INFO, "DataBase Ciphered");
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        }

        private String signUp(ObjectOutputStream outStream, ObjectInputStream inStream, byte[] nonce, String userID) {
            try {
                // enviar nonce e boolean
                outStream.writeObject(nonce);
                outStream.writeObject(true);
                outStream.flush();

                // recebe nonce cifrado e path para a certificação de chave pública
                byte[] receivedNonce = (byte[]) inStream.readObject();
                byte[] cipherNonce = (byte[]) inStream.readObject();
                byte[] certificateBytes = (byte[]) inStream.readObject();

                if (!(Arrays.equals(receivedNonce, nonce))) {
                    outStream.writeObject(AUTHENTICATION_ERROR);
                    outStream.flush();
                    socket.close();
                    return null;
                }

                String certPath = "certificates/" + userID + ".cer";
                try (FileOutputStream outputStream = new FileOutputStream(certPath)) {
                    outputStream.write(certificateBytes);
                    outputStream.flush();
                }

                // Cria certificado
                FileInputStream fis = new FileInputStream(certPath);
                CertificateFactory cf = CertificateFactory.getInstance("X509");
                Certificate cert = cf.generateCertificate(fis);

                // Verificar assinatura
                Signature s = Signature.getInstance("MD5withRSA");
                s.initVerify(cert.getPublicKey());
                s.update(receivedNonce);

                if (!s.verify(cipherNonce)) {
                    outStream.writeObject(AUTHENTICATION_ERROR);
                    outStream.flush();
                    socket.close();
                    return null;
                }
                logger.log(Level.INFO, "Done SignUp");
                return certPath;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }

        private boolean login(ObjectOutputStream outStream, ObjectInputStream inStream, byte[] nonce, String userID) {
            try {
                // enviar nonce e boolean
                outStream.writeObject(nonce);
                outStream.writeObject(false);
                outStream.flush();

                // recebe nonce cifrado e path para a certificação de chave pública
                byte[] receivedNonce = (byte[]) inStream.readObject();
                byte[] cipherNonce = (byte[]) inStream.readObject();
                String certPath = "certificates/" + userID + ".cer";

                // Se rNonce e nonce não forem iguais fecha o socket
                if (!(Arrays.equals(receivedNonce, nonce))) {
                    outStream.writeObject(AUTHENTICATION_ERROR);
                    outStream.flush();
                    socket.close();
                }

                // Cria certificado
                FileInputStream fis = new FileInputStream(certPath);
                CertificateFactory cf = CertificateFactory.getInstance("X509");
                Certificate cert = cf.generateCertificate(fis);

                // Verificar assinatura
                Signature s = Signature.getInstance("MD5withRSA");
                s.initVerify(cert.getPublicKey());
                s.update(receivedNonce);

                if (!s.verify(cipherNonce)) {
                    outStream.writeObject(AUTHENTICATION_ERROR);
                    outStream.flush();
                    socket.close();
                    return false;
                }
                logger.log(Level.INFO, "Done Login");
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public void run() {
            try {
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                Message messageHandler = bd.getMessageHandler();


                HashMap<String, String> users;
                if (USERS_FILE.exists()) {
                    users = cifra.deCipherUsers();
                } else {
                    users = new HashMap<>();
                }

                // Get userID and passwd from client
                String userID = messageHandler.getUserCredentials(inStream);

                boolean isNew;
                if (users.isEmpty()) {
                    isNew = true;
                } else {
                    isNew = !users.containsKey(userID);
                }

                // Criação do nonce
                byte[] nonce = new byte[8];
                new SecureRandom().nextBytes(nonce);
                // -------------------------------

                User user;
                boolean isAuthentic = true;
                // Se é novo
                if (isNew) {
                    // Chama função ‘signup’ se correr bem retorna certificatePath
                    String certificatePath = signUp(outStream, inStream, nonce, userID);
                    if (certificatePath == null) {
                        isAuthentic = false;
                    }
                    users.put(userID, certificatePath);
                    user = new User(userID);
                    user.updateBalance(100);
                    bd.addUserHashMap(user);
                    cifra.cipherBD(bd);
                } else {
                    isAuthentic = login(outStream, inStream, nonce, userID);
                    if (isAuthentic) {
                        user = bd.getUser(userID);
                    } else {
                        user = null;
                    }
                }
                cifra.cipherUsers(users);


                outStream.writeObject(AUTHENTICATION_SUCCESSFUL);
                outStream.flush();

                cifra.cipherBD(bd);

                // Do cycle to always receive commands
                boolean doTask = true;
                while (isAuthentic && doTask) {
                    doTask = doTask(user, messageHandler, outStream, inStream);
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.log(Level.WARNING, e.getMessage());
                try {
                    socket.close();
                } catch (IOException eSocket) {
                    eSocket.printStackTrace();
                }
            }
        }
    }
}
