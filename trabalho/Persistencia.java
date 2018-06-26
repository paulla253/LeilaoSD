import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

import java.util.*;

import java.security.MessageDigest;

/*POR FAZER:
    SINCRONIZAR PERSISTENCIAS
    MODO PERSISTENICA OU MODO INVASOR
*/

public class Persistencia extends ReceiverAdapter implements RequestHandler {

    JChannel canalDeComunicacao;
    MessageDispatcher despachante; 
    Nickname_List nicknames;
    Sala_List salas;

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
        
        // ***********************************************************
        /* SE NÃO FOR A PRIMEIRA A ABRIR, SINCRONIZAR COM AS OUTRAS */
    	
        try {
        	System.out.println("Loading Nicknames..."); 
			getNicknames();
			System.out.println("Loading Salas..."); 
	        getSalas();
		} catch (IOException e) {
			System.out.println("ERRO - Não foi possivel iniciar a Persistencia");
			System.exit(1);
		}   
        System.out.println("Persistencia Operacional!");   

        while(true) {  
            Util.sleep(100);
        }
    }

  /*
   * 10 - Criar novo usuario
   * 11 - Logar com usuario
   * 12 - Criar sala
   * 13 - Registra Log
   * 14 - Restaura Log
   * 15 - Listar Itens/Ganhadores
   * 16 - Registrar Ganhador
  */

    public Object handle(Message msg) throws Exception { // responde requisições recebidas
        Protocolo pergunta = (Protocolo)msg.getObject();
    
        /*if (pergunta.getTipo() == 1) {
            loginNickname(pergunta.getConteudo());                             
        }
        else if (pergunta.getTipo() == 2) {
            //gravar_log();
        }*/
        return(pergunta); //*****************************
    }

    public boolean loginNickname(String nickname, String senha) {        
        if (nicknames.getNicknames().containsKey(nickname)) {
            if (nicknames.getNicknames().get(nickname).equals(sha1(senha))) {
                return(true);
            }
        }
        return(false);
    }
    
    public boolean criarNickname(String nickname, String senha) {        
        if (!(nicknames.getNicknames().containsKey(nickname))) {
        	nicknames.getNicknames().put(nickname, new String(sha1(senha)));
        	setNicknames();
        	return(true);   
        }
        return(false);
    }

    public boolean newSala(int id) {        
        if (salas.getSalas().containsKey(id)) {
            return(false); // ACESSO NEGADO - Sala ja existe
        }
        salas.getSalas().put(id, new String(""));
        setSalas();
        return(true); // ACESSO CONCEDIDO - Sala foi cadastrada;
    }

    public boolean setGanhador(int sala, String ganhador) {
        String string = new String();
        string = ganhador;
        try {
        	salas.getSalas().put(sala, string);
        	setSalas();
        } catch (Exception e) {
			return(false);
		}
        return(true);
    }

    public boolean setLog(int sala, String log) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("log-"+ sala +".txt", true)));
            out.println(log);
            out.close();
        } catch (Exception e) {
            System.out.println("ERRO: setLog()");
            return(false);
        }
        return(true);
    }

    public String getLog(int sala) {
        String log = null;
    	try {
    		Scanner scanner = new Scanner(new File("log-" + sala + ".txt"));
    	    scanner.useDelimiter("\\Z");
    	    log = scanner.next();
    	    scanner.close();
        } catch (Exception e) {
            System.out.println("ERRO: setGet()");
        }
        return(log);
    }

    
//PRIVATE--------------------------------------------------------------------------------------------------
    
    private String sha1(String senha) {
    	StringBuffer sb = new StringBuffer();
    	try {
        	byte[] input = senha.getBytes();
        	MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        	byte[] result = mDigest.digest(input);
        	for (int i = 0; i < result.length; i++) {
            	sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        	}
    	} catch (Exception e) {
        	System.out.println("ERRO: sha1()");
        }
        return(sb.toString());
    }
   
    private void getNicknames() throws IOException {
        ObjectInputStream in = null;         
        try {
            in = new ObjectInputStream(new FileInputStream("nicknames.txt"));
            nicknames = (Nickname_List) in.readObject(); 
            in.close();
        } catch (FileNotFoundException e) {
            nicknames = new Nickname_List();
        } catch (Exception e) {
            System.out.println("ERRO: getNicknames()");
        }
        return;
    }

    private void setNicknames() {
        try {
        	ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("nicknames.txt"));
            nicknames.setVersao(nicknames.getVersao() + 1);
            out.writeObject(nicknames); 
            out.close();  
        } catch (Exception e) {
            System.out.println("ERRO: setNicknames()");
        }
        return;
    }
    
    private void getSalas() throws IOException {
        ObjectInputStream in = null;         
        try {
            in = new ObjectInputStream(new FileInputStream("salas.txt"));
            salas = (Sala_List) in.readObject();
            in.close();
        } catch (FileNotFoundException e) {
            salas = new Sala_List();
        } catch (Exception e) {
            System.out.println("ERRO: getSalas()");
        }
        return;
    }

    private void setSalas() {
        try {
        	ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("salas.txt"));
        	salas.setVersao(salas.getVersao() + 1);
            out.writeObject(salas);
            out.close();
        } catch (Exception e) {
            System.out.println("ERRO: setSalas()");
        }
        return;
    }
    

}