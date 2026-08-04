# Plus Minus: Core Game Logic Memory

This file serves as the definitive source of truth for the strict game logic of "Plus Minus". It MUST be referenced when building the validation engine, the networking state, and the UI.

## 1. Game State and Dealing
* **Deck**: Standard 52-card deck.
* **Phase 1 Deal**: Deal exactly 5 cards to all 4 players.
* **Trump Selection (The Special Symbol)**:
    * Default trump is Spades.
    * After the first 5 cards, players can bid to change the trump suit.
    * Minimum bid to change the suit is **5**.
    * A subsequent player must bid strictly higher (e.g., 6) to override and pick a different suit.
* **Phase 2 Deal**: Deal the remaining 8 cards (so everyone has 13 cards).

## 2. Bidding Phase
* **Minimum Bid**: The absolute minimum bid for any player is **2**.
* **Trump Bidder Lock**: If a player bid to change the Trump suit (e.g., they bid 5), that number becomes their *minimum* bid. After receiving all 13 cards, they can increase their bid (e.g., to 6), but they cannot decrease it below their initial Trump-changing bid.

## 3. Trick-Taking Logic (Strict Card Validation)
The game follows strict Callbreak-style trick-taking logic, with an explicit exception for saving high cards when already trumped.

**Card Playing Rules for any Player's Turn:**
1. **Follow Suit**: The player MUST play a card of the leading suit if they have one.
    * **Condition A (Must Head)**: If no Trump (Special Symbol) has been played yet in the current trick, AND the player possesses a card of the leading suit that can strictly beat the current highest card on the table, they MUST play it. (If they cannot beat the highest card, e.g., an Ace is on the table, they may throw a lower card of that suit to save their high cards).
    * **Condition B (Can Save)**: If a Trump (Special Symbol) *has* already been played by another player, the player knows their leading suit card cannot win. In this case, the player does NOT have to play a high card. They are permitted to play a lower card of the leading suit to save their high cards.
2. **Trump if Void**: If the player does NOT have the leading suit, they MUST play a Trump card (Special Symbol) if they have one. (Note: standard rules usually mandate over-trumping if possible. The game will enforce playing a Trump, and if multiple trumps have been played, they must play a higher Trump if possible).
3. **Discard**: If the player does NOT have the leading suit AND does NOT have any Trump cards, they may play any card.

**Winning the Trick:**
* The highest Trump (Special Symbol) played wins the trick.
* If no Trumps were played, the highest card of the leading suit wins.
* The winner of the trick leads the next trick.

**Turn Timer (Auto-Play):**
* Players have a strict time limit (e.g., 15 seconds) per turn.
* If the timer expires, the server automatically selects and plays a valid card for the player according to the rules above (prioritizing the lowest valid card if multiple are valid, to avoid wasting high cards).

## 4. Scoring Engine
After 13 tricks are completed:
* **Meeting the Bid**: If `Tricks Won >= Bid`, the player's score increases by exactly `Bid`. (Overtricks grant NO extra points).
* **Failing the Bid**: If `Tricks Won < Bid`, the player's score decreases by exactly `Bid`.
* **Game Win Condition**: The game continues accumulating points across multiple rounds until a player's total score reaches **21**. If multiple players reach 21 in the same round, the player with the highest score wins.

## 5. Architectural Notes
* **Network**: Local Wi-Fi / Hotspot. Host-Client architecture.
* **Server Authority**: The Host Node is the absolute source of truth. All card validation (the rules in Section 3) MUST run on the Host. Clients only send intentions ("I want to play this card") and the Host accepts or rejects it based on the game state.
