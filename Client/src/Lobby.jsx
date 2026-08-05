import { useState } from 'react';

export default function Lobby({ onJoin, error, lobbyCount }) {
  const [name, setName] = useState('');

  return (
    <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', minWidth: '400px' }}>
      <h1>Plus Minus</h1>
      <p style={{ marginBottom: '15px', color: 'var(--text-muted)' }}>Join the lobby to start playing</p>
      
      <div style={{ marginBottom: '20px', color: 'var(--primary)', fontSize: '1.2rem', fontWeight: 'bold' }}>
        Connected players: {lobbyCount} / 4
      </div>
      
      {error && <div style={{ color: 'var(--accent)', marginBottom: '15px', fontWeight: 'bold' }}>{error}</div>}
      
      <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
        <input 
          className="input-field" 
          placeholder="Enter your name..." 
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && onJoin(name)}
        />
        <button className="btn" onClick={() => onJoin(name)} disabled={!name}>Join</button>
      </div>
    </div>
  );
}
