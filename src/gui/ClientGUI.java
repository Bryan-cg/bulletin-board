package gui;

import shared.Chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Set;

public class ClientGUI {

    Chat impl;
    String name;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16, 50);
    JTextArea onlineClients = new JTextArea(40, 16);

    //Encryption
    private final String CIPHER_INSTANCE = "AES/ECB/PKCS5Padding";
    private SecretKey secretKey;
    private byte[] tag;
    private byte[] idx;

    public ClientGUI() {

        textField.setEditable(false);
        onlineClients.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.getContentPane().add(new JScrollPane(onlineClients), BorderLayout.WEST);
        frame.pack();

        // Send on enter then clear to prepare for next message
        textField.addActionListener(e -> {
            try {
                String receiver = textField.getText().split(" ")[0];
                impl.sendMessagePrivate(name, textField.getText().substring(receiver.length()), receiver);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
            textField.setText("");
        });

        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                boolean cont = false;
                while (!cont) {
                    try {
                        if (impl.unRegister(name)) {
                            cont = true;
                        }
                    } catch (RemoteException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
    }

    private void initializeEncryption() throws NoSuchAlgorithmException {
        //Symmetric key
        this.secretKey = generateKeyAES(256);

        //tag
        byte[] tagBytes = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(tagBytes);
        this.tag = tagBytes;

        //idx
        final Random r = new Random();
        int randomIndex = r.nextInt(20);
        this.idx = BigInteger.valueOf(randomIndex).toByteArray();
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

    private void run() throws IOException, NotBoundException {
        try {

            Registry myRegistry = LocateRegistry.getRegistry("127.0.0.1", 1099);
            impl = (Chat) myRegistry.lookup("ChatService");

            boolean hasName = false;
            while (!hasName) {
                name = getName();
                if (name == null) {
                    return;
                }
                hasName = impl.register(name);
                frame.setTitle(name);
            }

            textField.setEditable(true);
            Set<String> names = impl.getUsers();
            onlineClients.setText("");
            for (String name : names) {
                onlineClients.append(name + "\n");
            }

            while (true) {
                String message = impl.getMessage(name);
                messageArea.append(message + "\n");
                names = impl.getUsers();
                onlineClients.setText("");
                for (String name : names) {
                    onlineClients.append(name + "\n");
                }
            }

        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }
}