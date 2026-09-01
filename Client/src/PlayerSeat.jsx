import { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { playTickSound } from './sounds';

export default function PlayerSeat({ player, position, isTurn, turnTimer }) {
  const [timeLeft, setTimeLeft] = useState(15);
  const lastTickRef = useRef(-1);

  useEffect(() => {
    if (!isTurn || !turnTimer || turnTimer.player !== player?.name) {
      lastTickRef.current = -1;
      return;
    }

    const updateTimer = () => {
      const elapsed = Date.now() - turnTimer.startTime;
      const remaining = Math.max(0, Math.ceil((turnTimer.duration - elapsed) / 1000));
      setTimeLeft(remaining);

      // Play tick sound when ≤5 seconds and this is a new second
      if (remaining <= 5 && remaining > 0 && remaining !== lastTickRef.current) {
        lastTickRef.current = remaining;
        playTickSound();
      }
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

  // Calculate ring progress for countdown
  const timerDuration = turnTimer?.duration || 15000;
  const progress = isTurn && turnTimer ? Math.max(0, timeLeft * 1000 / timerDuration) : 1;
  const circumference = 88; // matches stroke-dasharray in CSS
  const dashOffset = circumference * (1 - progress);

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
        <div className={avatarClass} style={{ position: 'relative' }}>
          {initial}
          {/* SVG Countdown Ring */}
          {isTurn && turnTimer && (
            <svg className="timer-ring-svg" viewBox="0 0 32 32">
              <circle
                className={`timer-ring-circle ${timeLeft <= 5 ? 'warning' : ''}`}
                cx="16"
                cy="16"
                r="14"
                style={{
                  strokeDashoffset: dashOffset,
                  transition: 'stroke-dashoffset 0.5s linear, stroke 0.3s ease'
                }}
              />
            </svg>
          )}
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
