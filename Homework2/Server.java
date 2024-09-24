import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static final int PORT = 8080;
    private static final int MAX_LIVES = 10;
    private static List<String> wordList = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        loadWords();  // Load words from the file
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is listening on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected.");
            new Thread(new ClientHandler(clientSocket)).start();  // Handle each client in a separate thread
        }
    }

    // Load words from a file
    private static void loadWords() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("words.txt"));
        String word;
        while ((word = reader.readLine()) != null) {
            wordList.add(word.trim().toLowerCase());  // Ensure words are lowercase
        }
        reader.close();
    }

    // ClientHandler class to manage the game for each client
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String selectedWord;
        private char[] displayWord;
        private int lives;
        private final Set<Character> guessedLetters = new HashSet<>();

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            boolean playAgain = true;

            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                while (playAgain) {
                    lives = MAX_LIVES;
                    guessedLetters.clear();
                    selectRandomWord();
                    sendGameState();  // Send the initial game state

                    // Main game loop
                    while (lives > 0) {
                        // Read the client's guess
                        String input = in.readLine();
                        if (input == null || input.length() != 1) {
                            sendMessage("Invalid input. Please enter a single letter.");
                            continue;
                        }

                        char guess = input.toLowerCase().charAt(0);
                        if (!Character.isLetter(guess)) {
                            sendMessage("Invalid input. Please enter a letter.");
                            continue;
                        }

                        // Check if the letter has already been guessed
                        if (guessedLetters.contains(guess)) {
                            sendMessage("You already guessed '" + guess + "'. Try a different letter.");
                            sendGameState();  // Resend the current game state
                            continue;
                        }

                        // Add the guess to the set of guessed letters
                        guessedLetters.add(guess);

                        // Check if the guess is correct
                        if (checkGuess(guess)) {
                            sendMessage("'" + guess + "' is correct!");
                        } else {
                            lives--;
                            sendMessage("'" + guess + "' is incorrect! You have " + lives + " lives left.");
                        }

                        // Check if the word has been fully guessed
                        if (isWordGuessed()) {
                            sendMessage("Congratulations! You guessed the word '" + selectedWord + "'!");
                            break;
                        }

                        // Send the updated game state (current word and lives)
                        sendGameState();
                    }

                    // If the player runs out of lives
                    if (lives == 0) {
                        sendMessage("Game over! You ran out of lives. The word was '" + selectedWord + "'.");
                    }

                    // Ask if the player wants to play again after the game ends, without allowing extra input
                    sendMessage("Would you like to play again? (y/n)");

                    // Wait for play again response
                    String playAgainInput = in.readLine();
                    if (playAgainInput == null || playAgainInput.equalsIgnoreCase("n")) {
                        playAgain = false;
                    } else if (playAgainInput.equalsIgnoreCase("y")) {
                        playAgain = true;
                        // Reset the game without re-sending "Would you like to play again?"
                    } else {
                        sendMessage("sorry please select y/n if you would like to play again");
                    }
                }

                clientSocket.close();  // Close the connection when the game ends
            } catch (IOException e) {
                System.out.println("Client disconnected.");
            }
        }

        // Select a random word from the word list
        private void selectRandomWord() {
            Random rand = new Random();
            selectedWord = wordList.get(rand.nextInt(wordList.size()));
            displayWord = new char[selectedWord.length()];
            Arrays.fill(displayWord, '_');  // Initialize display word with underscores
            System.out.println("Selected word: " + selectedWord);  // Debugging: show the selected word in the console
        }

        // Check if the guessed letter is in the word and update the displayWord
        private boolean checkGuess(char guess) {
            boolean correct = false;
            for (int i = 0; i < selectedWord.length(); i++) {
                if (selectedWord.charAt(i) == guess) {
                    displayWord[i] = guess;  // Reveal the guessed letter
                    correct = true;
                }
            }
            return correct;
        }

        // Check if the entire word is guessed
        private boolean isWordGuessed() {
            for (char c : displayWord) {
                if (c == '_') {
                    return false;
                }
            }
            return true;
        }

        // Send the current game state to the client
        private void sendGameState() {
            sendMessage("Current Word: " + getDisplayWord());
            sendMessage("Lives left: " + lives);
        }

        private String getDisplayWord() {
            StringBuilder sb = new StringBuilder();
            for (char c : displayWord) {
                sb.append(c).append(" ");
            }
            return sb.toString().trim();
        }

        // Helper method to send a message to the client
        private void sendMessage(String message) {
            out.println(message);
            out.flush();
        }
    }
}
