package gui;

import models.ClientProperties;
import shared.Chat;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class ClientThread extends Thread {
    private final Chat bulletinBoard;
    private volatile HashMap<String, ClientProperties> myProperties = null;
    private volatile HashMap<String, ClientProperties> receiversProperties = null;
    private volatile HashMap<String, ArrayList<String>> previousMessages = null;
    private volatile String currentClientName = null;

    private final JTextArea messageArea;
    private final String CIPHER_INSTANCE;

    //Hashing
    private static final String SHA2_ALGORITHM = "SHA-256";

    public ClientThread(
            Chat bulletinBoard,
            HashMap<String, ClientProperties> myProperties,
            HashMap<String, ClientProperties> receiversProperties,
            JTextArea messageArea,
            String CIPHER_INSTANCE,
            HashMap<String, ArrayList<String>> previousMessages) {

        this.bulletinBoard = bulletinBoard;
        this.myProperties = myProperties;
        this.receiversProperties = receiversProperties;
        this.messageArea = messageArea;
        this.CIPHER_INSTANCE = CIPHER_INSTANCE;
        this.previousMessages = previousMessages;
    }

    public void setMyProperties(HashMap<String, ClientProperties> myProperties) {
        this.myProperties = myProperties;
    }

    public void setReceiversProperties(HashMap<String, ClientProperties> receiversProperties) {
        this.receiversProperties = receiversProperties;
    }

    public void setCurrentClientName(String currentClientName) {
        this.currentClientName = currentClientName;
    }

    public String receive() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        ClientProperties receiverClientProperties = receiversProperties.get(currentClientName);
        byte[] hashedTag = hashing(receiverClientProperties.getTag());
        byte[] totalMessage = bulletinBoard.get(receiverClientProperties.getIdx(), hashedTag);
        String message;
        if (totalMessage != null) {
            message = decryptTotalMessage(totalMessage);
            keyDeriviationFunction(receiverClientProperties.getSecretKey());
            previousMessages.get(currentClientName).add(currentClientName + ": " + message);
            return message;
        }
        return null;
    }

    //TODO update receiverIdx and receiverTag from message content and return the actual message
    private String decryptTotalMessage(byte[] totalMessage) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        ClientProperties clientProperties = receiversProperties.get(currentClientName);
        final Cipher cipher;
        cipher = Cipher.getInstance(CIPHER_INSTANCE);
        cipher.init(Cipher.DECRYPT_MODE, clientProperties.getSecretKey());
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

        ClientProperties receiverClientProperties = receiversProperties.get(currentClientName);
        receiverClientProperties.setIdx(decryptedIdx);
        receiverClientProperties.setTag(decryptedTag);
        receiverClientProperties.setSecretKey(keyDeriviationFunction(receiverClientProperties.getSecretKey()));

        byte[] genericByteArr = new byte[decryptedMessage.size()];
        for (int i = 0; i < decryptedMessage.size(); i++) {
            genericByteArr[i] = decryptedMessage.get(i);
        }
        String messageResult = new String(genericByteArr);
        messageArea.append(String.format("%s: %s%n", currentClientName ,messageResult));
        return messageResult;
    }

    @Override
    public void run() {
        while (true) {
            if (currentClientName != null) {
                try {
                    receive();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

    }

    private byte[] hashing(byte[] tag) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(SHA2_ALGORITHM);
        return messageDigest.digest(tag);

    }

    private SecretKey keyDeriviationFunction(SecretKey key) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] hmacSha256;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), "HmacSHA256");
        mac.init(secretKeySpec);
        hmacSha256 = mac.doFinal(key.getEncoded());
        return new SecretKeySpec(hmacSha256, 0, hmacSha256.length, "AES");
    }

    public HashMap<String, ArrayList<String>> getPreviousMessages() {
        return previousMessages;
    }

    public void setPreviousMessages(HashMap<String, ArrayList<String>> previousMessages) {
        this.previousMessages = previousMessages;
    }
}
