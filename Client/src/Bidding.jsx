import { useState, useEffect } from 'react';
import Card from './Card';
import { motion, AnimatePresence } from 'framer-motion';
import PlayerSeat from './PlayerSeat';

export default function Bidding({ phase, hand, currentTurn, myName, onBid, highestBid, highestBidder, finalTrump, playersList }) {
  const [bidAmount, setBidAmount] = useState(phase === 1 ? highestBid + 1 : 2);
  const [trump, setTrump] = useState('SPADES');
  
  const symbolMap = { SPADES: '♠', HEARTS: '♥', DIAMONDS: '♦', CLUBS: '♣' };
  const isMyTurn = currentTurn === myName;
  
  const minBid = (phase === 2 && myName === highestBidder) ? highestBid : 2;

  useEffect(() => {
    if (phase === 1 && bidAmount <= highestBid) {
      setBidAmount(highestBid < 13 ? highestBid + 1 : 13);
    }
    if (phase === 2 && bidAmount < minBid) {
      setBidAmount(minBid);
    }
  }, [highestBid, phase, minBid]);

  const handlePhase1Bid = (amount) => {
    onBid(amount, trump);
  };

  const handlePhase2Bid = () => {
    onBid(Math.max(bidAmount, minBid));
  };

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

  return (
    <div style={{ width: '100vw', height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative' }}>
      
      {/* 4-Way Player Seats Anchored to Screen Edges */}
      <PlayerSeat player={bottomPlayer} position="bottom" isTurn={currentTurn === bottomPlayer?.name} />
      <PlayerSeat player={leftPlayer} position="left" isTurn={currentTurn === leftPlayer?.name} />
      <PlayerSeat player={topPlayer} position="top" isTurn={currentTurn === topPlayer?.name} />
      <PlayerSeat player={rightPlayer} position="right" isTurn={currentTurn === rightPlayer?.name} />

      {/* Central Bidding UI */}
      <div style={{ flexGrow: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 100 }}>
        <motion.div initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className="glass-panel" style={{ padding: '40px', textAlign: 'center', width: '90%', maxWidth: '600px', position: 'relative' }}>
          
          <h2 style={{ marginBottom: '15px' }}>{phase === 1 ? "Phase 1: Set the Color" : "Phase 2: Final Bids"}</h2>
          
          {phase === 1 && highestBidder !== 'None' && (
            <p style={{ color: 'var(--secondary)', fontSize: '1.2rem', marginBottom: '10px' }}>
              Current Highest Bid: <strong>{highestBid}</strong> by {highestBidder} 
              <br/>(Color is hidden!)
            </p>
          )}
          
          {phase === 2 && (
            <p style={{ color: 'var(--secondary)', fontSize: '1.2rem', marginBottom: '10px' }}>
              The chosen Color is: <strong><span style={{fontSize: '1.5rem'}}>{symbolMap[finalTrump]}</span></strong>
              <br/>(Set by {highestBidder} with a bid of {highestBid})
            </p>
          )}
          
          <h3 style={{ margin: '20px 0', color: isMyTurn ? 'var(--primary)' : 'var(--text-muted)', fontSize: isMyTurn ? '1.5rem' : '1.2rem', textTransform: 'uppercase', animation: isMyTurn ? 'pulse 1.5s infinite' : 'none' }}>
            {isMyTurn ? "🚨 YOUR TURN TO BID! 🚨" : `Waiting for ${currentTurn}...`}
          </h3>
          
          <div style={{ display: 'flex', gap: '15px', justifyContent: 'center', alignItems: 'center', marginTop: '20px', flexWrap: 'wrap' }}>
            
            {phase === 1 ? (
               <>
                  <select className="input-field" value={bidAmount} onChange={e => setBidAmount(Number(e.target.value))} disabled={!isMyTurn}>
                    {[5,6,7,8,9,10,11,12,13].filter(n => n > highestBid).map(n => <option key={n} value={n}>{n}</option>)}
                  </select>
                  <select className="input-field" value={trump} onChange={e => setTrump(e.target.value)} disabled={!isMyTurn}>
                    <option value="SPADES">♠ Spades</option>
                    <option value="HEARTS">♥ Hearts</option>
                    <option value="DIAMONDS">♦ Diamonds</option>
                    <option value="CLUBS">♣ Clubs</option>
                  </select>
                  <button className="btn" onClick={() => handlePhase1Bid(bidAmount)} disabled={!isMyTurn || bidAmount <= highestBid}>Change Color</button>
                  <button className="btn" style={{ background: '#444' }} onClick={() => handlePhase1Bid(0)} disabled={!isMyTurn}>Skip</button>
               </>
            ) : (
               <>
                  <select className="input-field" value={bidAmount} onChange={e => setBidAmount(Number(e.target.value))} disabled={!isMyTurn}>
                    {[2,3,4,5,6,7,8,9,10,11,12,13].filter(n => n >= minBid).map(n => <option key={n} value={n}>{n}</option>)}
                  </select>
                  <button className="btn" onClick={handlePhase2Bid} disabled={!isMyTurn}>Place Final Bid</button>
               </>
            )}
            
          </div>
        </motion.div>
      </div>
      
      {/* Hand */}
      <div style={{ paddingBottom: '100px', zIndex: 100 }}>
        <div className="hand-wrapper">
          <div className="hand-container">
            <AnimatePresence>
              {hand.map((c) => (
                <Card 
                  key={`${c.rank}-${c.symbol}`} 
                  symbol={c.symbol} 
                  rank={c.rank} 
                />
              ))}
            </AnimatePresence>
          </div>
        </div>
      </div>
    </div>
  );
}
