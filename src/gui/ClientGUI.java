package gui;

import shared.Chat;
import models.Message;
import javax.crypto.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

//TODO's (other)
// Show tag, idx & secretkey in gui, so you can copy paste it in other client.

public class ClientGUI {

    Chat bulletinBoard;
    String name;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16, 50);
    JButton newReceiverButton = new JButton("Add new receiver");
    JPanel southPanel = new JPanel(new BorderLayout());
    JPanel northPanel = new JPanel(new BorderLayout());
    JTextField idField = new JTextField(5); //geen idee wat die 5 doet eigenlijk
    JTextField tagField = new JTextField(5);
    JTextField keyField = new JTextField(5);

    //Encryption
    private final String CIPHER_INSTANCE = "AES/ECB/PKCS5Padding";

    //implementation with 1 receiver client
    private byte[] myTag;
    private byte[] myIdx;
    private SecretKey mySecretKey;

    private byte[] receiverTag;
    private byte[] receiverIdx;
    private SecretKey receiverSecretKey;

    public ClientGUI() {

        try {
            initializeEncryption();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Code for starting screen
        textField.setEditable(false);
        messageArea.setEditable(false);
        southPanel.add(newReceiverButton, BorderLayout.EAST);
        southPanel.add(textField, BorderLayout.WEST);

        // Panels for ID, Tag and Key
        JPanel panel = new JPanel(new GridLayout(3, 2, 0, 0));
        panel.add(new Label("ID: "));
        panel.add(new JTextField(Arrays.toString(myIdx)));
        panel.add(new Label("Tag: "));
        panel.add(new JTextField(Arrays.toString(myTag)));
        panel.add(new Label("Key: "));
        panel.add(new JTextField(Arrays.toString(mySecretKey.getEncoded())));

        frame.getContentPane().add(panel,BorderLayout.PAGE_START);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.getContentPane().add(southPanel,BorderLayout.SOUTH);
        frame.pack();

        // Code for popup screen: new receiver
        JPanel myPanel = new JPanel();
        myPanel.add(new JLabel("ID: "));
        myPanel.add(idField);
        myPanel.add(new JLabel("Tag: "));
        myPanel.add(tagField);
        myPanel.add(new JLabel("Key: "));
        myPanel.add(keyField);

        // On enter
        textField.addActionListener(e -> {
            try {
                send(textField.getText());
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
            textField.setText("");
        });

        // Action when button pressed
        newReceiverButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(null, myPanel,
                    "Please enter the ID, Tag and Key", JOptionPane.OK_CANCEL_OPTION);
            //TODO: voorlopig enkel uitprinten, later in een lijst steken ofso?
            if (result == JOptionPane.OK_OPTION) {
                System.out.println("ID: " + idField.getText());
                System.out.println("Tag: " + tagField.getText());
                System.out.println("Key: " + keyField.getText());
            }
        });
    }

    private void initializeEncryption() throws NoSuchAlgorithmException {
        //Symmetric key
        this.mySecretKey = generateKeyAES(256);

        //tag
        byte[] tagBytes = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(tagBytes);
        this.myTag = tagBytes;

        //idx
        final Random r = new Random();
        int randomIndex = r.nextInt(20);
        this.myIdx = BigInteger.valueOf(randomIndex).toByteArray();

        // Debugging: printing ID, Tag and Key
        System.out.println("This ID: " + Arrays.toString(myIdx));
        System.out.println("This Tag: " + Arrays.toString(myTag));
        System.out.println("This Key: " + Arrays.toString(mySecretKey.getEncoded()));
    }

    public static void main(String[] args) throws Exception {
        ClientGUI client = new ClientGUI();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }

    private String getName() {
        return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    public static SecretKey generateKeyAES(int n) {
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyGenerator.init(n);
        return keyGenerator.generateKey();
    }

    private void run() throws IOException, NotBoundException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        try {

            Registry myRegistry = LocateRegistry.getRegistry("127.0.0.1", 1099);
            bulletinBoard = (Chat) myRegistry.lookup("BulletinBoard");

            boolean hasName = false;
            while (!hasName) {
                name = getName();
                if (name == null) return;
                hasName = true;
                frame.setTitle(name);
            }

            textField.setEditable(true);

            while (true) {
                //TODO get message by tag and idx
                //receive()

                //TODO maybe only poll for messages when button is pressed?
            }

        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    public void send(String messageContent) throws RemoteException {

        //tag and idx for next message created inside message, client doesn't need to generate new ones
        Message message = new Message(messageContent, mySecretKey, CIPHER_INSTANCE);

        bulletinBoard.write(myIdx, message.getEncryptedMessage(), myTag);
        this.myTag = message.getTag();
        this.myIdx = message.getIdx();

        //TODO: generate new mySecretkey --> key derivation function

    }



    public String receive() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte [] totalMessage = bulletinBoard.get(receiverIdx, receiverTag);
        String message;

        if(totalMessage!= null){
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
        String fullMessage = Arrays.toString(cipher.doFinal(totalMessage));

        return spitTotalMessage(fullMessage);
    }

    //Hoe hieruit nieuwe tag en id en effectieve message achterhalen?
    //Zal fullMessage er zo uitzien 11tekst5?
    private String spitTotalMessage(String fullMessage) {
        int indexID = -1;
        int indexTag = -1;

        for(int i=0; i<fullMessage.length();i++) {
            if(!Character.isDigit(fullMessage.charAt(i))) {
                indexID = i-1;
                break;
            }
        }
        for(int i=fullMessage.length()-1; i>-1;i--) {
            if(!Character.isDigit(fullMessage.charAt(i))) {
                indexTag = i+1;
                break;
            }
        }

        this.receiverIdx = fullMessage.substring(0,indexID).getBytes(StandardCharsets.UTF_8);
        this.receiverTag = fullMessage.substring(indexTag).getBytes(StandardCharsets.UTF_8);
        return "message";
    }

    //TODO: after receive generate new receiverSecretKey --> key derivation function
    //https://sorenpoulsen.com/calculate-hmac-sha256-with-java
    //https://stackoverflow.com/questions/14204437/convert-byte-array-to-secret-key
    private void keyDeriviationFunction(SecretKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] hmacSha256;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), "HmacSHA256");
        mac.init(secretKeySpec);
        hmacSha256 = mac.doFinal(key.getEncoded());
        SecretKey newKey = new SecretKeySpec(hmacSha256, 0, hmacSha256.length, "AES");
        System.out.println(newKey);
    }
}