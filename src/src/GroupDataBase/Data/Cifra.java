package GroupDataBase.Data;

import GroupDataBase.BaseDeDados;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;

public class Cifra {

    private static final String PBE = "PBEWithHmacSHA256AndAES_128";

    private byte[] params;
    private SecretKey key;
    private Cipher cipher;

    public Cifra(String serverPassword){
        byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };
        PBEKeySpec keySpec = new PBEKeySpec(serverPassword.toCharArray(), salt, 20); // pass, salt, iterations
        try{
            SecretKeyFactory kf = SecretKeyFactory.getInstance(PBE);
            this.key = kf.generateSecret(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

    }

    public synchronized void cipherUsers(HashMap<String, String> users) {
        /*Cifrar o users.txt*/
        try{
            this.cipher = Cipher.getInstance(PBE);
            this.cipher.init(Cipher.ENCRYPT_MODE, this.key);
            this.params = cipher.getParameters().getEncoded();
            writeParamsUsersToFile();


            FileOutputStream fos = new FileOutputStream("users.cif");
            CipherOutputStream cos = new CipherOutputStream(fos, this.cipher);
            ObjectOutputStream oos = new ObjectOutputStream(cos);

            oos.writeObject(users);

            oos.close();
            cos.close();
            fos.close();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public HashMap<String, String> deCipherUsers() {
        /*Decifrar users.cif*/
        try{
            if(Files.exists(Paths.get("params.user"))) {
                getParamsUsersFromFile();
            }

            AlgorithmParameters p = AlgorithmParameters.getInstance(PBE);
            p.init(this.params);
            Cipher d = Cipher.getInstance(PBE);
            d.init(Cipher.DECRYPT_MODE, this.key, p);

            FileInputStream fic = new FileInputStream("users.cif");
            CipherInputStream cis = new CipherInputStream(fic, d);
            ObjectInputStream ois = new ObjectInputStream(cis);

            return (HashMap<String, String>) ois.readObject();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private void writeParamsUsersToFile(){
        try(FileOutputStream fileOut = new FileOutputStream("params.user")) {
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(this.params);
            objectOut.flush();
            objectOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getParamsUsersFromFile() {
        try (FileInputStream fileIn = new FileInputStream("params.user")) {
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);

            this.params = (byte[]) objectIn.readObject();
            objectIn.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void writeParamsBDToFile(){
        try(FileOutputStream fileOut = new FileOutputStream("params.bd")) {
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(this.params);
            objectOut.flush();
            objectOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getParamsBDFromFile() {
        try (FileInputStream fileIn = new FileInputStream("params.bd")) {
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);

            this.params = (byte[]) objectIn.readObject();
            objectIn.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public synchronized void cipherBD(BaseDeDados bd){
        try(FileOutputStream fileOut = new FileOutputStream("bd.cif")) {
            this.cipher = Cipher.getInstance(PBE);
            this.cipher.init(Cipher.ENCRYPT_MODE, this.key);
            this.params = cipher.getParameters().getEncoded();
            writeParamsBDToFile();

            CipherOutputStream cis = new CipherOutputStream(fileOut, cipher);
            ObjectOutputStream ois = new ObjectOutputStream(cis);

            ois.writeObject(bd);
            ois.flush();
            ois.close();
        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public BaseDeDados deCipherBD(){
        if(Files.exists(Paths.get("params.bd"))) {
            getParamsBDFromFile();
        }
        try(FileInputStream fileOut = new FileInputStream("bd.cif")) {
            AlgorithmParameters p = AlgorithmParameters.getInstance(PBE);
            p.init(this.params);
            Cipher d = Cipher.getInstance(PBE);
            d.init(Cipher.DECRYPT_MODE, this.key, p);

            CipherInputStream cis = new CipherInputStream(fileOut, d);
            ObjectInputStream ois = new ObjectInputStream(cis);

            BaseDeDados bd = (BaseDeDados) ois.readObject();
            ois.close();
            return bd;
        } catch (IOException | InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}