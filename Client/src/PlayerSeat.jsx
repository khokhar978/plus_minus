import { motion } from 'framer-motion';
import { User } from 'lucide-react';

export default function PlayerSeat({ player, position, isTurn }) {
  if (!player) return null;

  const isConnected = player.connected;
  const isMe = position === 'bottom';

  // Position specific styles
  // Note: Since Framer Motion overrides the style transform property during animations,
  // we must pass the translate values directly to Framer Motion's x and y props.
  const positionStyles = {
    bottom: { bottom: '10px', left: '50%', flexDirection: 'row' },
    top: { top: '10px', left: '50%', flexDirection: 'row' },
    left: { top: '50%', left: '10px', flexDirection: 'row' },
    right: { top: '50%', right: '10px', flexDirection: 'row-reverse' }
  };

  let initialTransform = { x: 0, y: 0 };
  if (position === 'bottom' || position === 'top') initialTransform.x = "-50%";
  if (position === 'left' || position === 'right') initialTransform.y = "-50%";

  return (
    <motion.div
      initial={{ opacity: 0, x: initialTransform.x, y: initialTransform.y }}
      animate={{ 
        opacity: 1, 
        scale: isTurn ? 1.2 : 1,
        x: initialTransform.x,
        y: initialTransform.y
      }}
      transition={{ 
        scale: { type: 'spring', stiffness: 300, damping: 15 },
        opacity: { duration: 0.3 }
      }}
      style={{
        position: 'fixed',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        zIndex: isTurn ? 200 : 50,
        ...positionStyles[position]
      }}
    >
      <div 
        style={{
          width: '45px',
          height: '45px',
          borderRadius: '50%',
          background: isConnected ? (isMe ? 'var(--primary)' : 'var(--secondary)') : '#555',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          boxShadow: isTurn ? `0 0 25px 8px ${isMe ? 'var(--primary)' : 'var(--secondary)'}` : (isConnected ? '0 4px 10px rgba(0,0,0,0.3)' : 'none'),
          border: `2px solid ${isConnected ? (isTurn ? '#fff' : 'transparent') : '#ff4444'}`,
          transition: 'box-shadow 0.3s ease',
          color: 'var(--bg-dark)',
          animation: isTurn ? 'pulse 1.5s infinite' : 'none'
        }}
        title={!isConnected ? 'Disconnected' : ''}
      >
        <User size={24} />
      </div>

      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: position === 'right' ? 'flex-end' : 'flex-start',
        background: 'rgba(0,0,0,0.6)',
        padding: '4px 8px',
        borderRadius: '8px',
        border: '1px solid var(--glass-border)',
        minWidth: '80px'
      }}>
        <span style={{ 
          fontSize: '0.85rem', 
          color: isConnected ? 'white' : '#ff4444',
          fontWeight: 'bold',
          lineHeight: '1.2'
        }}>
          {player.name.substring(0, 8)}
        </span>
        <div style={{ display: 'flex', gap: '6px', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
          <span title="Tricks won / Bid">
            {player.tricks}/{player.bid}
          </span>
          <span title="Total Score" style={{ color: 'gold' }}>
            ★ {player.totalScore || 0}
          </span>
        </div>
      </div>
    </motion.div>
  );
}
