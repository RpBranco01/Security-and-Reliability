package GroupDataBase.Data;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

public class BlockChain implements Serializable {

    private static final String PATH = "blockchain/block_";

    private byte[] previousHash;
    private int numBlock; //should initialize with = 1;
    private int countTransaction = 0; // init 0

    private MessageDigest hashMD;
    private Signature signSignature;
    private Signature verifySignature;
    private FileOutputStream fileCreateBLK;
    private ObjectOutputStream oosa;
    private FileInputStream fileVerifyBLK;
    private ObjectInputStream oisv;
    private PrivateKey pk;
    private List<byte[]> txBytesAndSign;


    public BlockChain(byte[] previousHash, PrivateKey pk) throws NoSuchAlgorithmException {
        this.previousHash = previousHash;
        this.hashMD = MessageDigest.getInstance("SHA-256");
        this.signSignature = Signature.getInstance("MD5withRSA");
        this.verifySignature = Signature.getInstance("MD5withRSA");
        this.numBlock = 1;
        this.pk = pk;
    }

    public byte[] getPreviousHash() {
        return this.previousHash;
    }

    /* opens FIS and OIS to the block requested and make init of Signature with
     server public key*/
    public void initVerify(int blockIndex) throws IOException, NoSuchAlgorithmException, CertificateException, InvalidKeyException {
        this.fileVerifyBLK = new FileInputStream(PATH + blockIndex + ".blk");
        this.oisv = new ObjectInputStream(fileVerifyBLK);
        this.hashMD = MessageDigest.getInstance("SHA-256");
        txBytesAndSign = new ArrayList<>();

        FileInputStream fis = new FileInputStream("keystores/certServer.cer");
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        Certificate certificate = cf.generateCertificate(fis);

        this.signSignature.initSign(pk);
        this.verifySignature.initVerify(certificate.getPublicKey());
    }

    /* opens the FOS and the OOS to the current numBlock and make init of Signature with
     server private key*/
    public void initWriteBlock() throws IOException, InvalidKeyException {
        this.fileCreateBLK = new FileOutputStream(PATH + numBlock + ".blk");
        this.oosa = new ObjectOutputStream(fileCreateBLK);
        signSignature.initSign(pk);
        try {
            this.hashMD = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // Sets the OOS writing the previousHash, the numBlock and the countTransaction //TODO  e se fechar a meio??
    public void setWriteBlock() throws IOException {
        if(countTransaction != 0){
            this.fileCreateBLK = new FileOutputStream(PATH + numBlock + ".blk");
            this.oosa = new ObjectOutputStream(fileCreateBLK);

            oosa.writeObject(previousHash);
            oosa.flush();
            oosa.writeObject(numBlock);
            oosa.flush();
            oosa.writeObject(countTransaction);
            oosa.flush();
            for (byte[] content : txBytesAndSign) {
                oosa.writeObject(content);
                oosa.flush();
            }
        }else{
            doFirstThree();
        }
    }

    public void doFirstThree() throws IOException {
        oosa.writeObject(previousHash);
        oosa.flush();
        oosa.writeObject(numBlock);
        oosa.flush();
        oosa.writeObject(countTransaction);
        oosa.flush();
    }

    // rewrite the content of the current .blk to the _temp.blk changing the current transaction and change its name
    public void rewriteContentAndAddTransactions() throws IOException, ClassNotFoundException {
        this.oosa.close();
        this.fileCreateBLK.close();
        FileInputStream fis = new FileInputStream(PATH + numBlock + ".blk");
        ObjectInputStream ois = new ObjectInputStream(fis);

        for (int i = 0; i < 3; i++) {
            ois.readObject();
        }

        this.txBytesAndSign = new ArrayList<>();
        for (int i = 0; i < countTransaction; i++) {
            txBytesAndSign.add((byte[]) ois.readObject()); //transactions
            txBytesAndSign.add((byte[]) ois.readObject()); //transactions
        }
        if(countTransaction == 5){
            txBytesAndSign.add((byte[]) ois.readObject());
        }
        ois.close();
        fis.close();
        this.fileCreateBLK = new FileOutputStream(PATH + numBlock + ".blk");
        this.oosa = new ObjectOutputStream(fileCreateBLK);
        doFirstThree();
        for (byte[] content : txBytesAndSign) {
            oosa.writeObject(content);
            oosa.flush();
        }
    }

    // with the OIS of the current .blk gets the hash (used in bd only once per block)
    public byte[] getHash() throws IOException, ClassNotFoundException {
        return (byte[]) oisv.readObject();
    }

    // with the OIS of the current .blk gets the number of the block (used in bd only once per block)
    public int getNumBlock() throws IOException, ClassNotFoundException { // Get current block
        return (int) oisv.readObject();
    }

    // with the OIS of the current .blk gets the number of transfers (used in bd only once per block)
    public int getNumTrans() throws IOException, ClassNotFoundException {
        return (int) oisv.readObject();
    }

    // do while cicle to count how many files with .blk there are
    public int getTotalBLK() {
        int i = 1;

        while (Files.exists(Paths.get(PATH + i + ".blk"))) {
            i++;
        }
        return i - 1;
    }

    /* adds a new transaction writing the object in the OOS of the current .blk
     also update the sign and the hash, if it has been done 5 transactions close
     the OOS and the FOS and open the next one*/
    public void addTransaction(byte[] txBytes, byte[] txSignBytes) {
        try {

            this.oosa.writeObject(txBytes);
            this.oosa.flush();
            this.oosa.writeObject(txSignBytes);
            this.oosa.flush();

            byte[] nextBlockHash = makeSignAndHash(txBytes, txSignBytes, true);


            // Create new BLK
            if (nextBlockHash != null) {
                this.previousHash = nextBlockHash;
                this.oosa.close();
                this.fileCreateBLK.close();
                initWriteBlock();
                setWriteBlock();
            }
        } catch (IOException | SignatureException | InvalidKeyException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Verify if the last .blk sign and the sign generated doing makeSignAndHash are the sane
    public boolean verifyFinalSign() throws IOException, ClassNotFoundException {
        byte[] readSign = (byte[]) oisv.readObject();
        boolean isVerified = false;
        try{
            isVerified = verifySignature.verify(readSign);
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return isVerified;
    }

    /* updates the hash and the sign, increments the counTransaction and rewrites the content to add a new transaction
     if countTransaction is 5 then depending on if is verifying or not writes the sign on OOS and returns the hash
     generated*/
    public byte[] makeSignAndHash(byte[] txBytes, byte[] txSignBytes, boolean isToAddTx) throws SignatureException, IOException, ClassNotFoundException {
        // Update Hash final - 32
        hashMD.update(txBytes);
        hashMD.update(txSignBytes);

        // Update Signature final - 256
        signSignature.update(txBytes);
        signSignature.update(txSignBytes);

        if(!isToAddTx){
            txBytesAndSign.add(txBytes);
            txBytesAndSign.add(txSignBytes);

            verifySignature.update(txBytes);
            verifySignature.update(txSignBytes);
        }

        // Increment Transaction Count
        countTransaction++;

        byte[] blockHash = null;
        if (countTransaction == 5) {
            byte[] signBytes = signSignature.sign();
            if (isToAddTx) {
                oosa.writeObject(signBytes);
                oosa.flush();
                rewriteContentAndAddTransactions();
            }
            hashMD.update(signBytes);
            blockHash = hashMD.digest();
            numBlock++;
            countTransaction = 0;
        } else {
            if(isToAddTx){
                rewriteContentAndAddTransactions();
            }
        }
        return blockHash;
    }

    /* reads from the OIS the txBytes and the txSignBytes of a transaction and verifies it with the
     public key of the user of created it. Also updates the hash and the sign to the final server sign check
     and saves the hash generated (if generated) in a global variable previousHash */
    public String[] verifyTransaction() {
        try {
            boolean doTrans = false;

            byte[] txBytes = (byte[]) oisv.readObject();
            byte[] txSignBytes = (byte[]) oisv.readObject();

            String trans = new String(txBytes);
            String[] transParams = trans.split(",");
            try (FileInputStream certificateFile = new FileInputStream("certificates/" + transParams[2] + ".cer")) {
                CertificateFactory cf = CertificateFactory.getInstance("X509");
                Certificate certificate = cf.generateCertificate(certificateFile);
                Signature signature = Signature.getInstance("MD5withRSA");
                signature.initVerify(certificate.getPublicKey());
                signature.update(txBytes);
                doTrans = signature.verify(txSignBytes);
            } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException e) {
                e.printStackTrace();
            }

            byte[] previousHashProduced = makeSignAndHash(txBytes, txSignBytes, false);

            if (previousHashProduced != null) {
                this.previousHash = previousHashProduced;
            }

            if (doTrans) {
                return transParams;
            } else {
                return new String[0];
            }

        } catch (IOException | SignatureException | ClassNotFoundException e) {
            e.printStackTrace();
            return new String[0];
        }
    }
}
