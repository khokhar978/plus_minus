import { useState, useEffect } from 'react';
import Card from './Card';
import { motion, AnimatePresence } from 'framer-motion';
import PlayerSeat from './PlayerSeat';

export default function Bidding({ phase, hand, currentTurn, myName, onBid, highestBid, highestBidder, finalTrump, playersList, peekCard }) {
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

  return (
    <div style={{ width: '100vw', height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative' }}>
      
      {/* Player Seats */}
      {Object.entries(seats).map(([pos, player]) => (
        <PlayerSeat key={pos} player={player} position={pos} isTurn={currentTurn === player?.name} />
      ))}

      {/* Central Bidding Modal */}
      <div style={{ flexGrow: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 100 }}>
        <motion.div 
          initial={{ scale: 0.85, opacity: 0 }} 
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: 'spring', stiffness: 200, damping: 20 }}
          style={{ 
            background: 'linear-gradient(145deg, #faf5eb, #f0e8d8)',
            borderRadius: '24px',
            padding: '28px 32px',
            textAlign: 'center',
            width: '85%',
            maxWidth: '440px',
            color: '#3d2c1a',
            boxShadow: '0 20px 60px rgba(0,0,0,0.5), inset 0 1px 0 rgba(255,255,255,0.5)',
            border: '1px solid rgba(160, 120, 60, 0.3)'
          }}
        >
          <h2 style={{ 
            fontSize: '1.4rem', fontWeight: 800, 
            marginBottom: '16px', color: '#2c1810',
            borderBottom: '2px solid rgba(160,120,60,0.2)',
            paddingBottom: '12px'
          }}>
            {phase === 1 ? "Set the Color" : "Make your bid"}
          </h2>

          {peekCard && (
            <div style={{
              background: 'rgba(74, 222, 128, 0.15)',
              border: '1px solid rgba(74, 222, 128, 0.4)',
              borderRadius: '10px',
              padding: '6px 14px',
              marginBottom: '12px',
              fontSize: '0.85rem',
              color: '#166534',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontWeight: 700
            }}>
              <span>👀 Dealer's Bottom Card:</span>
              <span style={{ fontSize: '1rem' }}>
                {symbolMap[peekCard.symbol]} {peekCard.rank}
              </span>
            </div>
          )}
          
          {phase === 1 && highestBidder !== 'None' && (
            <p style={{ color: '#6b4423', fontSize: '0.95rem', marginBottom: '12px' }}>
              Highest: <strong>{highestBid}</strong> by <strong>{highestBidder}</strong>
            </p>
          )}
          
          {phase === 2 && (
            <p style={{ color: '#6b4423', fontSize: '0.95rem', marginBottom: '12px' }}>
              Color: <strong style={{ fontSize: '1.4rem' }}>{symbolMap[finalTrump]}</strong>
              <span style={{ opacity: 0.7 }}> — {highestBidder} (bid {highestBid})</span>
            </p>
          )}
          
          <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', alignItems: 'center', flexWrap: 'wrap' }}>
            
            {phase === 1 ? (
               <>
                  <select 
                    value={bidAmount} onChange={e => setBidAmount(Number(e.target.value))} disabled={!isMyTurn}
                    style={{ 
                      background: '#e8dcc8', border: '2px solid rgba(160,120,60,0.3)', 
                      borderRadius: '10px', padding: '10px 14px', color: '#2c1810',
                      fontFamily: 'inherit', fontSize: '1rem', fontWeight: 600, outline: 'none'
                    }}
                  >
                    {[5,6,7,8,9,10,11,12,13].filter(n => n > highestBid).map(n => <option key={n} value={n}>{n}</option>)}
                  </select>
                  <select 
                    value={trump} onChange={e => setTrump(e.target.value)} disabled={!isMyTurn}
                    style={{ 
                      background: '#e8dcc8', border: '2px solid rgba(160,120,60,0.3)', 
                      borderRadius: '10px', padding: '10px 14px', color: '#2c1810',
                      fontFamily: 'inherit', fontSize: '1rem', fontWeight: 600, outline: 'none'
                    }}
                  >
                    <option value="SPADES">♠ Spades</option>
                    <option value="HEARTS">♥ Hearts</option>
                    <option value="DIAMONDS">♦ Diamonds</option>
                    <option value="CLUBS">♣ Clubs</option>
                  </select>
                  <button 
                    onClick={() => handlePhase1Bid(bidAmount)} disabled={!isMyTurn || bidAmount <= highestBid}
                    style={{ 
                      background: isMyTurn ? '#4ade80' : '#ccc', border: 'none', borderRadius: '12px',
                      padding: '10px 20px', color: '#0a1f12', fontFamily: 'inherit',
                      fontWeight: 700, fontSize: '0.95rem', cursor: isMyTurn ? 'pointer' : 'not-allowed',
                      boxShadow: isMyTurn ? '0 4px 12px rgba(74,222,128,0.3)' : 'none'
                    }}
                  >
                    Change
                  </button>
                  <button 
                    onClick={() => handlePhase1Bid(0)} disabled={!isMyTurn}
                    style={{ 
                      background: 'rgba(60,40,20,0.15)', border: '1px solid rgba(160,120,60,0.3)',
                      borderRadius: '12px', padding: '10px 20px', color: '#6b4423',
                      fontFamily: 'inherit', fontWeight: 600, fontSize: '0.95rem',
                      cursor: isMyTurn ? 'pointer' : 'not-allowed'
                    }}
                  >
                    Skip
                  </button>
               </>
            ) : (
               <>
                  <select 
                    value={bidAmount} onChange={e => setBidAmount(Number(e.target.value))} disabled={!isMyTurn}
                    style={{ 
                      background: '#e8dcc8', border: '2px solid rgba(160,120,60,0.3)', 
                      borderRadius: '10px', padding: '10px 14px', color: '#2c1810',
                      fontFamily: 'inherit', fontSize: '1rem', fontWeight: 600, outline: 'none'
                    }}
                  >
                    {[2,3,4,5,6,7,8,9,10,11,12,13].filter(n => n >= minBid).map(n => <option key={n} value={n}>{n}</option>)}
                  </select>
                  <button 
                    onClick={handlePhase2Bid} disabled={!isMyTurn}
                    style={{ 
                      background: isMyTurn ? '#4ade80' : '#ccc', border: 'none', borderRadius: '12px',
                      padding: '10px 24px', color: '#0a1f12', fontFamily: 'inherit',
                      fontWeight: 700, fontSize: '0.95rem', cursor: isMyTurn ? 'pointer' : 'not-allowed',
                      boxShadow: isMyTurn ? '0 4px 12px rgba(74,222,128,0.3)' : 'none'
                    }}
                  >
                    ✓ Place Bid
                  </button>
               </>
            )}
          </div>
        </motion.div>
      </div>
      
      {/* Hand — flush at bottom */}
      <div style={{ zIndex: 100 }}>
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
