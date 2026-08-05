import { motion } from 'framer-motion';

export default function Leaderboard({ scores, gameWinner, onNextRound }) {
  // Sort scores descending by totalScore
  const sortedScores = [...scores].sort((a, b) => b.totalScore - a.totalScore);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '30px', padding: '20px', width: '100vw' }}>
      <motion.div initial={{ y: -50, opacity: 0 }} animate={{ y: 0, opacity: 1 }} className="glass-panel" style={{ padding: '40px', width: '90%', maxWidth: '900px' }}>
        
        {gameWinner ? (
          <div style={{ textAlign: 'center', marginBottom: '30px' }}>
            <h1 style={{ fontSize: '4rem', marginBottom: '10px' }}>🏆 {gameWinner} WINS! 🏆</h1>
            <p style={{ fontSize: '1.5rem', color: 'var(--primary)' }}>Target score of 21 reached!</p>
          </div>
        ) : (
          <h1 style={{ textAlign: 'center', marginBottom: '30px' }}>Round Complete!</h1>
        )}

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '1.2rem' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid var(--glass-border)', color: 'var(--primary)' }}>
                <th style={{ padding: '15px' }}>Player</th>
                <th style={{ padding: '15px' }}>Bid</th>
                <th style={{ padding: '15px' }}>Tricks Won</th>
                <th style={{ padding: '15px' }}>Points Earned</th>
                <th style={{ padding: '15px', textAlign: 'right' }}>Total Score</th>
              </tr>
            </thead>
            <tbody>
              {sortedScores.map((s, i) => (
                <motion.tr 
                  key={s.player} 
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.1 }}
                  style={{ 
                    borderBottom: '1px solid var(--glass-border)',
                    background: i === 0 ? 'rgba(0, 242, 254, 0.1)' : 'transparent'
                  }}
                >
                  <td style={{ padding: '15px', fontWeight: 'bold' }}>
                    {i === 0 ? '👑 ' : ''}{i + 1}. {s.player}
                  </td>
                  <td style={{ padding: '15px' }}>{s.bid}</td>
                  <td style={{ padding: '15px', color: s.tricks >= s.bid ? '#4caf50' : 'white' }}>{s.tricks}</td>
                  <td style={{ padding: '15px', color: s.pointsEarned > 0 ? '#4caf50' : 'var(--accent)', fontWeight: 'bold' }}>
                    {s.pointsEarned > 0 ? '+' : ''}{s.pointsEarned}
                  </td>
                  <td style={{ padding: '15px', textAlign: 'right', fontWeight: 'bold', fontSize: '1.5rem', color: 'var(--secondary)' }}>
                    {s.totalScore}
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>

        {!gameWinner && (
          <div style={{ display: 'flex', justifyContent: 'center', marginTop: '40px' }}>
            <button className="btn" onClick={onNextRound} style={{ fontSize: '1.2rem', padding: '15px 40px' }}>
              Start Next Round
            </button>
          </div>
        )}

      </motion.div>
    </div>
  );
}
