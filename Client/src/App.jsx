import { useState, useEffect, useRef } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import Lobby from './Lobby'
import Bidding from './Bidding'
import Playing from './Playing'
import Leaderboard from './Leaderboard'
import PlayerBubbles from './PlayerBubbles'
import { unlockAudio, toggleMute, isMuted, playYourTurnSound, playDealSound, playErrorSound, vibrate } from './sounds'

export default function App() {
  const [gameState, setGameState] = useState('LOBBY');
  const [myName, setMyName] = useState('');
  const [hand, setHand] = useState([]);
  const [turn, setTurn] = useState('');
  const [error, setError] = useState('');
  
  // Lobby states
  const [playersList, setPlayersList] = useState([]);
  const [readyPlayers, setReadyPlayers] = useState([]);
  
  // Bidding states
  const [highestBidder, setHighestBidder] = useState('None');
  const [highestBid, setHighestBid] = useState(4);
  const [finalTrump, setFinalTrump] = useState('');
  const [peekCard, setPeekCard] = useState(null);
  const [dealer, setDealer] = useState(null);
  
  // Playing states
  const [table, setTable] = useState([]);
  const [trickWinner, setTrickWinner] = useState(null);
  
  // Scoring states
  const [scores, setScores] = useState([]);
  const [gameWinner, setGameWinner] = useState(null);
  const [autoSkipped, setAutoSkipped] = useState(false);
  const [turnTimer, setTurnTimer] = useState(null);
  const [autoPlayedNotice, setAutoPlayedNotice] = useState('');
  const [soundMuted, setSoundMuted] = useState(isMuted());

  const wsRef = useRef(null);
  const myNameRef = useRef(myName);
  const trickGenRef = useRef(0);

  useEffect(() => {
    myNameRef.current = myName;
  }, [myName]);

  useEffect(() => {
    let reconnectTimer;
    let isIntentionallyClosed = false;

    const connect = () => {
      if (wsRef.current?.readyState === WebSocket.OPEN || wsRef.current?.readyState === WebSocket.CONNECTING) {
        return;
      }
      
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const ws = new WebSocket(`${protocol}//${window.location.host}/ws`);
      wsRef.current = ws;

      ws.onopen = () => {
        setError('');
        if (myNameRef.current) {
          ws.send(JSON.stringify({ action: 'JOIN', name: myNameRef.current }));
        }
      };
      
      ws.onmessage = (event) => {
        const msg = JSON.parse(event.data);
      console.log("Received:", msg);
      
      if (msg.type === 'ERROR') {
        setError(msg.message);
        playErrorSound();
        setTimeout(() => setError(''), 3000);
      }
      else if (msg.type === 'PLAYERS_SYNC') {
        setPlayersList(msg.players);
      }
      else if (msg.type === 'READY_UPDATE') {
        setReadyPlayers(msg.readyPlayers);
      }
      else if (msg.type === 'GAME_START') {
        setGameState('BIDDING_1');
        setHand(msg.yourHand);
        setTurn(msg.turn);
        setHighestBid(4);
        setHighestBidder('None');
        setReadyPlayers([]);
        setPeekCard(msg.peekCard || null);
        setDealer(msg.dealer || null);
        playDealSound();
      }
      else if (msg.type === 'BID_1_UPDATE') {
        setHighestBid(msg.highestBid);
        setHighestBidder(msg.highestBidder);
        if (msg.nextTurn) setTurn(msg.nextTurn);
      }
      else if (msg.type === 'PHASE_2_START') {
        setGameState('BIDDING_2');
        if (msg.yourHand) setHand(msg.yourHand);
        setTurn(msg.turn);
        setFinalTrump(msg.finalTrump);
      }
      else if (msg.type === 'BID_2_UPDATE') {
        if (msg.nextTurn) setTurn(msg.nextTurn);
      }
      else if (msg.type === 'GAME_READY') {
        setGameState('PLAYING');
        if (msg.yourHand) setHand(msg.yourHand);
        if (msg.turn) setTurn(msg.turn);
        if (msg.table) setTable(msg.table);
      }
      else if (msg.type === 'CARD_PLAYED') {
        trickGenRef.current++;
        setTable(prev => [...prev, { player: msg.player, symbol: msg.symbol, rank: msg.rank }]);
        const currentName = myNameRef.current;
        if (msg.player === currentName) {
           setHand(prev => prev.filter(c => !(c.rank === msg.rank && c.symbol === msg.symbol)));
        }
      }
      else if (msg.type === 'NEXT_TURN') {
        setTurn(msg.turn);
        if (msg.turn === myNameRef.current) {
          playYourTurnSound();
          vibrate(40);
        }
      }
      else if (msg.type === 'TRICK_WINNER') {
        setTrickWinner(msg.winner);
        const gen = trickGenRef.current;
        setTimeout(() => {
          if (trickGenRef.current === gen) setTable([]);
          setTrickWinner(null);
        }, 2500);
      }
      else if (msg.type === 'TIMER_START') {
        setTurnTimer({ player: msg.player, duration: msg.duration, startTime: Date.now() });
      }
      else if (msg.type === 'AUTO_PLAYED') {
        const text = msg.player === myNameRef.current ? "Time's up! Auto-played for you." : `Time's up! Auto-played for ${msg.player}.`;
        setAutoPlayedNotice(text);
        setTimeout(() => setAutoPlayedNotice(''), 3000);
      }
      else if (msg.type === 'ROUND_OVER' || msg.type === 'GAME_OVER') {
        setGameState('LEADERBOARD');
        setScores(msg.scores);
        setAutoSkipped(msg.autoSkipped || false);
        if (msg.type === 'GAME_OVER') setGameWinner(msg.winner);
        setHand([]);
        setTable([]);
        setTurnTimer(null);
      }
    };

    ws.onclose = () => {
      if (!isIntentionallyClosed) {
        setError('Connection lost — reconnecting...');
        reconnectTimer = setTimeout(connect, 2000);
      }
    };

    ws.onerror = () => {
      ws.close();
    };
  };

  connect();

  return () => {
    isIntentionallyClosed = true;
    clearTimeout(reconnectTimer);
    if (wsRef.current) wsRef.current.close();
  };
}, []);

  const handleJoin = (name) => {
    unlockAudio();
    setMyName(name);
    myNameRef.current = name;
    wsRef.current.send(JSON.stringify({ action: 'JOIN', name }));
  };

  const handleReady = () => {
    wsRef.current.send(JSON.stringify({ action: 'READY' }));
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

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(err => {
        console.log("Error attempting to enable fullscreen:", err.message);
      });
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen();
      }
    }
  };

  const handleToggleMute = () => {
    unlockAudio();
    const newMuted = toggleMute();
    setSoundMuted(newMuted);
  };

  return (
    <>
      {/* Mute toggle */}
      <button 
        className="mute-btn"
        onClick={handleToggleMute}
        title={soundMuted ? 'Unmute' : 'Mute'}
      >
        {soundMuted ? '🔇' : '🔊'}
      </button>

      {/* Fullscreen toggle */}
      <button 
        onClick={toggleFullscreen}
        style={{
          position: 'fixed',
          bottom: '20px',
          right: '20px',
          background: 'rgba(0,0,0,0.5)',
          border: '1px solid var(--badge-border)',
          color: 'var(--text-muted)',
          borderRadius: '50%',
          width: '40px',
          height: '40px',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          cursor: 'pointer',
          zIndex: 1000,
          backdropFilter: 'blur(8px)',
          fontSize: '1.1rem',
          transition: 'all 0.2s ease'
        }}
        title="Toggle Fullscreen"
      >
        ⛶
      </button>

      <AnimatePresence>
        {error && (
          <motion.div 
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            style={{ position: 'fixed', top: '20px', left: '50%', transform: 'translateX(-50%)', background: 'var(--accent)', padding: '10px 20px', borderRadius: '12px', zIndex: 1000, boxShadow: '0 4px 15px rgba(255,8,68,0.5)', fontWeight: 'bold' }}
          >
            {error}
          </motion.div>
        )}
      </AnimatePresence>
      
      {gameState === 'LOBBY' && (
        <Lobby 
          onJoin={handleJoin} 
          error={error} 
          playersList={playersList} 
          readyPlayers={readyPlayers} 
          onReady={handleReady} 
          myName={myName}
        />
      )}
      
      {autoPlayedNotice && (
        <div style={{
          position: 'fixed',
          top: '20px',
          left: '50%',
          transform: 'translateX(-50%)',
          background: 'rgba(239, 68, 68, 0.9)',
          color: '#ffffff',
          padding: '10px 20px',
          borderRadius: '12px',
          fontWeight: 'bold',
          zIndex: 2000,
          boxShadow: '0 4px 15px rgba(0,0,0,0.3)'
        }}>
          ⏱️ {autoPlayedNotice}
        </div>
      )}

      {gameState === 'BIDDING_1' && (
        <Bidding 
          phase={1}
          hand={hand} 
          currentTurn={turn} 
          myName={myName} 
          onBid={handleBidPhase1}
          highestBid={highestBid}
          highestBidder={highestBidder}
          playersList={playersList}
          peekCard={peekCard}
          turnTimer={turnTimer}
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
          playersList={playersList}
          peekCard={peekCard}
          turnTimer={turnTimer}
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
          playersList={playersList}
          trickWinner={trickWinner}
          turnTimer={turnTimer}
        />
      )}
      
      {gameState === 'LEADERBOARD' && (
        <Leaderboard 
          scores={scores} 
          gameWinner={gameWinner} 
          onNextRound={handleReady} 
          readyPlayers={readyPlayers}
          myName={myName}
          autoSkipped={autoSkipped}
        />
      )}
    </>
  )
}
