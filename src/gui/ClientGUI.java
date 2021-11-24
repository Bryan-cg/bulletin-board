package gui;

import models.ClientProperties;
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
import java.util.*;
import java.util.List;

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
    JTextField nameField = new JTextField(5);

    //Encryption
    private final String CIPHER_INSTANCE = "AES/ECB/PKCS5Padding";

    private volatile HashMap<String, ClientProperties> myProperties = new HashMap<>();
    private volatile HashMap<String, ClientProperties> receiversProperties = new HashMap<>();
    private String currentClientName;

    private ClientThread clientThread;

    public ClientGUI() {

        // Code for starting screen
        //TODO: borderlayout:   north "Conncetions"
        //                      south button newClientButton
        //                      center de effectieve buttons met clients
        JLabel conncetions = new JLabel("Connections");
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(newClientButton, BorderLayout.EAST);
        southPanel.add(textField, BorderLayout.WEST);

        textField.setEditable(false);
        messageArea.setEditable(false);


        frame.getContentPane().add(conncetions, BorderLayout.NORTH);
        frame.getContentPane().add(buttonPanel, BorderLayout.CENTER);
        frame.getContentPane().add(southPanel, BorderLayout.SOUTH);
        frame.setSize(625,390);
        frame.setLocationRelativeTo(null);

        // Code for popup screen: new receiver
        JPanel myPanel = new JPanel();
        myPanel.add(new JLabel("ID: "));
        myPanel.add(idField);
        myPanel.add(new JLabel("Tag: "));
        myPanel.add(tagField);
        myPanel.add(new JLabel("Key: "));
        myPanel.add(keyField);
        myPanel.add(new JLabel("Name: "));
        myPanel.add(nameField);

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

                byte[] receiverIdx = convertStringToByteArr(idField.getText());
                byte[] receiverTag = convertStringToByteArr(tagField.getText());
                SecretKey receiverSecretKey = new SecretKeySpec(convertStringToByteArr(keyField.getText()), 0, convertStringToByteArr(keyField.getText()).length, "AES");
                String receiverName = nameField.getText();
                ClientProperties clientProperties = new ClientProperties(receiverTag, receiverIdx, receiverSecretKey);
                receiversProperties.put("test", clientProperties);

                //this.clientThread.setReceiversProperties(receiversProperties);

                //TODO: Add a new button for the new client
                JButton newClient = new JButton(receiverName);
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

    private void initializeMyPropertiesNewClient(String clientName) throws NoSuchAlgorithmException {
        //Symmetric key
        SecretKey mySecretKey = generateKeyAES(256);

        //tag
        byte[] tagBytes = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(tagBytes);

        //idx
        final Random r = new Random();
        int randomIndex = r.nextInt(20);
        byte[] myIdx = BigInteger.valueOf(randomIndex).toByteArray();

        ClientProperties myClientProperties = new ClientProperties(tagBytes, myIdx, mySecretKey);
        myProperties.put(clientName, myClientProperties);
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

    private void run() throws IOException, NotBoundException {
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

            //clientThread = new ClientThread(bulletinBoard, myProperties, receiversProperties, messageArea, CIPHER_INSTANCE);
            //clientThread.start();

        } finally {
            //frame.setVisible(false);
            //frame.dispose();
        }
    }

    public void send(String messageContent) throws RemoteException, NoSuchAlgorithmException, InvalidKeyException {
        ClientProperties myClientProperties = myProperties.get(name);

        //tag and idx for next message created inside message, client doesn't need to generate new ones
        Message message = new Message(messageContent, myClientProperties.getSecretKey(), CIPHER_INSTANCE);
        bulletinBoard.write(myClientProperties.getIdx(), message.getEncryptedMessage(), myClientProperties.getTag());
        myClientProperties.setTag(message.getTag());
        myClientProperties.setIdx(message.getIdx());

        messageArea.append(String.format("%s: %s%n", this.name, messageContent));
        myClientProperties.setSecretKey(keyDeriviationFunction(myClientProperties.getSecretKey()));
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