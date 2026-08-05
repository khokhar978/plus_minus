import { motion } from 'framer-motion';
import { User } from 'lucide-react';

export default function PlayerBubbles({ players, myName }) {
  if (!players || players.length === 0) return null;

  return (
    <div className="bubbles-container">
      {players.map((p, i) => {
        const isMe = p.name === myName;
        const isConnected = p.connected;

        return (
          <motion.div
            key={p.name}
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            title={`${p.name} ${!isConnected ? '(Disconnected)' : ''}`}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '5px'
            }}
          >
            <div 
              className="bubble-avatar"
              style={{
                background: isConnected ? (isMe ? 'var(--primary)' : 'var(--secondary)') : '#555',
                boxShadow: isConnected ? `0 0 15px ${isMe ? 'var(--primary)' : 'var(--secondary)'}` : 'none',
                border: `2px solid ${isConnected ? 'transparent' : '#ff4444'}`,
              }}
            >
              <User size={20} />
            </div>
            <span style={{ 
              fontSize: '0.75rem', 
              color: isConnected ? 'var(--text-light)' : '#ff4444',
              fontWeight: 'bold',
              textShadow: '0 2px 4px rgba(0,0,0,0.5)',
              background: 'rgba(0,0,0,0.5)',
              padding: '2px 6px',
              borderRadius: '10px'
            }}>
              {p.name.substring(0, 8)}
            </span>
          </motion.div>
        );
      })}
    </div>
  );
}
