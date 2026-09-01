import { useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { playRoundOverSound, playGameWonSound, playButtonSound } from './sounds';

export default function Leaderboard({ scores, gameWinner, onNextRound, readyPlayers, myName, autoSkipped }) {
  const sortedScores = [...scores].sort((a, b) => b.totalScore - a.totalScore);
  const isReady = readyPlayers.includes(myName);
  const soundPlayedRef = useRef(false);

  // Play sound on mount
  useEffect(() => {
    if (!soundPlayedRef.current) {
      soundPlayedRef.current = true;
      if (gameWinner) {
        playGameWonSound();
      } else {
        playRoundOverSound();
      }
    }
  }, [gameWinner]);

  return (
    <div style={{ 
      width: '100vw', height: '100vh', 
      display: 'flex', justifyContent: 'center', alignItems: 'center',
      padding: '20px'
    }}>
      <motion.div 
        initial={{ y: 30, opacity: 0 }} 
        animate={{ y: 0, opacity: 1 }} 
        transition={{ type: 'spring', stiffness: 200, damping: 20 }}
        className="glass-panel" 
        style={{ padding: '32px', width: '100%', maxWidth: '600px' }}
      >
        
        {/* Header */}
        {gameWinner ? (
          <div style={{ textAlign: 'center', marginBottom: '24px' }}>
            <motion.div 
              initial={{ scale: 0, rotate: -180 }}
              animate={{ scale: 1, rotate: 0 }}
              transition={{ type: 'spring', stiffness: 200, damping: 12, delay: 0.2 }}
              style={{ fontSize: '3rem', marginBottom: '8px' }}
            >
              🏆
            </motion.div>
            <motion.h2 
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              style={{ color: 'var(--secondary)', fontSize: '1.5rem', margin: 0 }}
            >
              {gameWinner} Wins!
            </motion.h2>
            <p style={{ color: 'var(--text-muted)', marginTop: '4px', fontSize: '0.9rem' }}>First to 21 points</p>

            {/* Confetti */}
            <div style={{ position: 'relative', height: 0, overflow: 'visible' }}>
              {Array.from({ length: 20 }, (_, i) => (
                <div
                  key={i}
                  className="confetti-piece"
                  style={{
                    left: `${Math.random() * 100}%`,
                    top: '-20px',
                    background: ['#fbbf24', '#4ade80', '#ef4444', '#60a5fa', '#f472b6', '#a78bfa'][i % 6],
                    animationDelay: `${Math.random() * 0.5}s`,
                    animationDuration: `${1 + Math.random() * 0.8}s`
                  }}
                />
              ))}
            </div>
          </div>
        ) : (
          <motion.h2 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            style={{ textAlign: 'center', marginBottom: '24px', color: 'var(--primary)', fontSize: '1.4rem' }}
          >
            {autoSkipped ? '⚡ Easy Round — Auto-Awarded!' : 'Round Over'}
          </motion.h2>
        )}

        {/* Score Cards */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '24px' }}>
          {sortedScores.map((s, i) => (
            <motion.div 
              key={s.player}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: i * 0.12 }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                padding: '12px 16px',
                background: i === 0 ? 'rgba(251,191,36,0.1)' : 'rgba(255,255,255,0.03)',
                borderRadius: '14px',
                border: `1px solid ${i === 0 ? 'rgba(251,191,36,0.3)' : 'rgba(255,255,255,0.05)'}`,
              }}
            >
              {/* Rank */}
              <motion.div 
                initial={i === 0 ? { scale: 0 } : undefined}
                animate={i === 0 ? { scale: 1 } : undefined}
                transition={i === 0 ? { type: 'spring', delay: 0.3 } : undefined}
                style={{ 
                  width: '32px', height: '32px', borderRadius: '50%',
                  background: i === 0 ? 'var(--secondary)' : 'rgba(255,255,255,0.1)',
                  display: 'flex', justifyContent: 'center', alignItems: 'center',
                  fontWeight: 800, fontSize: '0.85rem',
                  color: i === 0 ? '#2c1810' : 'var(--text-muted)',
                  flexShrink: 0
                }}
              >
                {i === 0 ? '👑' : i + 1}
              </motion.div>

              {/* Name */}
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: '1rem' }}>{s.player}</div>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                  Bid {s.bid} · Won {s.tricks}
                </div>
              </div>

              {/* Points earned */}
              <motion.div 
                initial={{ scale: 0.5, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ delay: i * 0.12 + 0.2 }}
                style={{ 
                  fontWeight: 700, fontSize: '0.9rem',
                  color: s.pointsEarned > 0 ? '#4ade80' : '#ef4444',
                  minWidth: '40px', textAlign: 'right'
                }}
              >
                {s.pointsEarned > 0 ? '+' : ''}{s.pointsEarned}
              </motion.div>

              {/* Total score */}
              <motion.div 
                initial={{ scale: 0.5, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ delay: i * 0.12 + 0.3 }}
                style={{ 
                  fontWeight: 800, fontSize: '1.3rem', 
                  color: 'var(--secondary)',
                  minWidth: '40px', textAlign: 'right'
                }}
              >
                {s.totalScore}
              </motion.div>
            </motion.div>
          ))}
        </div>

        {/* Ready button */}
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <motion.button 
            className="btn" 
            onClick={() => { playButtonSound(); onNextRound(); }}
            disabled={isReady}
            whileTap={{ scale: 0.95 }}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
            style={{ 
              padding: '14px 32px', fontSize: '1.05rem',
              ...(isReady ? { background: 'rgba(255,255,255,0.1)', color: 'var(--text-muted)', boxShadow: 'none' } : {}),
              ...(gameWinner ? { background: 'linear-gradient(135deg, var(--secondary), #f97316)' } : {})
            }}
          >
            {isReady ? "✓ Ready! Waiting..." : (gameWinner ? "Play Again" : "Next Round")}
          </motion.button>
        </div>
        
      </motion.div>
    </div>
  );
}
