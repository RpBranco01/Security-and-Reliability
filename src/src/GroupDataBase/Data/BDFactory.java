package GroupDataBase.Data;

import GroupDataBase.BaseDeDados;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;

public class BDFactory {

    private BlockChain blockChain;

    public BDFactory(String path, String keyStorePassword){
        byte[] array = new byte[32];
        for(int i = 0; i < array.length; i++){
            Arrays.fill(array, (byte) '0');
        }
        try(FileInputStream kfile = new FileInputStream(path);){
            KeyStore kstore = KeyStore.getInstance("JKS");
            kstore.load(kfile, keyStorePassword.toCharArray());
            blockChain = new BlockChain(array, (PrivateKey) kstore.getKey("myServer", keyStorePassword.toCharArray()));
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException e) {
            e.printStackTrace();
        }
    }

    public boolean initializeBD(BaseDeDados bd, Cifra cifra) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, CertificateException, InvalidKeyException {
        HashMap<String, String> users;
        if (Files.exists(Paths.get("users.cif"))) {
            users = cifra.deCipherUsers();
            for (String userID : users.keySet()) {
                User user = bd.getUser(userID);
                user.updateBalance(100);
                bd.addUserHashMap(user);
            }
            cifra.cipherUsers(users);
        } else {
            return false;
        }
        int numBlocks = blockChain.getTotalBLK();
        for (int i = 1; i <= numBlocks; i++) {
            if (verifyBlock(i, bd)) {
                return false;
            }
        }
        return true;
    }

    public boolean verifyBlock(int blockIndex, BaseDeDados bd) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, CertificateException, InvalidKeyException {
        blockChain.initVerify(blockIndex);
        byte[] hash = blockChain.getHash();
        int numBlock = blockChain.getNumBlock();

        boolean goodBlock = false;
        if (numBlock == blockIndex) {
            byte[] previousHash = blockChain.getPreviousHash();
            if (Arrays.equals(previousHash, hash)) {
                goodBlock = true;
            }
        }
        String[] transaction;
        int numTrans = blockChain.getNumTrans();
        if (goodBlock) {
            for (int i = 0; i < numTrans; i++) {
                transaction = blockChain.verifyTransaction();
                if (transaction.length != 0) {
                    User giver = bd.getUser(transaction[2]);
                    bd.makePayment(giver, transaction[0], Integer.parseInt(transaction[1]));
                }
            }
        }
        if(numTrans != 5){
            return false;
        }
        return !blockChain.verifyFinalSign();
    }

    public void initWriteBlock() throws IOException, InvalidKeyException {
        blockChain.initWriteBlock();
    }

    public void setWriteBlock() throws IOException {
        blockChain.setWriteBlock();
    }

    public synchronized void addTransaction(byte[] txByte, byte[] txSignByte) {
        blockChain.addTransaction(txByte, txSignByte);
    }
}
