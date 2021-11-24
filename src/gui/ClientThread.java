package gui;

import server.BulletinBoard;
import shared.Chat;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class ClientThread extends Thread {
    private final Chat bulletinBoard;
    private volatile byte[] receiverIdx = null;
    private volatile byte[] receiverTag = null;
    private volatile SecretKey receiverSecretKey = null;
    private final JTextArea messageArea;
    private final String CIPHER_INSTANCE;

    public ClientThread(Chat bulletinBoard, byte[] receiverIdx, byte[] receiverTag, SecretKey receiverSecretKey, JTextArea messageArea, String CIPHER_INSTANCE) {
        this.bulletinBoard = bulletinBoard;
        this.receiverIdx = receiverIdx;
        this.receiverTag = receiverTag;
        this.receiverSecretKey = receiverSecretKey;
        this.messageArea = messageArea;
        this.CIPHER_INSTANCE = CIPHER_INSTANCE;
    }

    public void setReceiverIdx(byte[] receiverIdx) {
        this.receiverIdx = receiverIdx;
    }

    public void setReceiverTag(byte[] receiverTag) {
        this.receiverTag = receiverTag;
    }

    public void setReceiverSecretKey(SecretKey receiverSecretKey) {
        this.receiverSecretKey = receiverSecretKey;
    }

    public String receive() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] totalMessage = bulletinBoard.get(receiverIdx, receiverTag);
        String message;
        if (totalMessage != null) {
            message = decryptTotalMessage(totalMessage);
            keyDeriviationFunction(receiverSecretKey);
            return message;
        }
        return null;
    }

    //TODO update receiverIdx and receiverTag from message content and return the actual message
    private String decryptTotalMessage(byte[] totalMessage) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        final Cipher cipher;
        cipher = Cipher.getInstance(CIPHER_INSTANCE);
        cipher.init(Cipher.DECRYPT_MODE, receiverSecretKey);
        return splitTotalMessage(cipher.doFinal(totalMessage));
    }

    private String splitTotalMessage(byte[] fullMessage) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] decryptedIdx = new byte[1];
        byte[] decryptedTag = new byte[32];
        ArrayList<Byte> decryptedMessage = new ArrayList<>(); //Needs to be message size --> variable

        decryptedIdx[0] = fullMessage[0];
        for (int i = 1; i < 33; i++) {
            decryptedTag[i - 1] = fullMessage[i];
        }

        for (int i = 33; i < fullMessage.length; i++) {
            decryptedMessage.add(fullMessage[i]);
        }

        this.receiverIdx = decryptedIdx;
        this.receiverTag = decryptedTag;
        this.receiverSecretKey = keyDeriviationFunction(this.receiverSecretKey);
        byte[] genericByteArr = new byte[decryptedMessage.size()];
        for (int i = 0; i < decryptedMessage.size(); i++) {
            genericByteArr[i] = decryptedMessage.get(i);
        }
        String messageResult = new String(genericByteArr);
        messageArea.append(String.format("%s%n", messageResult));
        return messageResult;
    }

    @Override
    public void run() {
        while (true) {
            if (receiverIdx != null && receiverTag != null && receiverSecretKey != null) {
                try {
                    receive();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private SecretKey keyDeriviationFunction(SecretKey key) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] hmacSha256;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), "HmacSHA256");
        mac.init(secretKeySpec);
        hmacSha256 = mac.doFinal(key.getEncoded());
        return new SecretKeySpec(hmacSha256, 0, hmacSha256.length, "AES");
    }
}
