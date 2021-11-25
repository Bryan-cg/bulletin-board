package gui;

import models.ClientProperties;
import net.miginfocom.swing.MigLayout;
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
import java.util.*;

//TODO's (other)
// Show tag, idx & secretkey in gui, so you can copy paste it in other client.

public class ClientGUI {

    // General attributes
    Chat bulletinBoard;
    String name;

    // Attributes for frame
    JFrame frame = new JFrame("Chatter");
    JPanel buttonPanel;
    JScrollPane scrollPane;
    JTextArea messageArea;
    JTextField textField;
    JButton addNewClientButton;

    // Attributes for popup screen
    JPanel myPanel = new JPanel(new GridLayout(4, 1)); // 2 rows x 1 column
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

        // Initializing the JavaSwing components
        initComponents();

        // On enter
        textField.addActionListener(e -> {
            try {
                send(textField.getText());
            } catch (RemoteException | NoSuchAlgorithmException | InvalidKeyException ex) {
                ex.printStackTrace();
            }
            textField.setText("");
        });


        // Action when addNewClientButton pressed
        addNewClientButton.addActionListener(e -> {

            // Create new ID, Tag and Key for the new client
            ClientProperties newProperties = null;
            try {
                newProperties= initializeMyPropertiesNewClient();
            } catch (NoSuchAlgorithmException ex) {
                ex.printStackTrace();
            }

            JPanel bottomRow1 = new JPanel();
            bottomRow1.add(new JLabel("ID: "));
            bottomRow1.add(new JLabel(Arrays.toString(newProperties.getIdx())));
            JPanel bottomRow2 = new JPanel();
            bottomRow2.add(new JLabel("Tag: "));
            bottomRow2.add(new JLabel(Arrays.toString(newProperties.getTag())));
            JPanel bottomRow3 = new JPanel();
            bottomRow3.add(new JLabel("Key: "));
            bottomRow3.add(new JLabel(Arrays.toString(newProperties.getSecretKey().getEncoded())));
            myPanel.add(bottomRow1);
            myPanel.add(bottomRow2);
            myPanel.add(bottomRow3);

            int result = JOptionPane.showConfirmDialog(null, myPanel,
                    "Please enter the ID, Tag and Key", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                byte[] receiverIdx = convertStringToByteArr(idField.getText());
                byte[] receiverTag = convertStringToByteArr(tagField.getText());
                SecretKey receiverSecretKey = new SecretKeySpec(convertStringToByteArr(keyField.getText()), 0, convertStringToByteArr(keyField.getText()).length, "AES");
                String receiverName = nameField.getText();
                ClientProperties clientProperties = new ClientProperties(receiverTag, receiverIdx, receiverSecretKey);
                receiversProperties.put(receiverName, clientProperties);

                // Adding the properties tot the right map
                myProperties.put(receiverName,clientProperties);

                //this.clientThread.setReceiversProperties(receiversProperties);

                //TODO: Add a new button for the new client
                JButton newClient = new JButton(receiverName);
                newClient.addActionListener(event -> {
                    //messageArea.clearofso
                });
                buttonPanel.add(newClient);
                SwingUtilities.updateComponentTreeUI(frame);
                System.out.println(myProperties);
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

    private ClientProperties initializeMyPropertiesNewClient() throws NoSuchAlgorithmException {
        //Symmetric key
        SecretKey mySecretKey = generateKeyAES(256);

        //tag
        byte[] tagBytes = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(tagBytes);

        //idx
        final Random r = new Random();
        int randomIndex = r.nextInt(20);
        byte[] myIdx = BigInteger.valueOf(randomIndex).toByteArray();

        return new ClientProperties(tagBytes, myIdx, mySecretKey);
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

            clientThread = new ClientThread(bulletinBoard, myProperties, receiversProperties, messageArea, CIPHER_INSTANCE);
            clientThread.start();

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

    // Aanmaken Frame (vanblijven!!)
    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        buttonPanel = new JPanel();
        scrollPane = new JScrollPane();
        messageArea = new JTextArea();
        textField = new JTextField();
        addNewClientButton = new JButton();

        // Code for popup screen: new receiver
        JPanel topRow = new JPanel();
        topRow.add(new JLabel("ID: "));
        topRow.add(idField);
        topRow.add(new JLabel("Tag: "));
        topRow.add(tagField);
        topRow.add(new JLabel("Key: "));
        topRow.add(keyField);
        topRow.add(new JLabel("Name: "));
        topRow.add(nameField);
        
        myPanel.add(topRow);

        //======== this ========
        frame.setTitle("Chatter");
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]" +
                        "[fill]",
                // rows
                "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]" +
                        "[]"));

        //======== panel1 ========
        {
            buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));
        }
        contentPane.add(buttonPanel, "cell 0 0 55 4,grow");

        //======== scrollPane1 ========
        {
            scrollPane.setViewportView(messageArea);
        }
        contentPane.add(scrollPane, "cell 0 4 55 36,grow");
        contentPane.add(textField, "cell 0 40 51 1,growx");

        //---- button1 ----
        addNewClientButton.setText("Add new client");
        contentPane.add(addNewClientButton, "cell 51 40 4 1");
        frame.pack();
        frame.setLocationRelativeTo(null);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
}

