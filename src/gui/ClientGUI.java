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
    JButton newClientButton = new JButton("Add new client");
    JTextField idField = new JTextField(5);
    JTextField tagField = new JTextField(5);
    JTextField keyField = new JTextField(5);

    //Encryption
    private final String CIPHER_INSTANCE = "AES/ECB/PKCS5Padding";

    //implementation with 1 receiver client
    private byte[] myTag;
    private byte[] myIdx;
    private SecretKey mySecretKey;

    private volatile byte[] receiverTag = null;
    private volatile byte[] receiverIdx = null;
    private volatile SecretKey receiverSecretKey = null;

    private ClientThread clientThread;

    public ClientGUI() {

        try {
            initializeEncryption();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Code for starting screen
        //TODO: borderlayout:   north "Conncetions"
        //                      south button newClientButton
        //                      center de effectieve buttons met clients
        JLabel conncetions = new JLabel("Connections");
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(newClientButton, BorderLayout.EAST);
        southPanel.add(textField, BorderLayout.WEST);
        JButton test1 = new JButton("Test 1");
        JButton test2 = new JButton("Test 2");
        JButton test3 = new JButton("Test 3");
        JButton test4 = new JButton("Test 4");
        buttonPanel.add(test1);
        buttonPanel.add(test2);
        buttonPanel.add(test3);
        buttonPanel.add(test4);

        textField.setEditable(false);
        messageArea.setEditable(false);


        frame.getContentPane().add(conncetions, BorderLayout.NORTH);
        frame.getContentPane().add(buttonPanel, BorderLayout.CENTER);
        frame.getContentPane().add(southPanel, BorderLayout.SOUTH);
        frame.setSize(750,390);
        frame.setLocationRelativeTo(null);

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
            } catch (RemoteException | NoSuchAlgorithmException | InvalidKeyException ex) {
                ex.printStackTrace();
            }
            textField.setText("");
        });

        // Action when button pressed
        newClientButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(null, myPanel,
                    "Please enter the ID, Tag and Key", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                receiverIdx = convertStringToByteArr(idField.getText());
                receiverTag = convertStringToByteArr(tagField.getText());
                receiverSecretKey = new SecretKeySpec(convertStringToByteArr(keyField.getText()), 0, convertStringToByteArr(keyField.getText()).length, "AES");
                //receiverName = nameField.getText();

                this.clientThread.setReceiverIdx(receiverIdx);
                this.clientThread.setReceiverTag(receiverTag);
                this.clientThread.setReceiverSecretKey(receiverSecretKey);
                //this.clientThread.setReceiverName(receiverName);

                //TODO: Add a new button for the new client
                JButton newClient = new JButton("Test 5");
                //newClient.addActionListener();
                buttonPanel.add(newClient);
                frame.getContentPane().add(buttonPanel, BorderLayout.CENTER);
                SwingUtilities.updateComponentTreeUI(frame);
            }
        });
    }

    private byte[] convertStringToByteArr(String stringByteArr) {
        String[] bytesString = stringByteArr.split(", ");
        byte[] bytes = new byte[bytesString.length];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = Byte.parseByte(bytesString[i]);
        }
        return bytes;
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
        System.out.println(randomIndex);


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

            clientThread = new ClientThread(bulletinBoard, receiverIdx, receiverTag, receiverSecretKey, messageArea, CIPHER_INSTANCE);
            clientThread.start();

        } finally {
            //frame.setVisible(false);
            //frame.dispose();
        }
    }

    public void send(String messageContent) throws RemoteException, NoSuchAlgorithmException, InvalidKeyException {
        //tag and idx for next message created inside message, client doesn't need to generate new ones
        Message message = new Message(messageContent, mySecretKey, CIPHER_INSTANCE);
        bulletinBoard.write(myIdx, message.getEncryptedMessage(), myTag);
        this.myTag = message.getTag();
        this.myIdx = message.getIdx();

        messageArea.append(String.format("%s: %s%n", this.name, messageContent));
        this.mySecretKey = keyDeriviationFunction(this.mySecretKey);
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