import { useState, useEffect } from 'react';
import PlayerSeat from './PlayerSeat';

export default function Lobby({ onJoin, error, playersList, readyPlayers, onReady, myName }) {
  const [name, setName] = useState('');
  const [joining, setJoining] = useState(false);

  // Reset joining state if an error occurs
  useEffect(() => {
    if (error) setJoining(false);
  }, [error]);

  const isJoined = myName !== '';
  const isLobbyFull = playersList.length === 4;
  const isReady = readyPlayers.includes(myName);

  // Use the same seat mapping as other components
  const myIndex = playersList.findIndex(p => p.name === myName);
  const getSeat = (pos) => {
    if (myIndex === -1 || playersList.length < 2) return null;
    const offsets = { bottom: 0, left: 1, top: 2, right: 3 };
    const idx = (myIndex + offsets[pos]) % playersList.length;
    if (idx >= playersList.length) return null;
    return playersList[idx];
  };

  return (
    <div style={{ 
      width: '100vw', height: '100vh', 
      display: 'flex', justifyContent: 'center', alignItems: 'center',
      position: 'relative'
    }}>
      
      {/* Show player seats if we have joined */}
      {isJoined && playersList.length >= 2 && (
        <>
          {['bottom', 'left', 'top', 'right'].map(pos => {
            const p = getSeat(pos);
            return p ? <PlayerSeat key={pos} player={p} position={pos} isTurn={false} /> : null;
          })}
        </>
      )}

      {/* Central Lobby Panel */}
      <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', minWidth: '360px', maxWidth: '500px', zIndex: 100 }}>
        <h1>Plus Minus</h1>
        
        <p style={{ marginBottom: '20px', color: 'var(--text-muted)', fontSize: '1rem' }}>
          {!isJoined 
            ? "Enter your name to join" 
            : (isLobbyFull ? "Lobby full — ready up!" : `Waiting for players... (${playersList.length}/4)`)
          }
        </p>
        
        {/* Player count indicator */}
        <div style={{ 
          display: 'flex', justifyContent: 'center', gap: '8px', marginBottom: '24px' 
        }}>
          {[0,1,2,3].map(i => (
            <div key={i} style={{
              width: '12px', height: '12px', borderRadius: '50%',
              background: i < playersList.length ? 'var(--primary)' : 'rgba(255,255,255,0.1)',
              boxShadow: i < playersList.length ? '0 0 8px var(--primary-glow)' : 'none',
              transition: 'all 0.4s ease'
            }} />
          ))}
        </div>
        
        {error && <div style={{ color: 'var(--accent)', marginBottom: '15px', fontWeight: 'bold' }}>{error}</div>}
        
        {!isJoined ? (
          <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
            <input 
              className="input-field" 
              placeholder="Your name..." 
              value={name}
              onChange={(e) => setName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && name && !isLobbyFull && !joining) {
                  setJoining(true);
                  onJoin(name);
                }
              }}
              disabled={isLobbyFull || joining}
              style={{ flex: 1, maxWidth: '200px' }}
            />
            <button className="btn" onClick={() => { setJoining(true); onJoin(name); }} disabled={!name || isLobbyFull || joining}>Join</button>
          </div>
        ) : (
          <div>
            {isLobbyFull && (
              <button 
                className="btn" 
                onClick={onReady} 
                disabled={isReady}
                style={{ 
                  padding: '14px 32px', fontSize: '1.1rem',
                  background: isReady ? 'rgba(255,255,255,0.1)' : undefined,
                  color: isReady ? 'var(--text-muted)' : undefined,
                  boxShadow: isReady ? 'none' : undefined
                }}
              >
                {isReady ? "✓ Ready! Waiting..." : "I'm Ready!"}
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
