import { useState, useEffect, useRef } from "react"; // Hooks -> funcionales especiales
import "./App.css";

export default function App() {
  // use state guarda valores y cambia lo que se ve en pantalla
  const [historial, setHistorial] = useState({ general: [] });
  //     ^valor        ^función         ^valor inicial
  const [texto, setTexto] = useState("");
  const [nombre, setNombre] = useState("");
  const [conectado, setConectado] = useState(false);
  const [esperaNombre, setEsperaNombre] = useState(false);
  const [usuarios, setUsuarios] = useState([]);
  const [chat, setChat] = useState("general");
  const [servidorCaido, setServidorCaido] = useState(false);

  // useRef, guarda valore sigual que useState, pero no afectan en lo que se ve en la pantalla
  const ws = useRef(null); // -> conexion websocket
  const fin = useRef(null);

  // Variable que guarda una funcion lambda
  // conv especifica de que conversacion si de la global o de las privadas
  const agregar = (conv, msg) => {
    setHistorial((historialActual) => {
      const mensajes = historialActual[conv] || []; // obtiene mensajes previos, si no hay, devuelve []

      return {
        ...historialActual, // Copia el historial anterior
        [conv]: [...mensajes, msg], // Agrega el mensaje nuevo
      };
    });
  };

  // USO DE LA CONEXION WEB SOCKET
  useEffect(() => {
    ws.current = new WebSocket("ws://localhost:12345");

    // ws.current.onmessage se ejecuta cada vez que se llega un mensaje
    ws.current.onmessage = ({ data }) => {
      if (data === "SOLICITAR_NOMBRE") {
        setEsperaNombre(true);
        return;
      }
      // Recibe  la lista de los usuarios
      if (data.startsWith("USUARIOS:")) {
        setUsuarios(data.replace("USUARIOS:", "").split(","));
        return;
      }
      if (data.startsWith("[Privado] ")) {
        const resto = data.replace("[Privado] ", ""); // Elimina el prefijo Privado
        const remitente = resto.split(":")[0]; // Solo queda nombre, mensaje
        agregar(remitente, data);
        return;
      }
      if (data.startsWith("[Privado a ")) {
        const resto = data.replace("[Privado a ", "");
        const destinatario = resto.split("]")[0];
        agregar(destinatario, data);
        return;
      }
      agregar("general", data);
    };

    ws.current.onopen = () => setServidorCaido(false);
    ws.current.onclose = () => {
      setConectado(false);
      setServidorCaido(true);
    };
    ws.current.onerror = () => setServidorCaido(true);

    return () => ws.current.close();
  }, []);

  useEffect(() => {
    fin.current?.scrollIntoView({ behavior: "smooth" });
  }, [historial, chat]);

  const enviarNombre = () => {
    if (!nombre.trim()) return;
    ws.current.send(nombre.trim());
    setEsperaNombre(false);
    setConectado(true);
  };

  const enviarMensaje = () => {
    if (!texto.trim()) return;
    ws.current.send(chat === "general" ? texto : `PRIVADO:${chat}:${texto}`);
    setTexto("");
  };

  if (servidorCaido)
    return (
      <div className="centro">
        <p>⚠️ Servidor desconectado</p>
        <button onClick={() => window.location.reload()}>Reintentar</button>
      </div>
    );

  if (esperaNombre)
    return (
      <div className="centro">
        <p>Ingresa tu nombre</p>
        <input
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && enviarNombre()}
          autoFocus
        />
        <button onClick={enviarNombre}>Entrar</button>
      </div>
    );

  return (
    <div className="app">
      {/* Lista de conversaciones */}
      <div className="panel">
        <p className="panel-titulo">Chats</p>
        <div
          onClick={() => setChat("general")}
          className={chat === "general" ? "item activo" : "item"}
        >
          General
        </div>
        {usuarios
          .filter((u) => u !== nombre)
          .map((u) => (
            <div
              key={u}
              onClick={() => setChat(u)}
              className={chat === u ? "item activo" : "item"}
            >
              {u}
            </div>
          ))}
      </div>

      {/* Ventana del chat */}
      <div className="chat">
        <div className="mensajes">
          {(historial[chat] || []).map((m, i) => (
            <p key={i}>{m}</p>
          ))}
          <div ref={fin} />
        </div>
        <p className="subtitulo">
          {chat === "general" ? "General" : `Privado: ${chat}`}
        </p>
        <div className="envio">
          <input
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && enviarMensaje()}
            disabled={!conectado}
            placeholder="Mensaje..."
          />
          <button onClick={enviarMensaje} disabled={!conectado}>
            Enviar
          </button>
        </div>
      </div>
    </div>
  );
}
