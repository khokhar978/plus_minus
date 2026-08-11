import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';

export default function PlayerSeat({ player, position, isTurn, turnTimer }) {
  const [timeLeft, setTimeLeft] = useState(15);

  useEffect(() => {
    if (!isTurn || !turnTimer || turnTimer.player !== player?.name) {
      return;
    }

    const updateTimer = () => {
      const elapsed = Date.now() - turnTimer.startTime;
      const remaining = Math.max(0, Math.ceil((turnTimer.duration - elapsed) / 1000));
      setTimeLeft(remaining);
    };

    updateTimer();
    const interval = setInterval(updateTimer, 500);
    return () => clearInterval(interval);
  }, [isTurn, turnTimer, player?.name]);

  if (!player) return null;

  const isConnected = player.connected;
  const isMe = position === 'bottom';
  const initial = player.name.charAt(0).toUpperCase();

  // Fixed positions pinned to screen edges
  const positionStyles = {
    bottom: { bottom: '6px', left: '50%', flexDirection: 'row' },
    top:    { top: '6px',    left: '50%', flexDirection: 'row' },
    left:   { top: '50%',    left: '6px', flexDirection: 'row' },
    right:  { top: '50%',    right: '6px', flexDirection: 'row-reverse' }
  };

  // Framer Motion takes over transform, so centering must go through x/y
  let motionXY = { x: 0, y: 0 };
  if (position === 'bottom' || position === 'top') motionXY.x = "-50%";
  if (position === 'left' || position === 'right') motionXY.y = "-50%";

  const badgeClass = [
    'player-badge',
    isTurn ? 'is-turn' : '',
    isMe ? 'is-me' : '',
    !isConnected ? 'disconnected' : ''
  ].filter(Boolean).join(' ');

  const avatarClass = [
    'player-avatar',
    !isConnected ? 'dc' : (isMe ? 'me' : 'other')
  ].filter(Boolean).join(' ');

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8, x: motionXY.x, y: motionXY.y }}
      animate={{ 
        opacity: 1, 
        scale: isTurn ? 1.1 : 1,
        x: motionXY.x,
        y: motionXY.y
      }}
      transition={{ 
        scale: { type: 'spring', stiffness: 300, damping: 18 },
        opacity: { duration: 0.4 }
      }}
      style={{
        position: 'fixed',
        zIndex: isTurn ? 200 : 50,
        ...positionStyles[position]
      }}
    >
      <div className={badgeClass}>
        <div className={avatarClass}>
          {initial}
        </div>
        <div className="player-info">
          <span className="player-name">
            {isMe ? 'You' : player.name.substring(0, 8)}
            {isTurn && (
              <span style={{
                marginLeft: '6px',
                fontSize: '0.75rem',
                color: timeLeft <= 5 ? '#ef4444' : 'var(--primary)',
                fontWeight: 'bold'
              }}>
                ⏱️{timeLeft}s
              </span>
            )}
          </span>
          <div className="player-stats">
            <span>{player.tricks}/{player.bid}</span>
            <span className="score">★{player.totalScore || 0}</span>
          </div>
        </div>
      </div>
    </motion.div>
  );
}
