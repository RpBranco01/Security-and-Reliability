package GroupDataBase.Data;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;



public class QRCode implements Serializable{
    private static final String CHARSET = "UTF-8";

    private int QRCodeID;
    private String QRCodeFilePath;
    private String data;

    public QRCode(int reqID, String qRCodeFilePath, String data) {
        QRCodeID = reqID;
        this.QRCodeFilePath = qRCodeFilePath;
        this.data = data;
        createQRCode(this.data, qRCodeFilePath, CHARSET, 200, 200);
    }

    public String getQRCodeFilePath() {
        return QRCodeFilePath;
    }

    // Function to create the QR code
    public void createQRCode(String data, String path, String charset, int height, int width){

        try{
            BitMatrix matrix = new MultiFormatWriter().encode(
                    new String(data.getBytes(charset), charset),
                    BarcodeFormat.QR_CODE, width, height);

            MatrixToImageWriter.writeToFile(
                    matrix,
                    path.substring(path.lastIndexOf('.') + 1),
                    new File(path));
        }catch(IOException | WriterException e){
            System.out.println(e.getMessage());
        }

    }

    // Function to read the QR file
    public static String readQRCode(String path) {
        try{
            BinaryBitmap binaryBitmap
                    = new BinaryBitmap(new HybridBinarizer(
                    new BufferedImageLuminanceSource(
                            ImageIO.read(
                                    new FileInputStream(path)))));

            Result result
                    = new MultiFormatReader().decode(binaryBitmap);

            return result.getText();
        } catch (IOException | NotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

}
