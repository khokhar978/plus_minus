import { motion } from 'framer-motion';

export default function Leaderboard({ scores, gameWinner, onNextRound, readyPlayers, myName }) {
  const sortedScores = [...scores].sort((a, b) => b.totalScore - a.totalScore);
  const isReady = readyPlayers.includes(myName);

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
            <div style={{ fontSize: '3rem', marginBottom: '8px' }}>🏆</div>
            <h2 style={{ color: 'var(--secondary)', fontSize: '1.5rem', margin: 0 }}>{gameWinner} Wins!</h2>
            <p style={{ color: 'var(--text-muted)', marginTop: '4px', fontSize: '0.9rem' }}>First to 21 points</p>
          </div>
        ) : (
          <h2 style={{ textAlign: 'center', marginBottom: '24px', color: 'var(--primary)', fontSize: '1.4rem' }}>
            Round Over
          </h2>
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
              <div style={{ 
                width: '32px', height: '32px', borderRadius: '50%',
                background: i === 0 ? 'var(--secondary)' : 'rgba(255,255,255,0.1)',
                display: 'flex', justifyContent: 'center', alignItems: 'center',
                fontWeight: 800, fontSize: '0.85rem',
                color: i === 0 ? '#2c1810' : 'var(--text-muted)',
                flexShrink: 0
              }}>
                {i === 0 ? '👑' : i + 1}
              </div>

              {/* Name */}
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: '1rem' }}>{s.player}</div>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                  Bid {s.bid} · Won {s.tricks}
                </div>
              </div>

              {/* Points earned */}
              <div style={{ 
                fontWeight: 700, fontSize: '0.9rem',
                color: s.pointsEarned > 0 ? '#4ade80' : '#ef4444',
                minWidth: '40px', textAlign: 'right'
              }}>
                {s.pointsEarned > 0 ? '+' : ''}{s.pointsEarned}
              </div>

              {/* Total score */}
              <div style={{ 
                fontWeight: 800, fontSize: '1.3rem', 
                color: 'var(--secondary)',
                minWidth: '40px', textAlign: 'right'
              }}>
                {s.totalScore}
              </div>
            </motion.div>
          ))}
        </div>

        {/* Ready button */}
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <button 
            className="btn" 
            onClick={onNextRound}
            disabled={isReady}
            style={{ 
              padding: '14px 32px', fontSize: '1.05rem',
              ...(isReady ? { background: 'rgba(255,255,255,0.1)', color: 'var(--text-muted)', boxShadow: 'none' } : {}),
              ...(gameWinner ? { background: 'linear-gradient(135deg, var(--secondary), #f97316)' } : {})
            }}
          >
            {isReady ? "✓ Ready! Waiting..." : (gameWinner ? "Play Again" : "Next Round")}
          </button>
        </div>
        
      </motion.div>
    </div>
  );
}
