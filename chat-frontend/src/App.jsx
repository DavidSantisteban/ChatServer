import { useState, useEffect, useRef } from "react";
import "./App.css";

export default function App() {
  const [historial, setHistorial]         = useState({ general: [] });
  const [texto, setTexto]                 = useState("");
  const [nombre, setNombre]               = useState("");
  const [conectado, setConectado]         = useState(false);
  const [esperaNombre, setEsperaNombre]   = useState(false);
  const [usuarios, setUsuarios]           = useState([]);
  const [chat, setChat]                   = useState("general");
  const [servidorCaido, setServidorCaido] = useState(false);
  const ws  = useRef(null);
  const fin = useRef(null);

  const agregar = (conv, msg) =>
    setHistorial(h => ({ ...h, [conv]: [...(h[conv] || []), msg] }));

  useEffect(() => {
    ws.current = new WebSocket("ws://localhost:12345");

    ws.current.onmessage = ({ data }) => {
      if (data === "SOLICITAR_NOMBRE")          { setEsperaNombre(true); return; }
      if (data.startsWith("USUARIOS:"))         { setUsuarios(data.replace("USUARIOS:", "").split(",").filter(Boolean)); return; }
      if (data.startsWith("[Privado] "))        { agregar(data.match(/\[Privado\] (.+?):/)?.[1], data); return; }
      if (data.startsWith("[Privado a "))       { agregar(data.match(/\[Privado a (.+?)\]/)?.[1], data); return; }
      agregar("general", data);
    };

    ws.current.onopen  = () => setServidorCaido(false);
    ws.current.onclose = () => { setConectado(false); setServidorCaido(true); };
    ws.current.onerror = () => setServidorCaido(true);

    return () => ws.current.close();
  }, []);

  useEffect(() => { fin.current?.scrollIntoView({ behavior: "smooth" }); }, [historial, chat]);

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

  if (servidorCaido) return (
    <div className="centro">
      <p>⚠️ Servidor desconectado</p>
      <button onClick={() => window.location.reload()}>Reintentar</button>
    </div>
  );

  if (esperaNombre) return (
    <div className="centro">
      <p>Ingresa tu nombre</p>
      <input value={nombre} onChange={e => setNombre(e.target.value)}
             onKeyDown={e => e.key === "Enter" && enviarNombre()} autoFocus />
      <button onClick={enviarNombre}>Entrar</button>
    </div>
  );

  return (
    <div className="app">

      {/* Lista de conversaciones */}
      <div className="panel">
        <p className="panel-titulo">Chats</p>
        <div onClick={() => setChat("general")} className={chat === "general" ? "item activo" : "item"}>
          General
        </div>
        {usuarios.filter(u => u !== nombre).map(u => (
          <div key={u} onClick={() => setChat(u)} className={chat === u ? "item activo" : "item"}>
            {u}
          </div>
        ))}
      </div>

      {/* Ventana del chat */}
      <div className="chat">
        <div className="mensajes">
          {(historial[chat] || []).map((m, i) => <p key={i}>{m}</p>)}
          <div ref={fin} />
        </div>
        <p className="subtitulo">{chat === "general" ? "General" : `Privado: ${chat}`}</p>
        <div className="envio">
          <input value={texto} onChange={e => setTexto(e.target.value)}
                 onKeyDown={e => e.key === "Enter" && enviarMensaje()}
                 disabled={!conectado} placeholder="Mensaje..." />
          <button onClick={enviarMensaje} disabled={!conectado}>Enviar</button>
        </div>
      </div>

    </div>
  );
}
