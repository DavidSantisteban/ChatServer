import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.awt.BorderLayout;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class ServidorChat extends JFrame {

    static final int PUERTO = 12345;

    final PublishSubject<String> canal = PublishSubject.create();

    final List<Disposable> subs = Collections.synchronizedList(
            new ArrayList<>()
    );

    JTextArea pantalla;

    public ServidorChat() {
        getContentPane().setLayout(new BorderLayout());
        pantalla = new JTextArea("Bitacora:\n");
        pantalla.setEditable(false);
        getContentPane().add(new JScrollPane(pantalla), BorderLayout.CENTER);
        setSize(500, 600);
        setTitle("Servidor Chat");
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        new Thread(
                () -> {
                    try (ServerSocket servidor = new ServerSocket(PUERTO)) {
                        SwingUtilities.invokeLater(() ->
                                pantalla.append("Escuchando puerto " + PUERTO + "\n")
                        );

                        // Espera constantemente a que se registre algun cliente
                        while (true) {
                            Socket socket = servidor.accept(); // espera un cliente
                            ManejadorCliente mc = new ManejadorCliente(
                                    socket,
                                    pantalla,
                                    canal,
                                    subs
                            );
                            new Thread(mc).start(); // cada cliente en su propio hilo, se le comparte el socket, la pantalla del chat, el canal de comunicaicon
                            // comun y la lista de suscriptores
                        }
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() ->
                                pantalla.append("Error: " + e.getMessage() + "\n")
                        );
                    }
                },
                "hilo-servidor"
        )
                .start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServidorChat::new);
    }
}