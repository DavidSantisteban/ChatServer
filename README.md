# Chat en Tiempo Real — Java WebSocket + React

Aplicación de chat local con servidor en Java y frontend en React. Soporta canal general y mensajes privados entre usuarios.

---

## Estructura del proyecto

```
/
├── ChatJava/               ← Proyecto IntelliJ (backend)
│   ├── ServidorChat.java
│   ├── ManejadorCliente.java
│   └── pom.xml
│
└── chat-frontend/          ← Proyecto React (frontend)
    ├── src/
    │   ├── App.jsx
    │   └── App.css
    └── package.json
```

---

## Requisitos

- Java 11 o superior
- Maven
- Node.js 18 o superior

---

## Dependencia Maven

Agregar en `pom.xml` dentro de `<dependencies>`:

```xml
<dependency>
    <groupId>io.reactivex.rxjava3</groupId>
    <artifactId>rxjava</artifactId>
    <version>3.1.8</version>
</dependency>

<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.4</version>
</dependency>
```

---

## Cómo correrlo

### 1. Iniciar el servidor

Abrir el proyecto en IntelliJ, click derecho en `ServidorChat.java` → Run.

En la consola de IntelliJ aparecerá:

```
Servidor WebSocket escuchando en puerto 12345
```

### 2. Iniciar el frontend

```bash
cd chat-frontend
npm install
npm run dev
```

Abrir el navegador en `http://localhost:5173`.

### Orden importante

Siempre iniciar el servidor **antes** de abrir el navegador. Si el servidor no está corriendo, el frontend muestra el aviso de servidor desconectado.

---

## Cómo funciona — Backend

### `ServidorChat.java`

Es el punto de entrada del servidor. Extiende `WebSocketServer` de la librería `Java-WebSocket`.

Mantiene dos estructuras compartidas entre todos los clientes:

- `canal` — un `PublishSubject` de RxJava que actúa como canal de broadcast. Cualquier mensaje enviado al canal llega a todos los clientes suscritos.
- `clientes` — un `ConcurrentHashMap` de `nombre → ManejadorCliente`. Permite buscar a un usuario por nombre para enviarle mensajes privados.

Cuando un cliente se conecta (`onOpen`), el servidor le pide el nombre enviando `SOLICITAR_NOMBRE`.

Cuando llega un mensaje (`onMessage`), el servidor distingue tres casos:

1. Si el cliente no tiene nombre asignado todavía, ese primer mensaje es el nombre. Se crea su `ManejadorCliente`, se registra en el mapa y se avisa a todos la lista actualizada de usuarios.
2. Si el mensaje empieza con `PRIVADO:destinatario:contenido`, se delega al `ManejadorCliente` para enviarlo directamente al destinatario.
3. Cualquier otro mensaje se trata como mensaje general y se mete al canal de broadcast.

Cuando un cliente se desconecta (`onClose`), se elimina del mapa y se actualiza la lista de usuarios para todos.

---

### `ManejadorCliente.java`

Representa a un cliente conectado. Cada cliente tiene su propia instancia.

Cuando se llama `conectar()`:

- Emite al canal que el usuario entró (`canal.onNext`). Aquí el canal actúa como **Observer** porque recibe el dato.
- Se suscribe al canal para recibir todos los mensajes futuros. Aquí el canal actúa como **Observable** porque emite datos a sus suscriptores. Cada vez que cualquier cliente manda un mensaje al canal, este cliente lo recibe y lo reenvía por su WebSocket.

`recibirMensaje(msg)` mete el mensaje al canal con el formato `nombre: mensaje`, lo que dispara la entrega a todos los suscriptores.

`enviarPrivado(destinatario, contenido)` busca al destinatario en el mapa de clientes y le envía el mensaje directamente, sin pasar por el canal general. También le envía una confirmación al remitente.

`desconectar()` cancela la suscripción al canal y emite el mensaje de salida.

---

## Cómo funciona — Frontend

### Conexión y ciclo de vida (`useEffect`)

Al cargar la página se abre una conexión WebSocket con `new WebSocket("ws://localhost:12345")`. Esto ocurre una sola vez gracias al `useEffect` con array de dependencias vacío.

Si el servidor no está disponible o se cierra, los eventos `onclose` y `onerror` actualizan el estado `servidorCaido` a `true`, lo que hace que React muestre la pantalla de aviso automáticamente.

### Clasificación de mensajes entrantes (`onmessage`)

Cada mensaje que llega del servidor se analiza en orden:

| Mensaje recibido | Acción |
|---|---|
| `SOLICITAR_NOMBRE` | Muestra la pantalla para ingresar nombre |
| `USUARIOS:Juan,Maria,...` | Actualiza la lista del panel izquierdo |
| `[Privado] Juan: hola` | Lo guarda en el historial de `Juan` |
| `[Privado a Juan] hola` | Lo guarda en el historial de `Juan` (confirmación al remitente) |
| Cualquier otro texto | Va al historial de `general` |

### Historial separado por conversación

En lugar de un solo array de mensajes, se usa un objeto `historial`:

```js
{
  general: [">> Juan entro al chat", "Juan: hola"],
  Maria:   ["[Privado] Maria: como estas", "[Privado a Maria] bien"]
}
```

Cuando el usuario hace clic en una conversación del panel izquierdo, solo cambia la variable `chat` (la clave activa). React automáticamente muestra `historial[chat]`, que es el array de esa conversación.

### Envío de mensajes

Si la conversación activa es `general`, el mensaje se envía tal cual al servidor.

Si es un usuario específico, se envía con el formato `PRIVADO:destinatario:contenido`, que el servidor sabe interpretar.

### Render condicional

El componente tiene tres pantallas posibles. React muestra solo una según el estado:

1. `servidorCaido === true` → pantalla de aviso con botón para recargar
2. `esperaNombre === true` → pantalla para ingresar el nombre
3. Estado normal → la interfaz del chat con panel de conversaciones y área de mensajes

### Scroll automático

Un segundo `useEffect` observa los cambios en `historial` y en `chat`. Cada vez que llega un mensaje nuevo o el usuario cambia de conversación, hace scroll automático al final del área de mensajes.
