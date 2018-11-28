package server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nikalsh
 */
public class LobbyThread extends Thread {

    private ClientHandler client;
    private Game currGame = null;
    private List<ClientHandler> clientList;
    private String input = "";

    private static final String NAME_REQUEST = "name";

    //Scene constans
    private static final String LOGIN_SCENE = "login";
    private static final String REGISTER_SCENE = "register";
    private static final String GAME_SCENE = "game";
    private static final String SETTINGS_SCENE = "settings";
    private static final String MAIN_MENU_SCENE = "main";
    private static final String LOBBY_SCENE = "pregame";
    private static final String PRE_GAME_SCENE = "pregame";
    private static final String CATEGORY_SCENE = "category";
//    private static final String RO

    //ACTIONS IN SCENES
    private static final String BACK = "back";
    private static final String ROUND = "runda";

    //MAIN MENU REQUESTS
    private static final String LOBBY_BUTTON = "lobby";
    private static final String LOGIN_BUTTON = "login";
    private static final String REGISTER_BUTTON = "register";
    private static final String SETTINGS_BUTTON = "settings";
    private static final String EXIT_BUTTON = "exit";

    //REGISTER REQUESTS
    private static final String REGISTER_SUBMIT = "registersubmit";

    //LOGIN REQUESTS
    private static final String LOGIN_SUBMIT = "loginsubmit";

    //PREGAME REQUESTS
    private static final String FIND_GAME = "findgame";
    private static final String NEW_ROUND = "round";

//GAME REQUESTS
    private static final String QUESTION = "question";
    private static final String NUMBER_OF_QUESTIONS = "nrofquestions";
    private static final String CATEGORY_PICK = "category";
    private static final String RIGHT = "right";

    private String scene = "";
    private String MAIN_MENU_REQUEST = "";
    private String GAME_REQUEST = "";
    private String LOBBY_REQUEST = "";
    private String REGISTER_REQUEST = "";
    private String FORM_REQUEST = "";
    private String LOGIN_REQUEST = "";
    private String SETTINGS_REQUEST = "";
    private String GAME_ANSWER = "";
    private String CATEGORY_PICKED = "";

    /*
    
   
     */
    private boolean IN_GAME = false;
    private UserHandler userHandler = new UserHandler();
    private ServerProt questionHandler = new ServerProt();

    public LobbyThread(ClientHandler currSock) {
        client = currSock;
        System.out.println(currSock);

    }

    @Override
    public void run() {
        try {

            //get name request and send client name
            if (client.readLine().equals(NAME_REQUEST)) {
                client.println(client.getID());
            }
            scene = MAIN_MENU_SCENE;

            //PROTOCOL SHOULD GO HERE
            MAIN_MENU_LOOP:
            while ((MAIN_MENU_REQUEST = client.readLine()) != null) {

                while (scene.equals(MAIN_MENU_SCENE)) {

                    switch (MAIN_MENU_REQUEST) {
                        case LOBBY_BUTTON:
                            takeClienToLobbyScene(MAIN_MENU_REQUEST);
                            continue MAIN_MENU_LOOP;

                        case REGISTER_BUTTON:
                            takeClientToRegisterScene(MAIN_MENU_REQUEST);
                            continue MAIN_MENU_LOOP;

                        case LOGIN_BUTTON:
                            takeClientToLoginScene(MAIN_MENU_REQUEST);
                            continue MAIN_MENU_LOOP;

                        case SETTINGS_BUTTON:
                            takeClientToSettingScene(MAIN_MENU_REQUEST);
                            continue MAIN_MENU_LOOP;

                        case "exitapp":
                            //SAVE AUTHORIZED USER STATS
                            //GRACEFULLY DISCONNECT
                            break;
                        default:
                            break;
                    }
                }

                //DO NOT ECHO
                client.println("NOT IMPLEMENTED YET");
            }

        } catch (IOException ex) {
            Thread.currentThread().interrupt();
            System.out.println(client + " stopped");

            Logger.getLogger(LobbyThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void takeClienToLobbyScene(String MAIN_MENU_REQUEST) throws IOException {

        scene = LOBBY_SCENE;
        client.println(String.valueOf(client.getID()));

        OUTER:
        while ((LOBBY_REQUEST = client.readLine()) != null) {

            switch (LOBBY_REQUEST) {

                case FIND_GAME:
                    findNextGame(LOBBY_REQUEST);

                    break OUTER;

                case BACK:
                    client.println(BACK);
                    scene = MAIN_MENU_SCENE;
                    break OUTER;

                default:
                    client.println("ACTION NOT RECOGNIZED BY SERVER: " + LOBBY_REQUEST);
                    break;
            }
        }
    }

    public void findNextGame(String LOBBY_REQUEST) throws IOException {

        scene = PRE_GAME_SCENE;

        //Are there any games?
        System.out.println("gameList size: " + Lobby.gameList.size());
        if (!Lobby.gameList.isEmpty()) {

            for (Game game : Lobby.gameList) {
                //Join first existing joinable game
                if (game.isJoinable(client)) {
                    currGame = game;
                    game.add(client);
                    System.out.println(client + " JOINED a game : " + currGame);
                    IN_GAME = true;

//                    client.println(sendGameStats());
                    break;
                }
            }

            //else make a new
            if (!IN_GAME) {
                currGame = Lobby.generateNewGame(client);
                Lobby.gameList.add(currGame);

                //empty scores
                client.println("0@@@0");
                System.out.println(client + " ELSE starting a new game: " + currGame);
            }
        } else {
            //if no games exist, make a new
            currGame = Lobby.generateNewGame(client);
            Lobby.gameList.add(currGame);

            //emtpy scores
            client.println("-@@@-");
            System.out.println(client + " NO GAMES starting a new game: " + currGame);
        }
        takeClientToCategoryScene(LOBBY_REQUEST);
    }

//    public String sendGameStats() {
//        Game.Player ours;
//        Game.Player theirs;
//        if (currGame.getPlayers().get(0).client.equals(client)) {
//            ours = currGame.getPlayers().get(0);
//            theirs = currGame.getPlayers().get(1);
//        } else {
//            ours = currGame.getPlayers().get(1);
//            theirs = currGame.getPlayers().get(0);
//        }
//
////        return ours.score
//    }
    public void takeClientToCategoryScene(String LOBBY_REQUEST) throws IOException {
        scene = CATEGORY_SCENE;

        List<String> categories = questionHandler.get3Categories();
        String categoriesString = categories.get(0) + "@@@" + categories.get(1) + "@@@" + categories.get(2);
        
        client.println(categoriesString);

        
        CATEGORY_PICKED = client.readLine();
        
        nextRound(CATEGORY_PICKED);

    }

    public void nextRound(String CATEGORY_PICKED) throws IOException {
        client.println(CATEGORY_PICKED);
        QuestionObject currQuestion;
        currGame.currPlayer().currRound().generateQuestionsFromCategory(CATEGORY_PICK);

        OUTER:
        while ((GAME_REQUEST = client.readLine()) != null) {

            switch (GAME_REQUEST) {

                case QUESTION:

                    currQuestion = currGame.currPlayer().currRound().getNextQuestion();
                    client.println(questionAndShuffledAnswers(currQuestion));

                    //send right answer
                    if (client.readLine().equals(RIGHT)) {
                        client.println(currQuestion.correctAnswer);
                        String theirAnswer = client.readLine();

                        if (theirAnswer.equals(RIGHT)) {
                            currGame.currPlayer().score();
                            client.println(String.valueOf(currGame.currPlayer().getScore()));
                        }
                    }
                    break;

//                case ROUND_OVER:
//                    client.println(currGame.currRound().currPlayer.score);
//                    break;
                case BACK:
                    client.println(BACK);
                    scene = LOBBY_SCENE;
                    break OUTER;

                default:
                    client.println("ACTION NOT RECOGNIZED BY SERVER: " + GAME_REQUEST);
                    break OUTER;
            }

        }
    }
//    public void takeClientToExistingGame(String PRE_GAME_REQUEST) throws IOException {
//
//        scene = LOBBY_SCENE;
//        client.println(MAIN_MENU_REQUEST);
//
//        OUTER:
//        while ((PRE_GAME_REQUEST = client.readLine()) != null) {
//
//            switch (PRE_GAME_REQUEST) {
//
//                case FIND_GAME:
//
//                    
//                        client.println(PRE_GAME_REQUEST);
//                        findNextGame(LOBBY_REQUEST)
//                 
//                    continue OUTER;
//
//                case BACK:
//                    client.println(BACK);
//                    scene = MAIN_MENU_SCENE;
//                    break OUTER;
//
//                default:
//                    client.println("ACTION NOT RECOGNIZED BY SERVER: " + PRE_GAME_REQUEST);
//                    break;
//            }
//        }
//    }
//    public void takeClientToGameScene(String PRE_GAME_REQUEST) throws IOException {
//        boolean IN_GAME = false;
//        currGame = null;
//        scene = GAME_SCENE;
//
//        client.println(PRE_GAME_REQUEST);
//
//        //Are there any games?
//        System.out.println("gameList size: " + Lobby.gameList.size());
//        if (!Lobby.gameList.isEmpty()) {
//
//            for (Game game : Lobby.gameList) {
//                //Join first existing joinable game
//                if (game.isJoinable(client)) {
//                    currGame = game;
//                    game.add(client);
//                    System.out.println(client + " JOINED a game : " + currGame);
//                    IN_GAME = true;
//                    break;
//                }
//            }
//
//            //else make a new
//            if (!IN_GAME) {
//                currGame = Lobby.generateNewGame(client);
//                Lobby.gameList.add(currGame);
//                System.out.println(client + " ELSE starting a new game: " + currGame);
//            }
//        } else {
//            //if no games exist, make a new
//            currGame = Lobby.generateNewGame(client);
//            Lobby.gameList.add(currGame);
//            System.out.println(client + " NO GAMES starting a new game: " + currGame);
//        }

//        QuestionObject currQuestion;
//
//        OUTER:
//        while ((GAME_REQUEST = client.readLine()) != null) {
//
//            switch (GAME_REQUEST) {
//
//                case QUESTION:
//                    currQuestion = currGame.currPlayer().currRound().getNextQuestion();
//                    client.println(questionAndShuffledAnswers(currQuestion));
//
//                    //send right answer
//                    if (client.readLine().equals(RIGHT)) {
//                        client.println(currQuestion.correctAnswer);
//                        String theirAnswer = client.readLine();
//
//                        if (theirAnswer.equals(RIGHT)) {
//                            currGame.currPlayer().score();
//                            client.println(String.valueOf(currGame.currPlayer().getScore()));
//                        }
//                    }
//                    break;
//
////                case ROUND_OVER:
////                    client.println(currGame.currRound().currPlayer.score);
////                    break;
//                case BACK:
//                    client.println(BACK);
//                    scene = LOBBY_SCENE;
//                    break OUTER;
//
//                default:
//                    client.println("ACTION NOT RECOGNIZED BY SERVER: " + GAME_REQUEST);
//                    break OUTER;
//            }
//
//        }
//    }
    private String questionAndShuffledAnswers(QuestionObject q) {
//        System.out.println(q);
        String theQ = q.question;
        String[] arr = new String[4];
        arr[0] = q.answer2;
        arr[1] = q.answer3;
        arr[2] = q.answer4;
        arr[3] = q.correctAnswer;

        Collections.shuffle(Arrays.asList(arr));
        return String.format("%s@@@%s@@@%s@@@%s@@@%s", theQ, arr[0], arr[1], arr[2], arr[3]);
    }

    public void takeClientToRegisterScene(String MAIN_MENU_REQUEST) throws IOException {
        scene = REGISTER_SCENE;

        client.println(MAIN_MENU_REQUEST);
        OUTER:
        while ((REGISTER_REQUEST = client.readLine()) != null) {

            switch (REGISTER_REQUEST) {

                case REGISTER_SUBMIT:
                    tryToSaveUserToDatabase(REGISTER_SUBMIT);
                    break;

                case BACK:
                    client.println(BACK);
                    scene = MAIN_MENU_SCENE;
                    break OUTER;

                default:
                    client.println("ACTION NOT RECOGNIZED BY SERVER: " + REGISTER_REQUEST);
                    break;
            }
        }
    }

    public void takeClientToLoginScene(String MAIN_MENU_REQUEST) throws IOException {

        scene = LOGIN_SCENE;

        client.println(MAIN_MENU_REQUEST);
        OUTER:
        while ((LOGIN_REQUEST = client.readLine()) != null) {
            switch (LOGIN_REQUEST) {
                case LOGIN_SUBMIT:
                    tryToLoginUser(LOGIN_SUBMIT);
                    break;

                case BACK:
                    client.println(BACK);
                    scene = MAIN_MENU_SCENE;
                    break OUTER;
                default:
                    client.println("ACTION NOT RECOGNIZED BY SERVER: " + LOGIN_REQUEST);
                    break;
            }
        }

    }

    public void takeClientToSettingScene(String MAIN_MENU_REQUEST) throws IOException {
        scene = SETTINGS_SCENE;

        client.println(MAIN_MENU_REQUEST);
        OUTER:
        while ((SETTINGS_REQUEST = client.readLine()) != null) {
            switch (SETTINGS_REQUEST) {

                case BACK:
                    client.println(BACK);
                    scene = MAIN_MENU_SCENE;
                    break OUTER;

                default:
                    client.println("ACTION NOT RECOGNIZED BY SERVER: " + SETTINGS_REQUEST);
                    break;
            }
        }

    }

    public void tryToLoginUser(String LOGIN_SUBMIT) throws FileNotFoundException, IOException {

        client.println("authenticating user credentials..");

        String loginUserWithPass = client.readLine();
        String[] UserPass = loginUserWithPass.split(" ", 2);

        client.println(Boolean.toString(userHandler.login(UserPass[0], UserPass[1])));

    }

    public void tryToSaveUserToDatabase(String INCOMING_FORM) throws IOException {

        client.println("proccessing forms..");

        String theUserToFind = client.readLine();
        client.println(Boolean.toString(userHandler.findUsername(theUserToFind)));

        String validPass = client.readLine();
        client.println(Boolean.toString(userHandler.validatePass(validPass)));

        String validMail = client.readLine();
        client.println(Boolean.toString(userHandler.validateMail(validMail)));

        String theMailToFind = client.readLine();
        client.println(Boolean.toString(userHandler.findMail(theMailToFind)));

        String validFields = client.readLine();
        String[] splitFields = validFields.split(",", 3);

        client.println(Boolean.toString(userHandler.validateFields(splitFields[0], splitFields[1], splitFields[2])));

        String toRegisterFields = client.readLine();
        String[] splitRegister = toRegisterFields.split(",", 3);

        client.println(Boolean.toString(userHandler.register(splitRegister[0], splitRegister[1], splitRegister[2])));
    }

}
