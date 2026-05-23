import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class ClienteChat extends JFrame {

    static final String HOST = "localhost";
    static final int PUERTO = 12345;

    DataInputStream dis;
    DataOutputStream dos;
    String nombre;

    JTextArea pantalla;
    JTextField campoTexto;

    public ClienteChat() {
        nombre = JOptionPane.showInputDialog(
                null,
                "Ingresa tu nombre:",
                "Chat",
                JOptionPane.PLAIN_MESSAGE
        );
        if (nombre == null || nombre.trim().isEmpty()) nombre = "Anonimo";

        getContentPane().setLayout(new BorderLayout());

        JLabel etiquetaNombre = new JLabel(
                "  Conectado como: " + nombre,
                SwingConstants.LEFT
        );
        etiquetaNombre.setFont(new Font("Arial", Font.BOLD, 14));
        etiquetaNombre.setOpaque(true);
        etiquetaNombre.setBackground(new Color(60, 120, 200));
        etiquetaNombre.setForeground(Color.WHITE);
        etiquetaNombre.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        getContentPane().add(etiquetaNombre, BorderLayout.NORTH);

        pantalla = new JTextArea();
        pantalla.setEditable(false);
        pantalla.setLineWrap(true);
        getContentPane().add(new JScrollPane(pantalla), BorderLayout.CENTER);

        campoTexto = new JTextField();
        campoTexto.setEnabled(false);
        campoTexto.addActionListener(e -> enviar());
        getContentPane().add(campoTexto, BorderLayout.SOUTH);

        setSize(450, 500);
        setTitle("Chat - " + nombre);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        new Thread(
                () -> {
                    try {
                        Socket socket = new Socket(HOST, PUERTO);
                        dis = new DataInputStream(socket.getInputStream());
                        dos = new DataOutputStream(socket.getOutputStream());

                        // Verifica si el mensaje del servidor es "SOLICITAR_NOMBRE", si es asi escribir nombre
                        if ("SOLICITAR_NOMBRE".equals(dis.readUTF())) {
                            dos.writeUTF(nombre);
                        }

                        // Habilita el campo del texto al cliente
                        SwingUtilities.invokeLater(() -> {
                            campoTexto.setEnabled(true);
                            campoTexto.requestFocus();
                        });

                        // Lee el msg que envia el ManejadorCliente.java que es enviado a todos lo clientes
                        while (true) {
                            String msg = dis.readUTF();
                            SwingUtilities.invokeLater(() ->
                                    pantalla.append(msg + "\n")
                            );
                        }
                        // Catch por si el servidor no se ha iniciado y el cliente ya netro
                    } catch (ConnectException e) {
                        SwingUtilities.invokeLater(() -> {
                            pantalla.append("El servidor no esta disponible.\n");
                            campoTexto.setEnabled(false);
                        });
                        // Catch cuando el servidor se cierre por accidente
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() ->
                                pantalla.append("-- Desconectado --\n")
                        );
                    }
                },
                "hilo-cliente"
        )
                .start();
    }

    // Cliente envia mensaje al Servidor que es Dirigido al Socket de cada Cliente
    void enviar() {
        String texto = campoTexto.getText().trim();
        if (texto.isEmpty()) return;
        try {
            dos.writeUTF(texto);
            campoTexto.setText("");
        } catch (IOException e) {
            SwingUtilities.invokeLater(() ->
                    pantalla.append("Error al enviar.\n")
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClienteChat::new);
    }
}