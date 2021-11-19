package server;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class Message {
    private String cypherText;
    private int idx;
    private byte[] tag;
    private final int SIZE_BB = 20;


    public Message(String text, SecretKey secretKey) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        this.cypherText = generateCypherText(text, secretKey);
        this.idx = generateRandomIndex();
        this.tag = generateTag();
    }

    private String generateCypherText(String text, SecretKey secretKey) {
        //generate cyphertext with secret key
        return "";
    }

    public byte[] generateTag() throws NoSuchAlgorithmException {
        //random byteArray 256 bit
        byte[] bytes = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(bytes);
        return bytes;
    }

    private SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA1");
        return keyGen.generateKey();
    }

    private int generateRandomIndex() {
        Random r = new Random();
        return r.nextInt(SIZE_BB);
    }

    public String getCypherText() {
        return cypherText;
    }

    public int getIdx() {
        return idx;
    }

    public byte[] getTag() {
        return tag;
    }
}
