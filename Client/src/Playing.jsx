import { motion, AnimatePresence } from 'framer-motion';
import Card from './Card';
import PlayerSeat from './PlayerSeat';

export default function Playing({ hand, table, currentTurn, myName, onPlayCard, trump, playersList, trickWinner }) {
  const isMyTurn = currentTurn === myName;
  const symbolMap = { SPADES: '♠', HEARTS: '♥', DIAMONDS: '♦', CLUBS: '♣' };

  const myIndex = playersList.findIndex(p => p.name === myName);
  
  const getPlayerByRelativePosition = (pos) => {
    if (myIndex === -1 || playersList.length < 4) return null;
    let offset = 0;
    if (pos === 'bottom') offset = 0;
    if (pos === 'left') offset = 1;
    if (pos === 'top') offset = 2;
    if (pos === 'right') offset = 3;
    
    const targetIndex = (myIndex + offset) % 4;
    return playersList[targetIndex];
  };

  const bottomPlayer = getPlayerByRelativePosition('bottom');
  const leftPlayer = getPlayerByRelativePosition('left');
  const topPlayer = getPlayerByRelativePosition('top');
  const rightPlayer = getPlayerByRelativePosition('right');

  let tableTransform = 'none';
  let tableOpacity = 1;
  if (trickWinner) {
    if (trickWinner === bottomPlayer?.name) tableTransform = 'translateY(100vh)';
    else if (trickWinner === topPlayer?.name) tableTransform = 'translateY(-100vh)';
    else if (trickWinner === leftPlayer?.name) tableTransform = 'translateX(-100vw)';
    else if (trickWinner === rightPlayer?.name) tableTransform = 'translateX(100vw)';
    tableOpacity = 0;
  }

  return (
    <div style={{ width: '100vw', height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative' }}>

      {/* 4-Way Player Seats Anchored to Screen Edges */}
      <PlayerSeat player={bottomPlayer} position="bottom" isTurn={currentTurn === bottomPlayer?.name} />
      <PlayerSeat player={leftPlayer} position="left" isTurn={currentTurn === leftPlayer?.name} />
      <PlayerSeat player={topPlayer} position="top" isTurn={currentTurn === topPlayer?.name} />
      <PlayerSeat player={rightPlayer} position="right" isTurn={currentTurn === rightPlayer?.name} />

      {/* Floating Color Badge */}
      <div className="glass-panel" style={{ 
        position: 'fixed', 
        top: '20px', 
        left: '20px', 
        padding: '10px 15px', 
        fontSize: '1.2rem', 
        zIndex: 100,
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        width: 'fit-content'
      }}>
        <strong>Color:</strong> 
        <span style={{ color: trump === 'HEARTS' || trump === 'DIAMONDS' ? 'var(--accent)' : 'var(--text-main)', fontSize: '1.5rem' }}>
          {symbolMap[trump]}
        </span>
      </div>

      {/* Virtual Table */}
      <div className="table-layout" style={{ flexGrow: 1, margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div 
          className="trick-center" 
          style={{ 
            transform: tableTransform, 
            opacity: tableOpacity,
            transition: trickWinner ? 'all 1s cubic-bezier(0.25, 0.1, 0.25, 1)' : 'none',
          }}
        >
          <div className="glass-panel" style={{ width: '100%', height: '100%', borderRadius: '50%', border: '2px dashed var(--glass-border)', position: 'absolute' }}></div>
          
          <AnimatePresence>
            {table.map((play, i) => {
              // Determine which seat played this card to offset it slightly towards them
              let cardX = 0;
              let cardY = 0;
              if (play.player === bottomPlayer?.name) cardY = 40;
              else if (play.player === topPlayer?.name) cardY = -40;
              else if (play.player === leftPlayer?.name) cardX = -40;
              else if (play.player === rightPlayer?.name) cardX = 40;

              return (
                <motion.div 
                  key={play.player} 
                  initial={{ opacity: 0, scale: 0, x: cardX * 2, y: cardY * 2 }}
                  animate={{ opacity: 1, scale: 1, rotate: (i * 15) - 20, x: cardX, y: cardY }}
                  exit={{ opacity: 0, scale: 0 }}
                  transition={{ type: "spring", stiffness: 200, damping: 20 }}
                  style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', zIndex: i + 10 }}
                >
                  <Card symbol={play.symbol} rank={play.rank} style={{ pointerEvents: 'none' }} />
                  <div style={{
                    background: 'var(--bg-gradient)', 
                    padding: '2px 8px', 
                    borderRadius: '8px', 
                    fontSize: '12px', 
                    fontWeight: 'bold',
                    textAlign: 'center', 
                    marginTop: '5px',
                    boxShadow: '0 4px 10px rgba(0,0,0,0.5)',
                    border: '1px solid var(--glass-border)',
                    position: 'absolute',
                    bottom: '-25px',
                    left: '50%',
                    transform: 'translateX(-50%)'
                  }}>
                    {play.player.substring(0, 8)}
                  </div>
                </motion.div>
              );
            })}
          </AnimatePresence>
        </div>
      </div>

      {/* Hand */}
      <div style={{ paddingBottom: '10px', zIndex: 100 }}>
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
