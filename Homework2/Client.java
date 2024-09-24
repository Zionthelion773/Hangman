import java.io.*;
import java.net.*;

public class Client {

    private static final int PORT = 8080;
    private static final String SERVER_ADDRESS = "127.0.0.1";  // Change to server IP if needed

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            boolean playAgain = true;

            while (playAgain) {
                String serverResponse;

                // Display the welcome message at the start of each game
                System.out.println("Welcome to Hangman!");

                // Main game loop
                while ((serverResponse = in.readLine()) != null) {
                    // Display the server's response (game state, guess result)
                    System.out.println(serverResponse);

                    // Break the loop if the game ends (either win or loss)
                    if (serverResponse.contains("Congratulations") || serverResponse.contains("Game over")) {
                        break;
                    }

                    // Prompt the user for a guess
                    String guess = "";
                    while (guess.length() != 1) {
                        System.out.print("Enter a single letter: ");
                        guess = userInput.readLine().trim().toLowerCase();
                        if (guess.length() != 1 || !Character.isLetter(guess.charAt(0))) {
                            System.out.println("Invalid input. Please enter a single letter.");
                            guess = "";  // Reset for valid input
                        }
                    }

                    // Send the guess to the server
                    out.println(guess);
                    out.flush();

                    // Wait for the server to send the guess result (correct or incorrect)
                    serverResponse = in.readLine();
                    System.out.println(serverResponse);

                    // Wait for the server to send the updated game state (current word + lives)
                    serverResponse = in.readLine();
                    System.out.println(serverResponse);
                }

                // Ask if the player wants to play again after the game ends
                boolean validInput = false;
                while (!validInput) {
                    System.out.print("Would you like to play again? (y/n): ");
                    String playAgainInput = userInput.readLine().trim().toLowerCase();
                    if (playAgainInput.equals("n")) {
                        playAgain = false;
                        validInput = true;
                        System.out.println("Thank you for playing!");
                    } else if (playAgainInput.equals("y")) {
                        out.println("play again");  // Notify the server to start a new game
                        validInput = true;
                    } else {
                        System.out.println("sorry please select y/n if you would like to play again");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Connection to the server failed: " + e.getMessage());
        }
    }
}
