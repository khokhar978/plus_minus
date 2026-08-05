import { useState, useEffect } from 'react';
import Card from './Card';
import { motion } from 'framer-motion';

export default function Bidding({ phase, hand, currentTurn, myName, onBid, highestBid, highestBidder, finalTrump }) {
  const [bidAmount, setBidAmount] = useState(phase === 1 ? highestBid + 1 : 2);
  const [trump, setTrump] = useState('SPADES');
  
  const symbolMap = { SPADES: '♠', HEARTS: '♥', DIAMONDS: '♦', CLUBS: '♣' };
  const isMyTurn = currentTurn === myName;
  
  // For Phase 2, if I am the Phase 1 winner, my min bid is my Phase 1 bid.
  const minBid = (phase === 2 && myName === highestBidder) ? highestBid : 2;

  // Sync bidAmount if highestBid changes externally (e.g. someone else bid higher)
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
    // If they haven't manually changed the dropdown, default to minBid
    onBid(Math.max(bidAmount, minBid));
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '40px' }}>
      <motion.div initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className="glass-panel" style={{ padding: '40px', textAlign: 'center', width: '600px' }}>
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
        
        <h3 style={{ margin: '20px 0', color: isMyTurn ? 'var(--primary)' : 'var(--text-muted)', fontSize: isMyTurn ? '1.8rem' : '1.2rem', textTransform: 'uppercase', animation: isMyTurn ? 'pulse 1.5s infinite' : 'none' }}>
          {isMyTurn ? "🚨 YOUR TURN TO BID! 🚨" : `Waiting for ${currentTurn}...`}
        </h3>
        
        <div style={{ display: 'flex', gap: '15px', justifyContent: 'center', alignItems: 'center', marginTop: '20px' }}>
          
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
      
      <div>
        <h3 style={{ textAlign: 'center', marginBottom: '20px', color: 'var(--text-muted)' }}>{phase === 1 ? "Your First 5 Cards" : "Your Full Hand"}</h3>
        <div className="hand-container">
          {hand.map((c, i) => (
            <Card key={i} symbol={c.symbol} rank={c.rank} />
          ))}
        </div>
      </div>
    </div>
  );
}
