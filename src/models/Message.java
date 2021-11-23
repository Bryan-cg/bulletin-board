package models;

import javax.crypto.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

public class Message {
    private byte[] textBytes;
    private byte[] idx;
    private byte[] tag;
    private byte[] encryptedMessage;
    private final int SIZE_BB = 20;
    private final String CIPHER_INSTANCE;


    public Message(String text, SecretKey secretKey, String CIPHER_INSTANCE) {
        this.CIPHER_INSTANCE = CIPHER_INSTANCE;
        try {
            this.textBytes = generateTextBytes(text);
            this.idx = generateRandomIndex();
            this.tag = generateTag();
            this.encryptedMessage = generateHashedResult(secretKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] generateTextBytes(String text) {
        return textBytes = text.getBytes();
    }

    private byte[] generateTag() throws NoSuchAlgorithmException {
        //random byteArray 256 bit
        byte[] bytes = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(bytes);
        return bytes;
    }

    private byte[] generateRandomIndex() {
        final Random r = new Random();
        int randomIndex = r.nextInt(SIZE_BB);
        return BigInteger.valueOf(randomIndex).toByteArray();
    }

    private byte[] generateHashedResult(SecretKey secretKey) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        final byte[] totalByteArr = combineByteArrays(this.idx, this.tag,this.textBytes);
        final Cipher cipher;
        cipher = Cipher.getInstance(CIPHER_INSTANCE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(totalByteArr);
    }

    private byte[] combineByteArrays(byte[] a, byte[] b, byte[] c) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(a);
        outputStream.write(b);
        outputStream.write(c);
        return outputStream.toByteArray();
    }

    public byte[] getTextBytes() {
        return textBytes;
    }

    public byte[] getIdx() {
        return idx;
    }

    public byte[] getTag() {
        return tag;
    }

    public byte[] getEncryptedMessage() {
        return encryptedMessage;
    }

}
