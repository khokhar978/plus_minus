import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Card from './Card';
import PlayerSeat from './PlayerSeat';
import { playCardSound, playTrickWinSound, vibrate } from './sounds';

export default function Playing({ hand, table, currentTurn, myName, onPlayCard, trump, playersList, trickWinner, turnTimer }) {
  const isMyTurn = currentTurn === myName;
  const symbolMap = { SPADES: '♠', HEARTS: '♥', DIAMONDS: '♦', CLUBS: '♣' };
  const prevTableLen = useRef(table.length);
  const [sparkles, setSparkles] = useState([]);

  // Play card sound when a new card appears on the table
  useEffect(() => {
    if (table.length > prevTableLen.current) {
      playCardSound();
      vibrate(15);
    }
    prevTableLen.current = table.length;
  }, [table.length]);

  // Sparkle effect on trick winner
  useEffect(() => {
    if (trickWinner) {
      playTrickWinSound();
      // Generate sparkle particles
      const newSparkles = Array.from({ length: 12 }, (_, i) => ({
        id: Date.now() + i,
        x: Math.random() * 200 - 100,
        y: Math.random() * 200 - 100,
        color: ['#fbbf24', '#4ade80', '#f97316', '#60a5fa', '#f472b6'][Math.floor(Math.random() * 5)],
        delay: Math.random() * 0.3
      }));
      setSparkles(newSparkles);
      const timer = setTimeout(() => setSparkles([]), 1000);
      return () => clearTimeout(timer);
    }
  }, [trickWinner]);

  const myIndex = playersList.findIndex(p => p.name === myName);
  
  const getSeat = (pos) => {
    if (myIndex === -1 || playersList.length < 4) return null;
    const offsets = { bottom: 0, left: 1, top: 2, right: 3 };
    return playersList[(myIndex + offsets[pos]) % 4];
  };

  const seats = {
    bottom: getSeat('bottom'),
    left: getSeat('left'),
    top: getSeat('top'),
    right: getSeat('right')
  };

  // Trick winner animation — cards fly towards the winner's side
  let trickStyle = {};
  if (trickWinner) {
    const dir = { 
      [seats.bottom?.name]: 'translateY(100vh)', 
      [seats.top?.name]: 'translateY(-100vh)',
      [seats.left?.name]: 'translateX(-100vw)',
      [seats.right?.name]: 'translateX(100vw)'
    };
    trickStyle = { 
      transform: dir[trickWinner] || 'none', 
      opacity: 0, 
      transition: 'all 1.2s cubic-bezier(0.4, 0, 0.2, 1)' 
    };
  }

  // Card offsets — each card drifts towards the player who played it
  const getCardOffset = (playerName) => {
    if (playerName === seats.bottom?.name) return { x: 0, y: 35 };
    if (playerName === seats.top?.name)    return { x: 0, y: -35 };
    if (playerName === seats.left?.name)   return { x: -35, y: 0 };
    if (playerName === seats.right?.name)  return { x: 35, y: 0 };
    return { x: 0, y: 0 };
  };

  return (
    <div style={{ width: '100vw', height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative' }}>

      {/* Player Seats — pinned to edges */}
      {Object.entries(seats).map(([pos, player]) => (
        <PlayerSeat key={pos} player={player} position={pos} isTurn={currentTurn === player?.name} turnTimer={turnTimer} />
      ))}

      {/* Color badge — top left */}
      <motion.div 
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        style={{ 
          position: 'fixed', top: '12px', left: '12px', 
          background: 'var(--badge-bg)', border: '1px solid var(--badge-border)',
          borderRadius: '14px', padding: '6px 14px',
          display: 'flex', alignItems: 'center', gap: '6px',
          zIndex: 100, backdropFilter: 'blur(8px)', fontSize: '0.95rem'
        }}
      >
        <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Color</span>
        <span style={{ 
          fontSize: '1.3rem', 
          color: (trump === 'HEARTS' || trump === 'DIAMONDS') ? '#ef4444' : 'var(--text-main)',
          fontWeight: 800 
        }}>
          {symbolMap[trump]}
        </span>
      </motion.div>

      {/* Center playing area */}
      <div style={{ flexGrow: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div className="trick-center" style={trickStyle}>
          <AnimatePresence>
            {table.map((play, i) => {
              const offset = getCardOffset(play.player);
              return (
                <motion.div 
                  key={play.player} 
                  initial={{ opacity: 0, scale: 0.3, x: offset.x * 3, y: offset.y * 3 }}
                  animate={{ opacity: 1, scale: 1, rotate: (i * 10) - 15, x: offset.x, y: offset.y }}
                  exit={{ opacity: 0, scale: 0.5 }}
                  transition={{ type: "spring", stiffness: 250, damping: 22 }}
                  style={{ position: 'absolute', zIndex: i + 10 }}
                >
                  <Card symbol={play.symbol} rank={play.rank} style={{ pointerEvents: 'none', cursor: 'default' }} />
                </motion.div>
              );
            })}
          </AnimatePresence>

          {/* Sparkle particles on trick win */}
          {sparkles.length > 0 && (
            <div className="sparkle-container" style={{ position: 'absolute', top: '50%', left: '50%' }}>
              {sparkles.map(s => (
                <div 
                  key={s.id}
                  className="sparkle"
                  style={{
                    background: s.color,
                    left: `${s.x}px`,
                    top: `${s.y}px`,
                    animationDelay: `${s.delay}s`,
                    boxShadow: `0 0 6px ${s.color}`
                  }}
                />
              ))}
            </div>
          )}
        </div>

        {/* Trick winner name overlay */}
        <AnimatePresence>
          {trickWinner && (
            <motion.div
              initial={{ opacity: 0, scale: 0.5 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0 }}
              transition={{ type: 'spring', stiffness: 300, damping: 20 }}
              style={{
                position: 'absolute',
                background: 'rgba(20, 12, 5, 0.9)',
                border: '1px solid var(--secondary)',
                borderRadius: '16px',
                padding: '10px 24px',
                zIndex: 300,
                backdropFilter: 'blur(8px)',
                boxShadow: '0 0 20px rgba(251, 191, 36, 0.3)'
              }}
            >
              <span style={{ color: 'var(--secondary)', fontWeight: 800, fontSize: '1.1rem' }}>
                🏆 {trickWinner} wins!
              </span>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Hand — flush at bottom with your-turn glow */}
      <div style={{ zIndex: 100 }} className={isMyTurn ? 'hand-your-turn' : ''}>
        <div className="hand-wrapper">
          <div className="hand-container">
            <AnimatePresence>
              {hand.map((c) => (
                <Card 
                  key={`${c.rank}-${c.symbol}`} 
                  symbol={c.symbol} 
                  rank={c.rank} 
                  onClick={() => isMyTurn && onPlayCard(c)}
                  hoverable={isMyTurn}
                />
              ))}
            </AnimatePresence>
          </div>
        </div>
      </div>
    </div>
  );
}
