import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

public class Server {
	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>();
	HashMap<String, ClientThread> userMap = new HashMap<>();
	HashMap<String, CheckersGame> games = new HashMap<>();
	HashMap<String, String> userGameMap = new HashMap<>();
	HashMap<String, String> playerColor = new HashMap<>();
	HashMap<String, Boolean> gameOver = new HashMap<>();
	HashMap<String, String> userPasswords = new HashMap<>();
	HashMap<String, HashSet<String>> rematchVotes = new HashMap<>();
	HashSet<String> loggedInUsers = new HashSet<>();
	TheServer server;
	private Consumer<Serializable> callback;

	Server(Consumer<Serializable> call) {
		callback = call;
		server = new TheServer();
		server.start();
	}

	public class TheServer extends Thread {
		public void run() {
			try (ServerSocket mysocket = new ServerSocket(5555)) {
				callback.accept("Server is waiting for clients...");
				while (true) {
					ClientThread c = new ClientThread(mysocket.accept(), count);
					clients.add(c);
					c.start();
					count++;
				}
			} catch (Exception e) {
				callback.accept("Server socket did not launch");
			}
		}
	}

	class ClientThread extends Thread {
		Socket connection;
		int clientNumber;
		ObjectInputStream in;
		ObjectOutputStream out;
		String username;

		ClientThread(Socket s, int count) {
			this.connection = s;
			this.clientNumber = count;
		}

		public void sendGameListToAll() {
			Message gameListMsg = new Message();
			gameListMsg.setType("game_list");
			gameListMsg.setGames(new ArrayList<>(games.keySet()));

			updateAllClients(gameListMsg);
		}

		public void sendMessage(Message message) {
			try {
				out.writeObject(message);
				out.flush();
			} catch (Exception e) {
				callback.accept("Failed to send message");
			}
		}

		public void updateAllClients(Message message) {
			for (ClientThread client : clients) {
				client.sendMessage(message);
			}
		}

		public void sendUserList() {
			Message userListMessage = new Message();
			userListMessage.setType("userlist");
			userListMessage.setUsers(new ArrayList<>(userMap.keySet()));
			updateAllClients(userListMessage);
		}

		public void sendToGame(String gameId, Message msg) {
			for (ClientThread client : clients) {
				String clientGame = userGameMap.get(client.username);
				if (gameId != null && gameId.equals(clientGame)) {
					client.sendMessage(msg);
				}
			}
		}

		public void sendBoard(String gameId) {
			CheckersGame game = games.get(gameId);
			if (game == null) return;

			Message boardMsg = new Message();
			boardMsg.setType("game_state");
			boardMsg.setGameId(gameId);
			boardMsg.setBoard(convertBoard(game));
			String turn = game.getCurrentTurn();
			boardMsg.setContent(turn);
			sendToGame(gameId, boardMsg);
		}

		public String[][] convertBoard(CheckersGame game) {
			Piece[][] pieces = game.getBoard();
			String[][] board = new String[8][8];
			for (int r = 0; r < 8; r++) {
				for (int c = 0; c < 8; c++) {
					Piece p = pieces[r][c];
					if (p == null) {
						board[r][c] = "";
					} else {
						if (p.getColor().equals("RED")) {
							board[r][c] = "R";
						} else {
							board[r][c] = "B";
						}
					}
				}
			}
			return board;
		}

		public void run() {
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				connection.setTcpNoDelay(true);
			} catch (Exception e) {
				callback.accept("Streams not open");
			}
			while (true) {
				try {
					Message msg = (Message) in.readObject();
					switch (msg.getType()) {
						case "rematch": {
							String gameId = msg.getGameId();
							String user = msg.getSender();
							rematchVotes.putIfAbsent(gameId, new HashSet<>());
							rematchVotes.get(gameId).add(user);
							if (!games.containsKey(gameId)) break;

							if (rematchVotes.get(gameId).size() == 2) {
								CheckersGame newGame = new CheckersGame();
								games.put(gameId, newGame);
								gameOver.remove(gameId);
								rematchVotes.remove(gameId);

								Message restart = new Message();
								restart.setType("game_state");
								restart.setGameId(gameId);
								restart.setBoard(convertBoard(newGame));
								restart.setContent("RED");
								sendToGame(gameId, restart);
							}
							break;
						}

						case "quit_game": {
							String user = msg.getSender();
							String gameId = userGameMap.get(user);
							if (gameId != null) {
								userGameMap.remove(user);
								playerColor.remove(user);
								sendGameListToAll();
							}
							break;
						}

						case "login": {
							String user = msg.getSender();
							String pass = msg.getContent();
							if (!userPasswords.containsKey(user)) {
								Message error = new Message();
								error.setType("login_error");
								error.setContent("User does not exist");
								sendMessage(error);
								break;
							}
							if (!userPasswords.get(user).equals(pass)) {
								Message error = new Message();
								error.setType("login_error");
								error.setContent("Invalid password");
								sendMessage(error);
								break;
							}
							if (loggedInUsers.contains(user)) {
								Message error = new Message();
								error.setType("login_error");
								error.setContent("User already logged in");
								sendMessage(error);
								break;
							}
							username = user;
							userMap.put(username, this);
							loggedInUsers.add(username);

							Message success = new Message();
							success.setType("login_success");
							success.setSender("SERVER");
							success.setContent(username);
							sendMessage(success);

							sendUserList();
							callback.accept(username + " joined the server");

							Message joinMsg = new Message();
							joinMsg.setType("broadcast");
							joinMsg.setSender("SERVER");
							joinMsg.setReceiver("all");
							joinMsg.setContent(username + " joined the chat");
							updateAllClients(joinMsg);

							Message gameList = new Message();
							gameList.setType("game_list");
							gameList.setGames(new ArrayList<>(games.keySet()));
							sendMessage(gameList);
							break;
						}

						case "register": {
							String user = msg.getSender();
							String pass = msg.getContent();
							if (userPasswords.containsKey(user)) {
								Message error = new Message();
								error.setType("login_error");
								error.setContent("User already exists");
								sendMessage(error);
								break;
							}
							userPasswords.put(user, pass);
							Message success = new Message();
							success.setType("register_success");
							success.setContent("Account created");
							sendMessage(success);
							break;
						}

						case "join_game":
							String chosenGame = msg.getContent();
							boolean isNewGame = !games.containsKey(chosenGame);
							games.putIfAbsent(chosenGame, new CheckersGame());

							long playersInGame = userGameMap.values().stream().filter(g -> g.equals(chosenGame)).count();
							if (playersInGame >= 2) {
								Message error = new Message();
								error.setType("game_error");
								error.setContent("Game is full");
								sendMessage(error);
								break;
							}
							userGameMap.put(msg.getSender(), chosenGame);
							String assignedColor = (playersInGame == 0) ? "RED" : "BLACK";
							playerColor.put(msg.getSender(), assignedColor);
							Message success = new Message();
							success.setType("join_game_success");
							success.setContent(chosenGame);
							success.setColor(playerColor.get(msg.getSender()));
							sendMessage(success);
							if (isNewGame) {
								sendGameListToAll();
							}
							sendBoard(chosenGame);
							break;

						case "broadcast":
							callback.accept(msg.toString());
							updateAllClients(msg);
							break;

						case "private":
							ClientThread receiver = userMap.get(msg.getReceiver());
							if (receiver != null) {
								receiver.sendMessage(msg);
							}
							break;

						case "get_board":
							sendBoard(userGameMap.get(msg.getSender()));
							break;

						case "get_games":
							Message gameList = new Message();
							gameList.setType("game_list");
							gameList.setGames(new ArrayList<>(games.keySet()));
							sendMessage(gameList);
							break;

						case "game_chat": {
							String gameId = msg.getGameId();
							Message chat = new Message();
							chat.setType("game_chat");
							chat.setSender(msg.getSender());
							chat.setContent(msg.getContent());
							chat.setGameId(gameId);
							sendToGame(gameId, chat);
							break;
						}

						case "leave_game": {
							String user = msg.getSender();
							String gameId = userGameMap.get(user);
							if (gameId != null) {
								if (gameOver.getOrDefault(gameId, false)) {
									userGameMap.remove(user);
									playerColor.remove(user);

									boolean stillHasPlayers = userGameMap.containsValue(gameId);
									if (!stillHasPlayers) {
										games.remove(gameId);
										gameOver.remove(gameId);
									}
									sendGameListToAll();
									break;
								}

								userGameMap.remove(user);
								playerColor.remove(user);
								rematchVotes.remove(gameId);

								String remainingPlayer = null;
								for (String u : userGameMap.keySet()) {
									if (gameId.equals(userGameMap.get(u))) {
										remainingPlayer = u;
										break;
									}
								}
								if (remainingPlayer != null) {
									String winnerColor = playerColor.get(remainingPlayer);
									Message winMsg = new Message();
									winMsg.setType("win");
									winMsg.setContent(winnerColor);
									sendToGame(gameId, winMsg);
									callback.accept("🏆 " + gameId + " winner (opponent left): " + winnerColor);
								}
								games.remove(gameId);
								sendGameListToAll();
							}
							break;
						}
						case "move":
							String gameId = userGameMap.get(msg.getSender());
							CheckersGame game = games.get(gameId);
							if (game == null) break;

							String expectedTurn = game.getCurrentTurn();
							String senderColor = playerColor.get(msg.getSender());
							if (senderColor == null || !senderColor.equals(expectedTurn)) {
								Message error = new Message();
								error.setType("game_error");
								error.setContent("Not your turn");
								sendMessage(error);
								break;
							}
							boolean valid = game.move(msg.getFromRow(), msg.getFromCol(), msg.getToRow(), msg.getToCol());
							if (!valid) {
								Message err = new Message();
								err.setType("game_error");
								err.setContent("Invalid move");
								sendMessage(err);

								sendBoard(gameId);
								break;
							}
							sendBoard(gameId);

							String winner = game.checkWinner();
							if (winner != null) {
								gameOver.put(gameId, true);
								Message winMsg = new Message();
								winMsg.setType("win");
								winMsg.setContent(winner);
								callback.accept("Game " + gameId + " winner: " + winner);
								sendToGame(gameId, winMsg);
								sendGameListToAll();
							}
							break;
					}
				} catch (Exception e) {
					callback.accept("Client disconnected: " + username);
					clients.remove(this);

					if (username != null) {
						userMap.remove(username);
						loggedInUsers.remove(username);
						String gameId = userGameMap.get(username);

						if (gameId != null) {
							if (!gameOver.getOrDefault(gameId, false)) {
								String remainingPlayer = null;
								for (String u : userGameMap.keySet()) {
									if (!u.equals(username) && gameId.equals(userGameMap.get(u))) {
										remainingPlayer = u;
										break;
									}
								}
								if (remainingPlayer != null) {
									String winnerColor = playerColor.get(remainingPlayer);
									Message winMsg = new Message();
									winMsg.setType("win");
									winMsg.setContent(winnerColor);
									sendToGame(gameId, winMsg);
									callback.accept("🏆 " + gameId + " winner (disconnect): " + winnerColor);
									gameOver.put(gameId, true);
								}
							}
							userGameMap.remove(username);
							playerColor.remove(username);

							boolean stillHasPlayers = userGameMap.containsValue(gameId);
							if (!stillHasPlayers) {
								games.remove(gameId);
								gameOver.remove(gameId);
							}
							sendGameListToAll();
						}
						Message leaveMsg = new Message();
						leaveMsg.setType("broadcast");
						leaveMsg.setSender("SERVER");
						leaveMsg.setReceiver("all");
						leaveMsg.setContent(username + " left the chat");

						updateAllClients(leaveMsg);
						sendUserList();
					}
					break;
				}
			}
		}
	}
}