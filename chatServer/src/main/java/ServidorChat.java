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

public class ServidorChat extends WebSocketServer {

    static final int PUERTO = 12345;

    final PublishSubject<String> canal = PublishSubject.create();
    final List<Disposable> subs = Collections.synchronizedList(new ArrayList<>());

    // Map -> Guarda informacion en pares
    //"David"   -> ManejadorCliente@1
    //"Juan"    -> ManejadorCliente@2
    // "Ana"     -> ManejadorCliente@3
    final Map<String, ManejadorCliente> clientes = new ConcurrentHashMap<>();
    // Se implementa con ConcurrentHashMap para evitar problemas cuando varios clientes se conectan o se
    // desconectan al mismo tiempo

    // InetSocketAddress equivalente al Server Socket, inicializa el servidor con una direccion ip y un puerto
    public ServidorChat() {
        super(new InetSocketAddress(PUERTO));
    }

    // Cuando un Cliente se conecta
    // conn -> conexion con cliente en especifico, send reemplaza a dos.writeUTF
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send("SOLICITAR_NOMBRE"); // pide el nombre al cliente que entro
    }

    // Cuando llega un mensaje
    @Override
    public void onMessage(WebSocket conn, String mensaje) {

        // Obtiene el manejador asociado al cliente, tiene las funciones de enviar los mensajes
        ManejadorCliente mc = conn.getAttachment();

        // Situacion 1: el cliente aun no tiene nombre
        if (mc == null) {
            String nombre = mensaje.trim().isEmpty() ? "Anonimo" : mensaje.trim();

            // Si el nombre ya existe, le agrega un numero al final para evitar duplicados
            if (clientes.containsKey(nombre))
                nombre = nombre + "_" + (clientes.size() + 1);

            ManejadorCliente nuevoMc = new ManejadorCliente(conn, canal, subs, nombre, clientes);
            conn.setAttachment(nuevoMc); // Asocia la conexion con el cliente
            clientes.put(nombre, nuevoMc);
            nuevoMc.conectar(); // Suscribe al cliente para que le lleguen los mensajes

            // Avisa a todos la lista actualizada de usuarios
            broadcast("USUARIOS:" + String.join(",", clientes.keySet()));
            return;
        }

       // MENSAJE PRIVADO
        if (mensaje.startsWith("PRIVADO:")) {
            String[] p = mensaje.split(":", 3);
            if (p.length == 3)
                mc.enviarPrivado(p[1], p[2]);
            return;
        }

        // MENSAJE GENERAL
        if (mensaje.equalsIgnoreCase("/salir")) {
            // Si el cliente EXPLICITAMENTE escribe /salir, se elimina del map y de la lista de suscripciones
            mc.desconectar();
            clientes.remove(mc.getNombre());
            broadcast("USUARIOS:" + String.join(",", clientes.keySet()));
            conn.close();
        } else {
            mc.recibirMensaje(mensaje);
        }
    }

    // Si el cliente se desconecta sin usar /salir
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
        new ServidorChat().start();
    }
}