package gravatarfrenzy.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class GravatarFrenzyServer {
    private static final Server SERVER = new Server();
    
    public static void main(String[] args) throws java.io.IOException { 
        Kryo kryo = SERVER.getKryo();
        kryo.register(Character.class);
        
        SERVER.start();
        SERVER.bind(7860);
        
        System.out.println("Server is now running!");
        
        SERVER.addListener(new Listener() {            
            @Override
            public void received(Connection cnctn, Object o) {
                if(o instanceof Character) {
                    Character character = (Character)o;
                    System.out.println("[" + cnctn.getID() + "] " + "Received: "
                            + "hash: " + character.hash + ", x:"+character.x+""
                            + ",y:"+character.y);
                    
                    SERVER.sendToAllTCP(character);
                }
            }

            @Override
            public void disconnected(Connection cnctn) {
                System.out.println("[" + cnctn.getID() + "] " + "client disconnected.");
            }

            @Override
            public void connected(Connection cnctn) {
                System.out.println("["+cnctn.getID() +"] " + cnctn.getRemoteAddressTCP() + " client connected.");
            }
            
        });
    }
    
}
