import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class Persistencia extends ReceiverAdapter implements RequestHandler {

    JChannel canalDeComunicacao;
    MessageDispatcher despachante; 

    public static void main(String[] args) throws Exception {
        new Persistencia().start();
    }

    private void start() throws Exception {
        canalDeComunicacao = new JChannel();
        canalDeComunicacao.setReceiver(this);
        
        despachante = new MessageDispatcher(canalDeComunicacao, null, null, this);

        canalDeComunicacao.connect("Persistencia");
            eventLoop();
        canalDeComunicacao.close();
    }

    private void eventLoop() {       
        while(true) {  
            Util.sleep(100);
        }
    }

    //Protocolo.tipo:
    //1 = Verifica Nickname
    //2 = Gravar Log Leilão
    //

    public Object handle(Message msg) throws Exception { // responde requisições recebidas
        Protocolo pergunta = (Protocolo)msg.getObject();
    
        if (pergunta.getTipo() == 1) {
            loginNickname(pergunta.getConteudo());                             
        }
        else if (pergunta.getTipo() == 2) {
        	//gravar_log();
        }
    }

    public int loginNickname(String nickname) {        
        File nicknameFile = new File("nickname.txt");
        
        try {
            Scanner scanner = new Scanner(nicknameFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.equals(nickname)) { 
                    return(1); // Usuario ja cadastrado, ou seja, ele está tentando voltar pra sua seção anterior
                }
            }
        } catch(FileNotFoundException e) { 
            return(-1); //Erro
        }
        // Usuario não cadastrado, então, seu nickname é gracado no arquivo
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("nickname.txt", true)));
            out.println(nickname);
            out.close();
        } catch (IOException e) {
            return(-1); //Erro
        }
        return(0); // Usuario cadastrado;
    }

    public int gravarLog(int sala, String log) {
    	PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("log-"+ sala +".txt", true)));
        out.println(log);
        out.close();
    }

    /*public String getLog() {
    	String log = null;
    	return(log);
    }*/

}