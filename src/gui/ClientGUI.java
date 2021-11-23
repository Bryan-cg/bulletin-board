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
import java.util.ArrayList;
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

    private byte[] receiverTag = null;
    private byte[] receiverIdx = null;
    private SecretKey receiverSecretKey = null;

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
        JTextField textFieldId = new JTextField(Arrays.toString(myIdx));
        textFieldId.setEditable(false);
        panel.add(textFieldId);
        panel.add(new Label("Tag: "));
        JTextField textFieldTag = new JTextField(Arrays.toString(myTag));
        textFieldTag.setEditable(false);
        panel.add(textFieldTag);
        panel.add(new Label("Key: "));
        JTextField textFieldKey = new JTextField(Arrays.toString(mySecretKey.getEncoded()));
        textFieldKey.setEditable(false);
        panel.add(textFieldKey);

        frame.getContentPane().add(panel, BorderLayout.PAGE_START);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.getContentPane().add(southPanel, BorderLayout.SOUTH);
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
            } catch (RemoteException | NoSuchAlgorithmException ex) {
                ex.printStackTrace();
            }
            textField.setText("");
        });

        // Action when button pressed
        newReceiverButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(null, myPanel,
                    "Please enter the ID, Tag and Key", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                receiverIdx = convertStringToByteArr(idField.getText());
                receiverTag = convertStringToByteArr(tagField.getText());
                receiverSecretKey = new SecretKeySpec(convertStringToByteArr(keyField.getText()), 0, convertStringToByteArr(keyField.getText()).length, "AES");

                System.out.println("ID: " + Arrays.toString(receiverIdx));
                System.out.println("Tag: " + Arrays.toString(receiverTag));
                System.out.println("Key: " + Arrays.toString(receiverSecretKey.getEncoded()));

                /*
                try {
                    receive();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (NoSuchAlgorithmException ex) {
                    ex.printStackTrace();
                } catch (NoSuchPaddingException ex) {
                    ex.printStackTrace();
                } catch (IllegalBlockSizeException ex) {
                    ex.printStackTrace();
                } catch (BadPaddingException ex) {
                    ex.printStackTrace();
                } catch (InvalidKeyException ex) {
                    ex.printStackTrace();
                }

                 */

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
        System.out.println("Key length: " + mySecretKey.getEncoded().length);
        System.out.println("ID length: " + myIdx.length);
        System.out.println("Tag length: " + myTag.length);
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

            Thread thread = new Thread(new ClientThread(bulletinBoard, receiverIdx, receiverTag, receiverSecretKey, messageArea, CIPHER_INSTANCE));
            thread.start();

        } finally {
            //frame.setVisible(false);
            //frame.dispose();
        }
    }

    public void send(String messageContent) throws RemoteException, NoSuchAlgorithmException {
        //tag and idx for next message created inside message, client doesn't need to generate new ones
        Message message = new Message(messageContent, mySecretKey, CIPHER_INSTANCE);
        bulletinBoard.write(myIdx, message.getEncryptedMessage(), myTag);
        this.myTag = message.getTag();
        this.myIdx = message.getIdx();
        int test = new BigInteger(myIdx).intValue();

        messageArea.append(String.format("%s: %s%n", this.name, messageContent));
        //TODO: generate new mySecretkey --> key derivation function

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