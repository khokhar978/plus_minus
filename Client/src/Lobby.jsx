import { useState } from 'react';
import PlayerBubbles from './PlayerBubbles';

export default function Lobby({ onJoin, error, playersList, readyPlayers, onReady, myName }) {
  const [name, setName] = useState('');

  const isJoined = myName !== '';
  const isLobbyFull = playersList.length === 4;
  const isReady = readyPlayers.includes(myName);

  return (
    <>
      <PlayerBubbles players={playersList} myName={myName} />
      <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', minWidth: '400px' }}>
        <h1>Plus Minus</h1>
        <p style={{ marginBottom: '15px', color: 'var(--text-muted)' }}>
          {!isJoined ? "Join the lobby to start playing" : (isLobbyFull ? "Waiting for everyone to be ready..." : "Waiting for more players...")}
        </p>
        
        <div style={{ marginBottom: '20px', color: 'var(--primary)', fontSize: '1.2rem', fontWeight: 'bold' }}>
          Connected players: {playersList.length} / 4
        </div>
        
        {error && <div style={{ color: 'var(--accent)', marginBottom: '15px', fontWeight: 'bold' }}>{error}</div>}
        
        {!isJoined ? (
          <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
            <input 
              className="input-field" 
              placeholder="Enter your name..." 
              value={name}
              onChange={(e) => setName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && onJoin(name)}
              disabled={isLobbyFull}
            />
            <button className="btn" onClick={() => onJoin(name)} disabled={!name || isLobbyFull}>Join</button>
          </div>
        ) : (
          <div>
            {isLobbyFull && (
              <button 
                className="btn" 
                onClick={onReady} 
                disabled={isReady}
                style={{ background: isReady ? 'var(--secondary)' : 'var(--primary)', padding: '15px 30px', fontSize: '1.2rem' }}
              >
                {isReady ? "Ready! Waiting for others..." : "Click when Ready to Start!"}
              </button>
            )}
          </div>
        )}
      </div>
    </>
  );
}
