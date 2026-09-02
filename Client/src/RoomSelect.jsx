import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { playButtonSound, playJoinSound, unlockAudio } from './sounds';

export default function RoomSelect({ onRoomReady, error }) {
  const [mode, setMode] = useState(null);         // null | 'create' | 'join'
  const [joinCode, setJoinCode] = useState('');
  const [loading, setLoading] = useState(false);

  // Auto-fill room code from URL param ?room=ABCD
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const roomParam = params.get('room');
    if (roomParam) {
      setJoinCode(roomParam.toUpperCase());
      setMode('join');
    }
  }, []);

  // Reset loading if an error arrives
  useEffect(() => {
    if (error) setLoading(false);
  }, [error]);

  const handleCreate = () => {
    unlockAudio();
    playButtonSound();
    setLoading(true);
    onRoomReady('CREATE_ROOM', null);
  };

  const handleJoin = () => {
    if (!joinCode.trim()) return;
    unlockAudio();
    playJoinSound();
    setLoading(true);
    onRoomReady('JOIN_ROOM', joinCode.trim().toUpperCase());
  };

  return (
    <div style={{
      width: '100vw', height: '100vh',
      display: 'flex', justifyContent: 'center', alignItems: 'center',
    }}>
      <motion.div
        className="glass-panel"
        initial={{ scale: 0.9, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        transition={{ type: 'spring', stiffness: 200, damping: 20 }}
        style={{ padding: '44px 40px', textAlign: 'center', minWidth: '360px', maxWidth: '440px' }}
      >
        {/* Decorative suit icons */}
        <div style={{ fontSize: '2rem', letterSpacing: '8px', marginBottom: '4px', opacity: 0.2 }}>
          ♠ ♥ ♦ ♣
        </div>

        <h1 style={{ marginBottom: '6px' }}>Plus Minus</h1>
        <p style={{ color: 'var(--text-muted)', marginBottom: '32px', fontSize: '0.95rem' }}>
          A 4-player trick-taking card game
        </p>

        <AnimatePresence>
          {error && (
            <motion.div
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              style={{
                background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.4)',
                borderRadius: '10px', padding: '10px 16px',
                color: '#fca5a5', marginBottom: '20px', fontSize: '0.9rem', fontWeight: 600
              }}
            >
              {error}
            </motion.div>
          )}
        </AnimatePresence>

        <AnimatePresence mode="wait">
          {mode === null && (
            <motion.div
              key="choose"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}
            >
              <button
                className="btn"
                style={{ padding: '14px 0', fontSize: '1rem', width: '100%' }}
                onClick={handleCreate}
                disabled={loading}
              >
                {loading ? 'Creating...' : '✦ Create Room'}
              </button>

              <button
                className="btn btn-secondary"
                style={{ padding: '14px 0', fontSize: '1rem', width: '100%' }}
                onClick={() => { playButtonSound(); setMode('join'); }}
              >
                → Join Room
              </button>
            </motion.div>
          )}

          {mode === 'join' && (
            <motion.div
              key="join"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              style={{ display: 'flex', flexDirection: 'column', gap: '14px', alignItems: 'center' }}
            >
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '4px' }}>
                Enter the 4-character room code:
              </p>
              <input
                className="input-field"
                placeholder="ABCD"
                value={joinCode}
                maxLength={4}
                onChange={e => setJoinCode(e.target.value.toUpperCase())}
                onKeyDown={e => e.key === 'Enter' && joinCode.length === 4 && !loading && handleJoin()}
                style={{
                  textAlign: 'center', fontSize: '2rem', fontWeight: 800,
                  letterSpacing: '10px', width: '180px', padding: '10px',
                  background: 'rgba(0,0,0,0.5)'
                }}
                autoFocus
              />
              <div style={{ display: 'flex', gap: '10px', width: '100%' }}>
                <button
                  className="btn btn-secondary"
                  style={{ flex: 1, padding: '12px 0' }}
                  onClick={() => { setMode(null); setLoading(false); }}
                >
                  ← Back
                </button>
                <button
                  className="btn"
                  style={{ flex: 1, padding: '12px 0' }}
                  onClick={handleJoin}
                  disabled={joinCode.length !== 4 || loading}
                >
                  {loading ? 'Joining...' : 'Join'}
                </button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    </div>
  );
}
