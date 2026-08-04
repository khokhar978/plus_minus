# Software Requirements Specification (SRS)

## Project: Plus Minus (Working Title: "The Last Braincell")

### 1. Introduction

#### 1.1 Purpose
This document explains how the "Plus Minus" game will work. It is a 4-player card game played over a local network (Wi-Fi or Hotspot). It is similar to "Callbreak" but has custom rules for picking the main card symbol, guessing how many rounds you will win, and fun game modes.

#### 1.2 Scope
The game will run without needing the internet. One player's phone will act as the "Host" (the game server), and up to three other players will connect to it as "Clients".

### 2. System Architecture & Networking

#### 2.1 Topology
**Host Node:** One player creates the game. Their device handles all the rules: shuffling the deck, checking if a move is allowed, keeping score, and sending the game updates to everyone.

**Client Nodes:** The other players connect to the Host. Their screens just show the cards and buttons, and send the player's choices (like "Play this card") to the Host.

#### 2.2 Connection
**Discovery:** The Host shows a code (or QR code) so friends can join the game on the same Wi-Fi.
**Keep-Alive:** The game constantly sends tiny messages in the background so players' phones don't disconnect if they take too long to think.

### 3. Gameplay Rules

#### 3.1 The Deck and Dealing
**The Deck:** The game uses a standard 52-card deck (Spades, Hearts, Diamonds, Clubs; numbered 2 to Ace).
**Dealing happens in two parts:**
1. **First Deal:** The system deals exactly 5 cards to each of the 4 players.
2. **Second Deal:** After the "Special Symbol" (Trump) is chosen, the remaining 8 cards are dealt to each player.

#### 3.2 Choosing the Special Symbol (Trump Color)
*   By default, **Spades** is the "Special Symbol". This means a Spade will beat any other card.
*   After receiving their first 5 cards, players look at them and can choose to change the Special Symbol to something else (like Hearts, Clubs, or Diamonds).
*   To change the symbol, a player must promise to win **at least 5 rounds** (this becomes their minimum guess).
*   If another player wants a different symbol instead, they must promise to win even more rounds (e.g., at least 6). The highest promise gets to choose the symbol.
*   If no one wants to change it, the symbol stays as Spades.
*   After this is decided, the remaining 8 cards are dealt so everyone has 13 cards.

#### 3.3 Guessing (The Bidding Phase)
Before the actual game starts, every player must guess how many rounds they will win.
*   **Minimum Guess:** Every player must guess **at least 2**. (You cannot guess 0 or 1).
*   If a player already promised to win 5 or more rounds to change the Special Symbol, that number becomes their **minimum guess**. After seeing all 13 cards, they can choose to increase their guess, but they cannot reduce it.

#### 3.4 Playing the Cards
There are 13 rounds in total. In each round, everyone throws one card.
1. **Starting the Round:** The first player throws a card (the "starting symbol").
2. **Matching the Symbol:** The other players **MUST** throw the starting symbol if they have one.
    *   **Trying to Win:** If you have the starting symbol, and no one has thrown a Special Symbol yet, you **MUST** throw a card higher than the current highest card to try and win the round.
    *   **Saving High Cards:** If someone has already thrown a Special Symbol, you know your starting symbol cannot win. In this case, you are allowed to throw a small card to save your high cards (even if you have a high one like an Ace).
3. **Using the Special Symbol:** If you do not have the starting symbol, you **MUST** throw a card of the Special Symbol (if you have one) to try and win.
4. **Throwing Anything:** If you don't have the starting symbol AND you don't have the Special Symbol, you can throw any card you want.

**Who wins the round?**
*   The highest Special Symbol card always wins.
*   If no Special Symbol was played, the highest card of the starting symbol wins.
*   The winner of the round gets to throw the first card for the next round.

**Turn Timer:**
*   Players have a time limit (e.g., 15 seconds) to throw their card.
*   If the timer runs out, the computer will automatically throw a valid card for them.

#### 3.5 Scoring
After all 13 rounds are played, the computer calculates the scores:
*   **Meeting the Guess:** If a player guessed they would win 2 rounds, and they win 2 (or 3, or 4), they get **exactly 2 points**. You do not get extra points for winning more than you guessed.
*   **Failing the Guess:** If a player guessed they would win 2 rounds, but they only win 1, they lose **2 points**.

#### 3.6 Game Modes
**Target Score (Standard):**
The game keeps playing until one player reaches a specific target score (default is 21 points). Every player plays until the end; no one is eliminated. The first person to hit 21 wins. The target score can be adjusted before the game starts.

### 4. Technical Requirements

#### 4.1 Reconnecting
If someone's Wi-Fi drops, the Host remembers their cards and score. When they reconnect, they can continue exactly where they left off.

#### 4.2 Error Checking
The game will not let a player cheat or throw the wrong card (for example, throwing a Diamond if they still have a Heart). The system will show a warning and make them choose a valid card.
