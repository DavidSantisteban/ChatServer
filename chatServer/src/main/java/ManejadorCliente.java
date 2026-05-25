import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.util.List;
import java.util.Map;
import org.java_websocket.WebSocket;

// CAMBIO: ya no implementa Runnable, no hay hilos manuales ni DataInputStream/DataOutputStream
// CAMBIO: se elimino JTextArea y SwingUtilities, el servidor imprime en consola directamente
// CAMBIO: se agrego enviarPrivado() para mensajes privados
// CAMBIO: se agrego el mapa clientes para buscar usuarios por nombre

public class ManejadorCliente {

    // La conexión directa con el cliente que representa este manejador
    // Reemplaza al Socket,  DataouputStream, Datainputstream, se leen mensajes con onMessage y se ecriben con conn.send();
    private final WebSocket conn;

    private final PublishSubject<String> canal; // Canal donde se suscriben los clientes, Observer y Observable1
    private final List<Disposable> subs;
    private final String nombre;
    private final Map<String, ManejadorCliente> clientes; // Guarda el nombre del cliente y su respectivo manejador
    private Disposable subscription;

    public ManejadorCliente(WebSocket conn, PublishSubject<String> canal, List<Disposable> subs, String nombre, Map<String, ManejadorCliente> clientes) {
        this.conn     = conn;
        this.canal    = canal;
        this.subs     = subs;
        this.nombre   = nombre;
        this.clientes = clientes;
    }

    // Suscribirse al canal
    public void conectar() {
        canal.onNext(">> " + nombre + " entro al chat");  // Mete un dato al canal, el canal en este caso es Observer, lo distribuye a los demas clientes
        subscription = canal.subscribe(this::enviar, err -> {}); // Cliente se suscrible, el canal en este caso es Observable
        subs.add(subscription);
    }

    // Mete el mensaje al canal, todos los suscritos lo reciben
    public void recibirMensaje(String msg) {
        canal.onNext(nombre + ": " + msg);
        // desaparece: agregarInterfaz(nombre + ": " + msg + "\n");
    }

    // NUEVO: busca al destinatario en el mapa y le envia el mensaje directamente
    public void enviarPrivado(String destinatario, String contenido) {
        ManejadorCliente dest = clientes.get(destinatario); // -> devuelve el manejador
        if (dest != null) {
            dest.enviar("[Privado] " + nombre + ": " + contenido);
            enviar("[Privado a " + destinatario + "] " + contenido); // confirmacion al remitente
        } else {
            enviar(">> Usuario '" + destinatario + "' no encontrado.");
        }
    }

    public void desconectar() {
        canal.onNext(">> " + nombre + " salio del chat");
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            subs.remove(subscription);
        }
    }

    public String getNombre() { return nombre; }

    public void enviar(String mensaje) {
        if (conn.isOpen()) conn.send(mensaje);
    }
}