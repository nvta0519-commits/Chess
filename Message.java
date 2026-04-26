import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;
    private String type;
    private String sender;
    private String receiver;
    private String content;
    private List<String> users;
    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;
    private String gameId;
    private String[][] board;
    private List<String> games;
    String color;

    public Message() {
        users = new ArrayList<>();
        games = new ArrayList<>();
    }

    public Message(String type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.users = new ArrayList<>();
    }

    public Message(String type, String sender, String receiver, String content, String gameId) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.gameId = gameId;
        this.users = new ArrayList<>();
    }

    // Getters
    public String getType() {return type;}
    public String getSender() {return sender;}
    public String getReceiver() {return receiver;}
    public String getContent() {return content;}
    public List<String> getUsers() {return users;}
    public int getFromRow() { return fromRow; }
    public int getFromCol() { return fromCol; }
    public int getToRow() { return toRow; }
    public int getToCol() { return toCol; }
    public String getGameId() { return gameId; }
    public String[][] getBoard() {return board;}
    public List<String> getGames() {return games;}
    public String getColor() { return color; }

    // Setters
    public void setType(String type) {this.type = type;}
    public void setSender(String sender) {this.sender = sender;}
    public void setReceiver(String receiver) {this.receiver = receiver;}
    public void setContent(String content) {this.content = content;}
    public void setUsers(List<String> users) {this.users = users;}
    public void addUser(String user) {users.add(user);}
    public void setFromRow(int fromRow) { this.fromRow = fromRow; }
    public void setFromCol(int fromCol) { this.fromCol = fromCol; }
    public void setToRow(int toRow) { this.toRow = toRow; }
    public void setToCol(int toCol) { this.toCol = toCol; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public void setBoard(String[][] board) {this.board = board;}
    public void setGames(List<String> games) {this.games = games;}
    public void setColor(String color) { this.color = color; }

    @Override
    public String toString() {
        return "[" + type + "] From: " + sender + ". To: " + receiver + ". Message: " + content;
    }
}