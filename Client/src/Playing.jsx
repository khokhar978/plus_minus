import { motion, AnimatePresence } from 'framer-motion';
import Card from './Card';

export default function Playing({ hand, table, currentTurn, myName, onPlayCard, trump, myTricks }) {
  const isMyTurn = currentTurn === myName;
  const symbolMap = { SPADES: '♠', HEARTS: '♥', DIAMONDS: '♦', CLUBS: '♣' };

  return (
    <div style={{ width: '100vw', height: '100vh', display: 'flex', flexDirection: 'column' }}>
      
      {/* Top Info Bar */}
      <div className="glass-panel" style={{ padding: '15px 30px', display: 'flex', justifyContent: 'space-between', margin: '20px', fontSize: '1.2rem', alignItems: 'center' }}>
        <div><strong>Color:</strong> <span style={{ color: trump === 'HEARTS' || trump === 'DIAMONDS' ? 'var(--accent)' : 'var(--text-main)', fontSize: '1.5rem' }}>{symbolMap[trump]}</span></div>
        <div style={{ 
          color: isMyTurn ? 'var(--primary)' : 'var(--text-main)', 
          fontWeight: isMyTurn ? '800' : '400',
          fontSize: isMyTurn ? '1.8rem' : '1.2rem',
          animation: isMyTurn ? 'pulse 1.5s infinite' : 'none'
        }}>
          {isMyTurn ? "🚨 YOUR TURN! 🚨" : `Waiting for ${currentTurn}...`}
        </div>
        <div><strong>Tricks Won:</strong> <span style={{ color: 'var(--accent)', fontWeight: 'bold' }}>{myTricks} / 13</span></div>
      </div>

      {/* Virtual Table */}
      <div className="table-layout" style={{ flexGrow: 1, margin: '0 auto' }}>
        <div className="trick-center">
          <div className="glass-panel" style={{ width: '280px', height: '280px', borderRadius: '50%', border: '2px dashed var(--glass-border)', position: 'absolute' }}></div>
          <AnimatePresence>
            {table.map((play, i) => (
              <motion.div 
                key={play.player} 
                initial={{ opacity: 0, scale: 0, rotate: Math.random() * 90 - 45 }}
                animate={{ opacity: 1, scale: 1, rotate: i * 20 - 30, x: i * 15 - 20, y: i * -15 + 10 }}
                exit={{ opacity: 0, scale: 0, y: 200 }}
                transition={{ type: "spring", stiffness: 200, damping: 20 }}
                style={{ position: 'absolute', zIndex: i }}
              >
                <Card symbol={play.symbol} rank={play.rank} style={{ pointerEvents: 'none' }} />
                <div style={{
                  background: 'var(--bg-gradient)', 
                  padding: '4px 10px', 
                  borderRadius: '12px', 
                  fontSize: '14px', 
                  fontWeight: 'bold',
                  textAlign: 'center', 
                  marginTop: '10px',
                  boxShadow: '0 4px 10px rgba(0,0,0,0.5)',
                  border: '1px solid var(--glass-border)'
                }}>
                  {play.player}
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      </div>

      {/* Hand */}
      <div style={{ paddingBottom: '40px' }}>
        <div className="hand-container">
          <AnimatePresence>
            {hand.map((c) => (
              <Card 
                key={`${c.rank}-${c.symbol}`} 
                symbol={c.symbol} 
                rank={c.rank} 
                onClick={() => isMyTurn && onPlayCard(c)}
              />
            ))}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}
