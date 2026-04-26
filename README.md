Overview: This project is a full-stack multiplayer checkers game that allows users to play against each other in real time while communicating through both lobby and in-game chat systems. It features a client-server architecture built using Java sockets, where the server manages game state, player sessions, and communication between clients. The graphical user interface was developed using JavaFX, providing scenes for login, registration, lobby interaction, and the game board itself. Core functionality includes user authentication, game creation and joining, turn-based move validation, win detection, and rematch support between players. Additional features such as player statistics (wins, losses, and win rate) and dynamic game lists enhance the overall experience. Throughout development, several bugs were identified and resolved, including synchronization issues, improper scene transitions, and edge cases like joining full games or handling disconnects. The project demonstrates concepts in networking, concurrency, GUI design, and state management in a cohesive interactive application.

Features:
  User Authentication:
    Register and log in with a username and password,
    Prevents duplicate logins and invalid credentials
    
  Lobby System:
    View available games,
    Create a new game or join an existing one,
    Global chat with all connected users
    
  Multiplayer Gameplay:
    Real-time two-player checkers matches,
    Automatic assignment of sides (RED / BLACK),
    Turn-based move validation handled by the server,
    Visual game board using JavaFX buttons
    
  Game Mechanics:
    Valid move enforcement,
    Turn tracking and updates,
    Win detection and game-over handling,
    King piece support
    
  Game Controls:
    Leave game at any time,
    Play again (rematch system with both players agreeing)
    
  Chat System:
    Lobby chat (broadcast & private messaging),
    In-game chat between players

<img width="300" height="268" alt="image" src="https://github.com/user-attachments/assets/5564f8ce-4c84-471b-b7c3-f70648955e3a" />

    
<img width="800" height="522" alt="image" src="https://github.com/user-attachments/assets/d6ddd202-bc21-4b04-9a7d-8589d252403f" />
