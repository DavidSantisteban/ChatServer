

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class ManejadorCliente implements Runnable {

    private final Socket socket;
    private final JTextArea pantalla;
    private final PublishSubject<String> canal;
    private final List<Disposable> subs;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String nombre = "Anonimo";
    private Disposable subscription;

    public ManejadorCliente(
            Socket socket,
            JTextArea pantalla,
            PublishSubject<String> canal, // <- Actua como Observer y Observable
            List<Disposable> subs
    ) {
        this.socket = socket;
        this.pantalla = pantalla;
        this.canal = canal;
        this.subs = subs;
    }

    @Override
    public void run() {
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            dos.writeUTF("SOLICITAR_NOMBRE");
            nombre = dis.readUTF().trim();
            if (nombre.isEmpty()) nombre = "Anonimo";

            agregarInterfaz(nombre + " se conecto.\n");

            // Canal Actua como Observer, porque esta recibiendo datos
            // Observer: el que recibe y procesa.

            canal.onNext(">> " + nombre + " entro al chat"); // canal.onNext() -> Observer

            // Canal Actua como Observable ya que cada vez que se emite un mensaje cada vez que
            // se haga un enviar(msg) a los clientes que estan suscritos
            subscription = canal.subscribe(msg -> enviar(msg), err -> {});

            subs.add(subscription);

            // Aca lee los mensajes de los clientes y los envia
            while (true) {
                // METIENDO DATOS AL CANAL
                // Canal sigue siendo Observer, Emite el mensaje al canal

                //dis.readUTF(); Lee el mensaje enviado desde ClienteChat.java y lo envia a los canales
                String msg = dis.readUTF();
                if (msg.equalsIgnoreCase("/salir")) break;

                canal.onNext(nombre + ": " + msg);
                agregarInterfaz(nombre + ": " + msg + "\n");
            }
        } catch (IOException e) {
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        canal.onNext(">> " + nombre + " salio del chat");
        agregarInterfaz(nombre + " se desconecto.\n");

        // Cancelar suscripcion y limpiar de la lista del servidor
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            subs.remove(subscription);
        }

        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    void enviar(String mensaje) {
        try {
            if (dos != null) dos.writeUTF(mensaje);
        } catch (IOException ignored) {}
    }

    private void agregarInterfaz(String text) {
        SwingUtilities.invokeLater(() -> pantalla.append(text));
    }
}