package gravatarfrenzy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class GravatarFrenzy extends Application {

    private double x, y = 0;
    private static Client client;
    private static final Pane ROOT = new Pane();
    private final Character PLAYER = new Character();
    private static final Properties prop = new Properties();
    private static final Map<String, ImageView> PLAYER_MAP = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {        
        primaryStage.setTitle("Gravatar Frenzy");

        PLAYER.hash = MD5Util.md5Hex(request_email()); //The players gravatar email hash.
        ImageView character = renderImage(PLAYER.hash);

        character.addEventHandler(MouseEvent.MOUSE_PRESSED, (MouseEvent event) -> {
            x = event.getX();
            y = event.getY();
            event.consume();
        });
        character.addEventHandler(MouseEvent.MOUSE_DRAGGED, (MouseEvent event) -> {
            PLAYER.x = event.getSceneX() - x;
            PLAYER.y = event.getSceneY() - y;
            client.sendTCP(PLAYER);
            event.consume();
        });
        ROOT.getChildren().add(character);
        PLAYER_MAP.put(PLAYER.hash, character);

        initClient();

        Scene scene = new Scene(ROOT, 900, 500);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        if (client.isConnected()) {
            PLAYER.isConnected = false;
            client.sendTCP(PLAYER);
        }
    }

    public static void initClient() {
        client = new Client();
        Kryo kryo = client.getKryo();
        kryo.register(Character.class);

        client.start();

        try {
            client.connect(5000, prop.getProperty("server"), Integer.parseInt(prop.getProperty("port")));
        } catch (IOException ex) {
            Logger.getLogger(GravatarFrenzy.class.getName()).log(Level.SEVERE, null, ex);
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Connection Failed");
                alert.setHeaderText("Connection Failed");
                alert.setContentText("Ooops, unable to connect to the server "
                        + "on " + prop.getProperty("server") + ":" + prop.getProperty("port"));
                alert.showAndWait();
                System.exit(0);
            });
        }

        client.addListener(new Listener() {
            @Override
            public void received(Connection cnctn, Object o) {
                if (o instanceof Character) {
                    Character character = (Character) o;

                    if (PLAYER_MAP.containsKey(character.hash)) {
                        ImageView aPlayer = PLAYER_MAP.get(character.hash);
                        if (character.isConnected) {
                            Platform.runLater(() -> {
                                aPlayer.setLayoutX(character.x);
                                aPlayer.setLayoutY(character.y);
                            });
                        } else {
                            Platform.runLater(() -> {
                                ROOT.getChildren().remove(aPlayer);
                                PLAYER_MAP.remove(character.hash);
                            });
                        }
                    } else {
                        ImageView aNewPlayer = renderImage(character.hash);
                        PLAYER_MAP.put(character.hash, aNewPlayer);
                        Platform.runLater(() -> {
                            ROOT.getChildren().add(aNewPlayer);
                        });
                        aNewPlayer.setLayoutX(character.x);
                        aNewPlayer.setLayoutY(character.y);
                    }
                }
            }

            @Override
            public void disconnected(Connection cnctn) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Connection Ended");
                    alert.setHeaderText("Connection Ended");
                    alert.setContentText("Ooops, the connection to the server "
                            + "( " + prop.getProperty("server") + " ) has ended.");
                    alert.showAndWait();
                    System.exit(0);
                });
            }

            @Override
            public void connected(Connection cnctn) {
                System.out.println("Connection Established.");
            }

        });
    }

    private static String request_email() {

        TextInputDialog dialog = new TextInputDialog("itsme@example.com");
        dialog.setTitle("Gravatar Email Address");
        dialog.setHeaderText("Gravatar Email Address");
        dialog.setContentText("Please enter your email address:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            return result.get();
        } else {
            System.exit(0);
            return null;
        }
    }

    public static ImageView renderImage(String hash) {
        String path = "http://www.gravatar.com/avatar/" + hash + "?d=retro";
        return new ImageView(new Image(path));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            prop.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            prop.setProperty("server", "0.0.0.0");
            prop.setProperty("port", "7860");
            try {
                prop.store(new FileOutputStream("config.properties"), null);
            } catch (IOException ex) {
                Logger.getLogger(GravatarFrenzy.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        launch(args);
    }

}
