import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import PlayerSeat from './PlayerSeat';
import { playJoinSound, playButtonSound } from './sounds';

export default function Lobby({ onJoin, error, playersList, readyPlayers, onReady, myName, roomCode }) {
  const [name, setName] = useState('');
  const [joining, setJoining] = useState(false);
  const prevCountRef = useRef(playersList.length);

  // Reset joining state if an error occurs
  useEffect(() => {
    if (error) setJoining(false);
  }, [error]);

  // Play join sound when a new player arrives
  useEffect(() => {
    if (playersList.length > prevCountRef.current) {
      playJoinSound();
    }
    prevCountRef.current = playersList.length;
  }, [playersList.length]);

  const isJoined = myName !== '';
  const isLobbyFull = playersList.length === 4;
  const isReady = readyPlayers.includes(myName);

  // Use the same seat mapping as other components
  const myIndex = playersList.findIndex(p => p.name === myName);
  const getSeat = (pos) => {
    if (myIndex === -1) return null;
    const offsets = { bottom: 0, left: 1, top: 2, right: 3 };
    const targetIdx = (myIndex + offsets[pos]) % 4;
    return playersList[targetIdx] || null;
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
      <motion.div 
        className="glass-panel" 
        style={{ padding: '40px', textAlign: 'center', minWidth: '360px', maxWidth: '500px', zIndex: 100 }}
        initial={{ scale: 0.9, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        transition={{ type: 'spring', stiffness: 200, damping: 20 }}
      >
        {/* Decorative card fan behind title */}
        <div style={{ position: 'relative', marginBottom: '8px' }}>
          <div style={{ 
            display: 'flex', justifyContent: 'center', gap: '0px', 
            position: 'absolute', top: '-30px', left: '50%', transform: 'translateX(-50%)',
            opacity: 0.12, pointerEvents: 'none'
          }}>
            {['♠', '♥', '♦', '♣'].map((s, i) => (
              <div key={s} style={{
                fontSize: '2.2rem',
                transform: `rotate(${(i - 1.5) * 12}deg) translateY(${Math.abs(i - 1.5) * 4}px)`,
              }}>{s}</div>
            ))}
          </div>
          <h1>Plus Minus</h1>
        </div>

        {/* Room code badge */}
        {roomCode && (
          <div style={{
            display: 'inline-flex', alignItems: 'center', gap: '8px',
            background: 'rgba(251,191,36,0.1)', border: '1px solid rgba(251,191,36,0.3)',
            borderRadius: '12px', padding: '6px 16px', marginBottom: '8px'
          }}>
            <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Room</span>
            <span style={{
              color: 'var(--secondary)', fontSize: '1.4rem', fontWeight: 800, letterSpacing: '6px'
            }}>{roomCode}</span>
          </div>
        )}

        <p style={{ marginBottom: '20px', color: 'var(--text-muted)', fontSize: '1rem' }}>
          {!isJoined
            ? "Enter your name to join"
            : (isLobbyFull ? "Lobby full — ready up!" : `Waiting for players... (${playersList.length}/4)`)
          }
        </p>

        {/* Player count indicator with bounce animation */}
        <div style={{
          display: 'flex', justifyContent: 'center', gap: '8px', marginBottom: '24px'
        }}>
          {[0, 1, 2, 3].map(i => (
            <motion.div 
              key={i} 
              animate={i < playersList.length ? { 
                scale: [1, 1.4, 1],
                transition: { duration: 0.3 }
              } : {}}
              style={{
                width: '12px', height: '12px', borderRadius: '50%',
                background: i < playersList.length ? 'var(--primary)' : 'rgba(255,255,255,0.1)',
                boxShadow: i < playersList.length ? '0 0 8px var(--primary-glow)' : 'none',
                transition: 'all 0.4s ease'
              }} 
            />
          ))}
        </div>

        <AnimatePresence>
          {error && (
            <motion.div 
              initial={{ opacity: 0, y: -10 }} 
              animate={{ opacity: 1, y: 0 }} 
              exit={{ opacity: 0, y: -10 }}
              style={{ color: 'var(--accent)', marginBottom: '15px', fontWeight: 'bold' }}
            >
              {error}
            </motion.div>
          )}
        </AnimatePresence>

        {!isJoined ? (
          <motion.div 
            style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
          >
            <input
              className="input-field"
              placeholder="Your name..."
              value={name}
              onChange={(e) => setName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && name && !isLobbyFull && !joining) {
                  setJoining(true);
                  playButtonSound();
                  onJoin(name);
                }
              }}
              disabled={isLobbyFull || joining}
              style={{ flex: 1, maxWidth: '200px' }}
            />
            <button className="btn" onClick={() => { setJoining(true); playButtonSound(); onJoin(name); }} disabled={!name || isLobbyFull || joining}>Join</button>
          </motion.div>
        ) : (
          <div>
            {isLobbyFull && (
              <motion.button
                className="btn"
                onClick={() => { playButtonSound(); onReady(); }}
                disabled={isReady}
                whileTap={{ scale: 0.95 }}
                style={{
                  padding: '14px 32px', fontSize: '1.1rem',
                  background: isReady ? 'rgba(255,255,255,0.1)' : undefined,
                  color: isReady ? 'var(--text-muted)' : undefined,
                  boxShadow: isReady ? 'none' : undefined
                }}
              >
                {isReady ? "✓ Ready! Waiting..." : "I'm Ready!"}
              </motion.button>
            )}
          </div>
        )}
      </motion.div>
    </div>
  );
}
