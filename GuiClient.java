import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiClient extends Application {
	TextField usernameField;
	TextField messageField;
	TextField gameMessageField;
	TextField loginUser;
	TextField regUser;
	Button joinButton;
	Button sendButton;
	Button joinGameButton;
	Button createGameButton;
	Button gameSendButton;
	Button leaveGameButton;
	Button loginButton;
	Button registerButton;
	Button playAgainButton;
	Button registerBtn;
	Button backBtn;
	ComboBox<String> recipientBox;
	ComboBox<String> gameBox;
	PasswordField loginPass;
	PasswordField regPass;
	HashMap<String, Scene> sceneMap;
	Client clientConnection;
	String username = null;
	String currentGame = null;
	int fromRow = -1;
	int fromCol = -1;
	boolean gameReady = false;
	boolean waitingForServer = false;
	ListView<String> lobbyChat;
	ListView<String> gameLog;
	ListView<String> gameChat;
	Label sideLabel;
	Label gameLabel;
	Label turnLabel;
	Label loginStatus;
	Label registerStatus;
	Label status;
	GridPane board;
	Button[][] cells = new Button[8][8];
	Stage primary;

	public static void main(String[] args) {launch(args);}

	@Override
	public void start(Stage primaryStage) throws Exception {
		clientConnection = new Client(data -> {
			Platform.runLater(() -> {
				Message msg = (Message) data;

				switch (msg.getType()) {
					case "broadcast":
					case "private":
						lobbyChat.getItems().add(msg.toString());
						break;

					case "userlist":
						recipientBox.getItems().clear();
						recipientBox.getItems().add("all");
						recipientBox.getItems().addAll(msg.getUsers());
						recipientBox.setValue("all");
						break;

					case "login_error":
						String err = msg.getContent();
						if (primary.getScene() == sceneMap.get("register")) {
							registerStatus.setText("ERROR: " + err);
						} else {
							loginStatus.setText("LOGIN ERROR: " + err);
						}
						break;

					case "game_error":
						String errorMsg = "GAME ERROR: " + msg.getContent();
						if (primary.getScene() == sceneMap.get("game")) {
							gameLog.getItems().add(errorMsg);
							gameLog.scrollTo(gameLog.getItems().size() - 1);
						} else {
							lobbyChat.getItems().add(errorMsg);
							lobbyChat.scrollTo(lobbyChat.getItems().size() - 1);
						}
						fromRow = -1;
						fromCol = -1;
						waitingForServer = false;
						if (currentGame != null && username != null) {
							Message req = new Message();
							req.setType("get_board");
							req.setSender(username);
							req.setGameId(currentGame);
							clientConnection.send(req);
						}
						break;

					case "game_list":
						gameBox.getItems().setAll(msg.getGames());
						break;

					case "win":
						String result;
						if ("DRAW".equals(msg.getContent())) {
							result = "🤝 DRAW";
						} else {
							result = "🏆 WINNER: " + msg.getContent();
						}
						gameLog.getItems().add(result);
						gameLog.scrollTo(gameLog.getItems().size() - 1);
						playAgainButton.setDisable(false);
						break;

					case "join_game_success": {
						String gameId = msg.getContent();
						String color = msg.getColor();
						if (gameId == null || color == null) return;
						currentGame = gameId;
						gameReady = true;
						waitingForServer = false;
						lobbyChat.getItems().add("Joined game: " + gameId + " as " + color);
						gameLabel.setText(currentGame);
						sideLabel.setText("Side: " + color);
						updateBoard(msg.getBoard());
						switchToGameScene();
						break;
					}

					case "game_state":
						if (msg.getGameId() == null || !msg.getGameId().equals(currentGame)) break;
						gameReady = true;
						waitingForServer = false;
						fromRow = -1;
						fromCol = -1;
						updateBoard(msg.getBoard());

						String turn = msg.getContent();
						if (turn != null) {
							turnLabel.setText("Turn: " + turn);
							if ("RED".equals(turn)) {
								turnLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16;");
							} else {
								turnLabel.setStyle("-fx-text-fill: black; -fx-font-size: 16;");
							}
						}
						break;

					case "game_chat":
						gameChat.getItems().add(msg.getSender() + ": " + msg.getContent());
						gameChat.scrollTo(gameChat.getItems().size() - 1);
						break;

					case "login_success":
						username = msg.getContent();
						loginUser.setDisable(true);
						loginPass.setDisable(true);
						loginButton.setDisable(true);
						registerButton.setDisable(true);
						loginStatus.setText("Login successful");
						primary.setScene(sceneMap.get("lobby"));
						break;

					case "register_success":
						loginStatus.setText("Account created! Go back and login.");
						primary.setScene(sceneMap.get("login"));
						break;
				}
			});
		});
		clientConnection.start();

		this.primary = primaryStage;

		gameBox = new ComboBox<>();
		recipientBox = new ComboBox<>();
		recipientBox.getItems().add("all");
		recipientBox.setValue("all");

		lobbyChat = new ListView<>();
		gameLog = new ListView<>();
		gameChat = new ListView<>();

		usernameField = new TextField();
		usernameField.setPromptText("Enter username");
		messageField = new TextField();
		messageField.setPromptText("Enter message");
		gameMessageField = new TextField();
		gameMessageField.setPromptText("Message game chat...");

		sideLabel = new Label("Side: Unknown");
		gameLabel = new Label("Game: None");
		turnLabel = new Label("Turn: ?");
		registerStatus = new Label();

		joinButton = new Button("Join");
		sendButton = new Button("Send");
		joinGameButton = new Button("Join Game");
		createGameButton = new Button("Create Game");
		gameSendButton = new Button("Send");
		leaveGameButton = new Button("Leave Game");
		registerButton = new Button("Register");
		playAgainButton = new Button("Play Again");
		playAgainButton.setDisable(true);

		sceneMap = new HashMap<>();
		sceneMap.put("lobby", createLobbyScene());
		sceneMap.put("game", null);
		sceneMap.put("login", createLoginScene());
		sceneMap.put("register", createRegisterScene());
		primaryStage.setScene(sceneMap.get("login"));
		primaryStage.show();

		board = new GridPane();
		initBoard(board);

		playAgainButton.setOnAction(e -> {
			if (username != null && currentGame != null) {
				Message msg = new Message();
				msg.setType("rematch");
				msg.setSender(username);
				msg.setGameId(currentGame);
				clientConnection.send(msg);
			}
			playAgainButton.setDisable(true);
			waitingForServer = false;
			fromRow = -1;
			fromCol = -1;
		});

		registerButton.setOnAction(e -> {
			primary.setScene(sceneMap.get("register"));
		});

		loginButton.setOnAction(e -> {
			Message msg = new Message();
			msg.setType("login");
			msg.setSender(loginUser.getText());
			msg.setContent(loginPass.getText());
			clientConnection.send(msg);
		});

		leaveGameButton.setOnAction(e -> {
			if (username != null && currentGame != null) {
				Message leaveMsg = new Message();
				leaveMsg.setType("leave_game");
				leaveMsg.setSender(username);
				leaveMsg.setGameId(currentGame);

				clientConnection.send(leaveMsg);
			}
			currentGame = null;
			gameReady = false;
			waitingForServer = false;
			fromRow = -1;
			fromCol = -1;
			playAgainButton.setDisable(true);
			primary.setScene(sceneMap.get("lobby"));
		});

		gameSendButton.setOnAction(e -> {
			if (username != null && !gameMessageField.getText().isEmpty()) {
				Message msg = new Message();
				msg.setSender(username);
				msg.setContent(gameMessageField.getText());
				msg.setType("game_chat");
				msg.setGameId(currentGame);
				clientConnection.send(msg);
				gameMessageField.clear();
			}
		});

		joinButton.setOnAction(e -> {
			if (usernameField.getText().isEmpty()) return;
			Message joinMsg = new Message();
			joinMsg.setType("join");
			joinMsg.setSender(usernameField.getText());
			clientConnection.send(joinMsg);

			Message getGames = new Message();
			getGames.setType("get_games");
			getGames.setSender(usernameField.getText());
			clientConnection.send(getGames);
		});

		sendButton.setOnAction(e -> {
			if (username != null && !messageField.getText().isEmpty()) {
				Message msg = new Message();
				msg.setSender(username);
				msg.setContent(messageField.getText());
				if (recipientBox.getValue().equals("all")) {
					msg.setType("broadcast");
					msg.setReceiver("all");
				} else {
					msg.setType("private");
					msg.setReceiver(recipientBox.getValue());
				}
				clientConnection.send(msg);
				messageField.clear();
			}
		});

		joinGameButton.setOnAction(e -> {
			if (username != null && gameBox.getValue() != null) {
				currentGame = gameBox.getValue();
				Message msg = new Message();
				msg.setType("join_game");
				msg.setSender(username);
				msg.setContent(currentGame);
				clientConnection.send(msg);
			}
		});

		createGameButton.setOnAction(e -> {
			if (username != null) {
				String newGameId = "Game " + (gameBox.getItems().size() + 1);
				gameBox.getItems().add(newGameId);
				gameBox.setValue(newGameId);

				Message msg = new Message();
				msg.setType("join_game");
				msg.setSender(username);
				msg.setContent(newGameId);
				clientConnection.send(msg);

				Message getGames = new Message();
				getGames.setType("get_games");
				getGames.setSender(username);
				clientConnection.send(getGames);

				currentGame = newGameId;
				switchToGameScene();
			}
		});

		registerBtn.setOnAction(e -> {
			String user = regUser.getText();
			String pass = regPass.getText();
			if (user == null || user.isBlank() || pass == null || pass.isBlank()) {
				status.setText("Username and password cannot be empty");
				return;
			}
			Message msg = new Message();
			msg.setType("register");
			msg.setSender(regUser.getText());
			msg.setContent(regPass.getText());
			clientConnection.send(msg);
			status.setText("Registering...");
		});

		backBtn.setOnAction(e -> {
			primary.setScene(createLoginScene());
		});

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});
	}

	public void initBoard(GridPane board) {
		for (int r = 0; r < 8; r++) {
			for (int c = 0; c < 8; c++) {
				Button cell = new Button();
				cell.setPrefSize(50, 50);

				final int row = r;
				final int col = c;

				cell.setOnAction(e -> handleClick(row, col));

				cells[r][c] = cell;
				board.add(cell, c, r);
			}
		}
	}

	public Scene createRegisterScene() {
		regUser = new TextField();
		regUser.setPromptText("Username");
		regPass = new PasswordField();
		regPass.setPromptText("Password");
		registerBtn = new Button("Sign-up");
		backBtn = new Button("Back");
		HBox buttonRow = new HBox(155, backBtn, registerBtn);
		VBox layout = new VBox(10, new Label("Register"), regUser, regPass, buttonRow, registerStatus);
		layout.setPadding(new Insets(20));
		return new Scene(layout, 300, 250);
	}

	public Scene createLoginScene() {
		loginUser = new TextField();
		loginUser.setPromptText("Username");
		loginPass = new PasswordField();
		loginPass.setPromptText("Password");
		loginStatus = new Label();
		loginButton = new Button("Login");
		HBox buttonRow = new HBox(155, loginButton, registerButton);
		VBox layout = new VBox(10, new Label("Username"), loginUser, new Label("Password"), loginPass, buttonRow, loginStatus);
		layout.setPadding(new Insets(20));
		return new Scene(layout, 300, 250);
	}

	public Scene createGameScene() {
		VBox left = new VBox(10, board);
		left.setPadding(new Insets(10));
		HBox gameBox = new HBox(55, gameLabel, leaveGameButton);
		HBox gameBox2 = new HBox(55, sideLabel, playAgainButton);
		VBox right = new VBox(10, gameBox, gameBox2, turnLabel, gameChat, gameMessageField, gameSendButton, gameLog);
		right.setPadding(new Insets(10));
		right.setPrefWidth(200);
		HBox root = new HBox(20, left, right);
		root.setPadding(new Insets(20));
		return new Scene(root, 800, 500);
	}

	public void switchToGameScene() {
		if (sceneMap.get("game") == null) {
			sceneMap.put("game", createGameScene());
		}
		primary.setScene(sceneMap.get("game"));
	}

	public Scene createLobbyScene() {
		VBox lobby = new VBox(10, gameBox, joinGameButton, createGameButton, messageField, sendButton, lobbyChat);
		lobby.setPadding(new Insets(20));
		return new Scene(lobby, 400, 400);
	}

	public void handleClick(int row, int col) {
		if (currentGame == null) return;
		if (waitingForServer) return;
		if (fromRow == -1) {
			fromRow = row;
			fromCol = col;
			gameLog.getItems().add("Selected: " + row + "," + col);
			gameLog.scrollTo(gameLog.getItems().size() - 1);
			return;
		}
		waitingForServer = true;

		// second click; destination
		Message move = new Message();
		move.setType("move");
		move.setSender(username);
		move.setFromRow(fromRow);
		move.setFromCol(fromCol);
		move.setToRow(row);
		move.setToCol(col);
		move.setGameId(currentGame);
		clientConnection.send(move);
		gameLog.getItems().add("Move: " + fromRow + "," + fromCol + " → " + row + "," + col);
		gameLog.scrollTo(gameLog.getItems().size() - 1);
		fromRow = -1;
		fromCol = -1;
	}

	public void updateBoard(String[][] boardState) {
		if (boardState == null) return;
		for (int r = 0; r < 8; r++) {
			for (int c = 0; c < 8; c++) {
				String val = boardState[r][c];
				Button cell = cells[r][c];

				// default
				if (val == null || val.isEmpty()) {
					cell.setText("");
					cell.setStyle("-fx-font-size: 18;" + "-fx-background-color: " + ((r + c) % 2 == 0 ? "#EEE" : "#444"));
					continue;
				}

				// render pieces
				switch (val) {
					case "R":
						cell.setText("🔴");
						cell.setStyle("-fx-font-size: 18;" + "-fx-background-color: " + ((r + c) % 2 == 0 ? "#EEE" : "#444") + ";" + "-fx-text-fill: red;");
						break;
					case "B":
						cell.setText("⚫");
						cell.setStyle("-fx-font-size: 18;" + "-fx-background-color: " + ((r + c) % 2 == 0 ? "#EEE" : "#444") + ";" + "-fx-text-fill: black;"
						);
						break;
					case "RK":
						cell.setText("K🔴");
						cell.setStyle("-fx-font-size: 18;" + "-fx-background-color: " + ((r + c) % 2 == 0 ? "#EEE" : "#444") + ";" + "-fx-text-fill: red;");
						break;
					case "BK":
						cell.setText("K⚫");
						cell.setStyle("-fx-font-size: 18;" + "-fx-background-color: " + ((r + c) % 2 == 0 ? "#EEE" : "#444") + ";" + "-fx-text-fill: white;");
						break;
					default:
						cell.setText("");
						break;
				}
			}
		}
	}
}