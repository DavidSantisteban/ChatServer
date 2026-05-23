import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

// CAMBIO: ya no extiende JFrame, ahora extiende WebSocketServer
// CAMBIO: se elimino todo lo de Swing (JTextArea, pantalla, SwingUtilities)
// CAMBIO: se agrego el mapa 'clientes' para mensajes privados y lista de usuarios
public class ServidorChat extends WebSocketServer {

    static final int PUERTO = 12345;

    final PublishSubject<String> canal = PublishSubject.create();
    final List<Disposable> subs = Collections.synchronizedList(new ArrayList<>());

    // NUEVO: mapa nombre -> manejador, para buscar a quien mandar un privado
    final Map<String, ManejadorCliente> clientes = new ConcurrentHashMap<>();

    public ServidorChat() {
        super(new InetSocketAddress(PUERTO));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send("SOLICITAR_NOMBRE"); // pide el nombre al cliente que entro
    }

    @Override
    public void onMessage(WebSocket conn, String mensaje) {
        ManejadorCliente mc = conn.getAttachment();

        // Primer mensaje que llega = el nombre del cliente
        if (mc == null) {
            String nombre = mensaje.trim().isEmpty() ? "Anonimo" : mensaje.trim();
            if (clientes.containsKey(nombre)) nombre = nombre + "_" + (clientes.size() + 1);

            ManejadorCliente nuevoMc = new ManejadorCliente(conn, canal, subs, nombre, clientes);
            conn.setAttachment(nuevoMc);
            clientes.put(nombre, nuevoMc);
            nuevoMc.conectar();

            // Avisa a todos la lista actualizada de usuarios
            broadcast("USUARIOS:" + String.join(",", clientes.keySet()));
            return;
        }

        // NUEVO: mensaje privado con formato "PRIVADO:destinatario:contenido"
        if (mensaje.startsWith("PRIVADO:")) {
            String[] p = mensaje.split(":", 3);
            if (p.length == 3) mc.enviarPrivado(p[1], p[2]);
            return;
        }

        // Mensaje normal al canal general
        if (mensaje.equalsIgnoreCase("/salir")) {
            mc.desconectar();
            clientes.remove(mc.getNombre());
            broadcast("USUARIOS:" + String.join(",", clientes.keySet()));
            conn.close();
        } else {
            mc.recibirMensaje(mensaje);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ManejadorCliente mc = conn.getAttachment();
        if (mc != null) {
            mc.desconectar();
            clientes.remove(mc.getNombre());
            broadcast("USUARIOS:" + String.join(",", clientes.keySet()));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("Error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Servidor escuchando en puerto " + PUERTO);
    }

    public static void main(String[] args) throws Exception {
        // CAMBIO: ya no usa SwingUtilities, simplemente arranca el servidor
        new ServidorChat().start();
    }
}