import { useState, useEffect, useRef } from 'react'
import Lobby from './Lobby'
import Bidding from './Bidding'
import Playing from './Playing'
import Leaderboard from './Leaderboard'

export default function App() {
  const [gameState, setGameState] = useState('LOBBY'); // LOBBY, BIDDING_1, BIDDING_2, PLAYING
  const [myName, setMyName] = useState('');
  const [hand, setHand] = useState([]);
  const [turn, setTurn] = useState('');
  const [error, setError] = useState('');
  
  // Bidding states
  const [highestBidder, setHighestBidder] = useState('None');
  const [highestBid, setHighestBid] = useState(4);
  const [finalTrump, setFinalTrump] = useState('');
  
  // Playing states
  const [table, setTable] = useState([]);
  const [myTricks, setMyTricks] = useState(0);
  const [lobbyCount, setLobbyCount] = useState(0);
  
  // Scoring states
  const [scores, setScores] = useState([]);
  const [gameWinner, setGameWinner] = useState(null);

  // Use a ref so the WebSocket is created ONCE and never destroyed on re-render
  const wsRef = useRef(null);
  const myNameRef = useRef('');

  useEffect(() => {
    // Dynamically connect to the server's IP address on the network instead of just localhost
    const ws = new WebSocket(`ws://${window.location.hostname}:8887`);
    wsRef.current = ws;
    
    ws.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      console.log("Received:", msg);
      
      if (msg.type === 'ERROR') {
        setError(msg.message);
        setTimeout(() => setError(''), 3000);
      }
      
      else if (msg.type === 'LOBBY_UPDATE') {
        setLobbyCount(msg.count);
      }
      
      else if (msg.type === 'GAME_START') {
        setGameState('BIDDING_1');
        setHand(msg.yourHand);
        setTurn(msg.turn);
        setHighestBid(4);
        setHighestBidder('None');
      }
      
      else if (msg.type === 'BID_1_UPDATE') {
        setHighestBid(msg.highestBid);
        setHighestBidder(msg.highestBidder);
        if (msg.nextTurn) setTurn(msg.nextTurn);
      }
      
      else if (msg.type === 'PHASE_2_START') {
        setGameState('BIDDING_2');
        setHand(msg.yourHand);
        setTurn(msg.turn);
        setFinalTrump(msg.finalTrump);
      }
      
      else if (msg.type === 'BID_2_UPDATE') {
        if (msg.nextTurn) setTurn(msg.nextTurn);
      }
      
      else if (msg.type === 'GAME_READY') {
        setGameState('PLAYING');
        if (msg.yourHand) setHand(msg.yourHand);
        setTurn(msg.turn);
      }
      
      else if (msg.type === 'CARD_PLAYED') {
        setTable(prev => [...prev, { player: msg.player, symbol: msg.symbol, rank: msg.rank }]);
        const currentName = myNameRef.current;
        if (msg.player === currentName) {
           setHand(prev => prev.filter(c => !(c.rank === msg.rank && c.symbol === msg.symbol)));
        }
      }
      
      else if (msg.type === 'NEXT_TURN') {
        setTurn(msg.turn);
      }
      
      else if (msg.type === 'TRICK_WINNER') {
        const currentName = myNameRef.current;
        if (msg.winner === currentName) setMyTricks(prev => prev + 1);
        setTimeout(() => {
          setTable([]);
        }, 2500);
      }
      
      else if (msg.type === 'ROUND_OVER' || msg.type === 'GAME_OVER') {
        setGameState('LEADERBOARD');
        setScores(msg.scores);
        if (msg.type === 'GAME_OVER') setGameWinner(msg.winner);
        setHand([]);
        setTable([]);
        setMyTricks(0);
      }
    };

    return () => ws.close();
  }, []); // Empty dependency array — WebSocket is created ONCE

  const handleJoin = (name) => {
    setMyName(name);
    myNameRef.current = name;
    wsRef.current.send(JSON.stringify({ action: 'JOIN', name }));
  };

  const handleBidPhase1 = (amount, selectedTrump) => {
    wsRef.current.send(JSON.stringify({ action: 'BID_PHASE_1', amount, trump: selectedTrump }));
  };

  const handleBidPhase2 = (amount) => {
    wsRef.current.send(JSON.stringify({ action: 'BID_PHASE_2', amount }));
  };

  const handlePlayCard = (card) => {
    wsRef.current.send(JSON.stringify({ action: 'PLAY_CARD', symbol: card.symbol, rank: card.rank }));
  };

  const handleNextRound = () => {
    wsRef.current.send(JSON.stringify({ action: 'NEXT_ROUND' }));
  };

  return (
    <>
      {error && (
        <div style={{ position: 'fixed', top: '20px', left: '50%', transform: 'translateX(-50%)', background: 'var(--accent)', padding: '10px 20px', borderRadius: '8px', zIndex: 1000, boxShadow: '0 4px 15px rgba(255,8,68,0.5)', fontWeight: 'bold' }}>
          {error}
        </div>
      )}
      
      {gameState === 'LOBBY' && <Lobby onJoin={handleJoin} error={error} lobbyCount={lobbyCount} />}
      
      {gameState === 'BIDDING_1' && (
        <Bidding 
          phase={1}
          hand={hand} 
          currentTurn={turn} 
          myName={myName} 
          onBid={handleBidPhase1}
          highestBid={highestBid}
          highestBidder={highestBidder}
        />
      )}
      
      {gameState === 'BIDDING_2' && (
        <Bidding 
          phase={2}
          hand={hand} 
          currentTurn={turn} 
          myName={myName} 
          onBid={handleBidPhase2}
          highestBid={highestBid}
          highestBidder={highestBidder}
          finalTrump={finalTrump}
        />
      )}
      
      {gameState === 'PLAYING' && (
        <Playing 
          hand={hand} 
          table={table} 
          currentTurn={turn} 
          myName={myName} 
          onPlayCard={handlePlayCard}
          trump={finalTrump}
          myTricks={myTricks}
        />
      )}
      
      {gameState === 'LEADERBOARD' && (
        <Leaderboard 
          scores={scores} 
          gameWinner={gameWinner} 
          onNextRound={handleNextRound} 
        />
      )}
    </>
  )
}
