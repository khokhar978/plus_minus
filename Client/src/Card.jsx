import { motion } from 'framer-motion';

const symbolMap = { SPADES: '♠', HEARTS: '♥', DIAMONDS: '♦', CLUBS: '♣' };
const rankMap = { TWO: '2', THREE: '3', FOUR: '4', FIVE: '5', SIX: '6', SEVEN: '7', EIGHT: '8', NINE: '9', TEN: '10', JACK: 'J', QUEEN: 'Q', KING: 'K', ACE: 'A' };
const faceRanks = new Set(['JACK', 'QUEEN', 'KING', 'ACE']);

export default function Card({ symbol, rank, onClick, style, hoverable }) {
  const isRed = symbol === 'HEARTS' || symbol === 'DIAMONDS';
  const isFace = faceRanks.has(rank);

  const classes = [
    'playing-card',
    isRed ? 'red' : 'black',
    isFace ? 'face-card' : '',
    hoverable ? 'hoverable' : ''
  ].filter(Boolean).join(' ');

  return (
    <motion.div 
      className={classes}
      onClick={onClick}
      style={style}
      layoutId={`${rank}-${symbol}`}
      initial={{ opacity: 0, y: 50 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.8, y: -50 }}
      whileTap={hoverable ? { scale: 0.92 } : undefined}
    >
      <div className="card-top-left">{rankMap[rank]}<br/>{symbolMap[symbol]}</div>
      <div className="card-center">{symbolMap[symbol]}</div>
      <div className="card-bottom-right">{rankMap[rank]}<br/>{symbolMap[symbol]}</div>
    </motion.div>
  );
}
