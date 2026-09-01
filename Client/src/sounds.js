// sounds.js — Web Audio API synthesized sound effects (zero external files)
// All sounds are generated procedurally using oscillators and gain envelopes.

let audioCtx = null;
let muted = localStorage.getItem('pm-muted') === 'true';

function getCtx() {
  if (!audioCtx) {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
  }
  return audioCtx;
}

// Resume audio context on first user gesture (required by mobile browsers)
export function unlockAudio() {
  const ctx = getCtx();
  if (ctx.state === 'suspended') {
    ctx.resume();
  }
}

export function isMuted() {
  return muted;
}

export function toggleMute() {
  muted = !muted;
  localStorage.setItem('pm-muted', muted);
  return muted;
}

// ── Helpers ──────────────────────────────────────────────

function playTone(freq, duration, type = 'sine', volume = 0.15, fadeOut = 0.05) {
  if (muted) return;
  try {
    const ctx = getCtx();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, ctx.currentTime);
    gain.gain.setValueAtTime(volume, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start(ctx.currentTime);
    osc.stop(ctx.currentTime + duration + fadeOut);
  } catch (e) {
    // Silently fail — audio is non-critical
  }
}

function playNoise(duration, volume = 0.08) {
  if (muted) return;
  try {
    const ctx = getCtx();
    const bufferSize = ctx.sampleRate * duration;
    const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      data[i] = (Math.random() * 2 - 1) * 0.5;
    }
    const source = ctx.createBufferSource();
    source.buffer = buffer;
    const gain = ctx.createGain();
    gain.gain.setValueAtTime(volume, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);
    // Bandpass to make it sound more like a card shuffle
    const filter = ctx.createBiquadFilter();
    filter.type = 'bandpass';
    filter.frequency.setValueAtTime(3000, ctx.currentTime);
    filter.Q.setValueAtTime(0.5, ctx.currentTime);
    source.connect(filter);
    filter.connect(gain);
    gain.connect(ctx.destination);
    source.start(ctx.currentTime);
    source.stop(ctx.currentTime + duration);
  } catch (e) {
    // Silently fail
  }
}

// ── Public Sound Effects ────────────────────────────────

// Card played — snappy thwack
export function playCardSound() {
  playNoise(0.06, 0.12);
  playTone(800, 0.06, 'square', 0.04);
}

// Cards dealt / hand received
export function playDealSound() {
  if (muted) return;
  for (let i = 0; i < 4; i++) {
    setTimeout(() => {
      playNoise(0.04, 0.06);
    }, i * 60);
  }
}

// Your turn — notification chime (two ascending tones)
export function playYourTurnSound() {
  playTone(660, 0.12, 'sine', 0.1);
  setTimeout(() => playTone(880, 0.18, 'sine', 0.1), 120);
}

// Bid placed — click/confirm
export function playBidSound() {
  playTone(520, 0.08, 'sine', 0.1);
  setTimeout(() => playTone(780, 0.1, 'sine', 0.08), 60);
}

// Trick won — satisfying sweep
export function playTrickWinSound() {
  playTone(440, 0.15, 'sine', 0.1);
  setTimeout(() => playTone(554, 0.15, 'sine', 0.1), 100);
  setTimeout(() => playTone(659, 0.2, 'sine', 0.12), 200);
}

// Round over — short fanfare
export function playRoundOverSound() {
  const notes = [523, 659, 784, 1047];
  notes.forEach((freq, i) => {
    setTimeout(() => playTone(freq, 0.2, 'sine', 0.1), i * 120);
  });
}

// Game won — celebratory jingle
export function playGameWonSound() {
  const notes = [523, 659, 784, 880, 1047, 1047];
  notes.forEach((freq, i) => {
    setTimeout(() => playTone(freq, 0.25, 'sine', 0.12), i * 100);
  });
  // Add a shimmer
  setTimeout(() => playTone(1568, 0.4, 'sine', 0.06), 600);
}

// Button click — tactile tap
export function playButtonSound() {
  playTone(600, 0.04, 'square', 0.05);
}

// Error — low buzz
export function playErrorSound() {
  playTone(200, 0.15, 'square', 0.08);
  setTimeout(() => playTone(180, 0.2, 'square', 0.06), 100);
}

// Timer tick — when ≤5 seconds remain
export function playTickSound() {
  playTone(1000, 0.03, 'sine', 0.08);
}

// Player joined lobby
export function playJoinSound() {
  playTone(440, 0.1, 'sine', 0.08);
  setTimeout(() => playTone(554, 0.15, 'sine', 0.08), 80);
}

// Vibrate on supported devices
export function vibrate(pattern = 30) {
  try {
    if (navigator.vibrate) {
      navigator.vibrate(pattern);
    }
  } catch (e) {
    // Not supported
  }
}
