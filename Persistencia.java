import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.security.MessageDigest;
import java.util.Map;

/*POR FAZER:
    SINCRONIZAR PERSISTENCIAS
    MODO PERSISTENICA OU MODO INVASOR
*/

public class Persistencia extends ReceiverAdapter implements RequestHandler {

    JChannel canalDeComunicacao;
    MessageDispatcher despachante; 
    State estado;

    public static void main(String[] args) throws Exception {
        new Persistencia().start();
    }

    private void start() throws Exception {
        canalDeComunicacao = new JChannel("pilha_persistencia.xml");
        canalDeComunicacao.setReceiver(this);
        
        despachante = new MessageDispatcher(canalDeComunicacao, null, null, this);

        canalDeComunicacao.connect("Persistencia");
        	eventLoop();
        canalDeComunicacao.close();
    }

    private void eventLoop() throws Exception {       
        estado = new State();
        System.out.println(canalDeComunicacao.getView().getMembers().toString());
    	if (canalDeComunicacao.getView().getMembers().size() > 1) {
        	canalDeComunicacao.getState(null, 10000);
        	loginNickname("luiz","123");
    	} 
    	else {
    		try {
            	System.out.println("Loading Nicknames..."); 
    			getNicknames();
    			System.out.println("Loading Salas..."); 
    	        getSalas();
    	        criarNickname("luiz", "123");
    		} catch (IOException e) {
    			System.out.println("ERRO - Não foi possivel iniciar a Persistencia");
    			System.exit(1);
    		}
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
   * 13 - Listar Itens/Ganhadores
   * 14 - Registrar Ganhador
  */
    
    public void getState(OutputStream output) throws Exception {
        synchronized(estado) {
            Util.objectToStream(estado, new DataOutputStream(output));
        }
    }
    
    public void setState(InputStream input) throws Exception {
        State state;
        state = (State) Util.objectFromStream(new DataInputStream(input));
        synchronized(estado) {
            estado = state;
        }
    }
    
    //Comunicação persistenia: 
    //10=Criar novo usuario
    //11=Logar com o usuario
    //12=Criar sala(item com o leilao)
    //15=Listar itens ganhadores.
    //16=Registrar Ganhador.
    
    public Object handle(Message msg) throws Exception { 
        Protocolo pergunta = (Protocolo)msg.getObject();
    
        // 10 = Criar novo usuario 
    	if (pergunta.getTipo() == 10) {
    	    if (criarNickname(pergunta.getConteudo(), pergunta.getConteudoExtra())) {
    	    	System.out.println("Usuario Cadastrado: " + pergunta.getConteudo()); 
    	    	return("y");
    	    }
    	    System.out.println("Usuario Indisponivel: " + pergunta.getConteudo());  
    	    return("n");    	
    	}
    	
        // 11 = Logar com o usuario
    	if (pergunta.getTipo() == 11) {
    		if (loginNickname(pergunta.getConteudo(), pergunta.getConteudoExtra())) {
    	    	System.out.println("Acesso Permitido: " + pergunta.getConteudo()); 
    	    	return("y");
    	    }
    	    System.out.println("Acesso Negado: " + pergunta.getConteudo());  
    	    return("n");    	
    	}
    	
        // 12 = Cria Sala
    	if (pergunta.getTipo() == 12) {
    		if (newSala(Integer.parseInt(pergunta.getConteudo()))) {
    	    	System.out.println("Sala Cadastrada: " + pergunta.getConteudo()); 
    	    	return("y");
    	    }
    	    System.out.println("Sala Indisponivel: " + pergunta.getConteudo());  
    	    return("n");    	
    	}
    	
    	// 15 = Listar itens ganhadores.
    	if (pergunta.getTipo() == 15) {
    		System.out.println("Itens/Ganhadores Enviado");
    		return(getItemGanhador());   						
    	}
    	
    	// 16 = Registrar Ganhador.
    	if (pergunta.getTipo() == 16) {
    		if (setGanhador(Integer.parseInt(pergunta.getConteudo()), pergunta.getConteudoExtra())) {
    			System.out.println("Ganhador Registrado: " + pergunta.getConteudoExtra()); 
    			return("y");
    		}
    	    System.out.println("Ganhador n�o Registrado: " + pergunta.getConteudoExtra());  
    	    return("n");
    	}
    	
        return(null);
    }

    public boolean loginNickname(String nickname, String senha) {        
    	if (estado.nicknames.getNicknames().containsKey(nickname)) {
            if (estado.nicknames.getNicknames().get(nickname).equals(sha1(senha))) {
                return(true);
            }
        }
        return(false);
    }
    
    public boolean criarNickname(String nickname, String senha) {        
        if (!(estado.nicknames.getNicknames().containsKey(nickname))) {
        	estado.nicknames.getNicknames().put(nickname, new String(sha1(senha)));
        	setNicknames();
        	return(true);   
        }
        return(false);
    }

    public boolean newSala(int id) {        
        if (estado.salas.getSalas().containsKey(id)) {
            return(false); // ACESSO NEGADO - Sala ja existe
        }
        estado.salas.getSalas().put(id, new String(""));
        setSalas();
        return(true); // ACESSO CONCEDIDO - Sala foi cadastrada;
    }

    public boolean setGanhador(int sala, String ganhador) {
        String string = new String();
        string = ganhador;
        try {
        	estado.salas.getSalas().put(sala, string);
        	setSalas();
        } catch (Exception e) {
			return(false);
		}
        return(true);
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
            estado.nicknames = (Nickname_List) in.readObject(); 
            in.close();
        } catch (FileNotFoundException e) {
        	estado.nicknames = new Nickname_List();
        } catch (Exception e) {
            System.out.println("ERRO: getNicknames()");
        }
        return;
    }

    private void setNicknames() {
        try {
        	ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("nicknames.txt"));
        	estado.nicknames.setVersao(estado.nicknames.getVersao() + 1);
            out.writeObject(estado.nicknames); 
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
            estado.salas = (Sala_List) in.readObject();
            in.close();
        } catch (FileNotFoundException e) {
        	estado.salas = new Sala_List();
        } catch (Exception e) {
            System.out.println("ERRO: getSalas()");
        }
        return;
    }

    private void setSalas() {
        try {
        	ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("salas.txt"));
        	estado.salas.setVersao(estado.salas.getVersao() + 1);
            out.writeObject(estado.salas);
            out.close();
        } catch (Exception e) {
            System.out.println("ERRO: setSalas()");
        }
        return;
    }
    
    private String getItemGanhador() {
    	String string = new String();
    	for (Map.Entry<Integer, String> entry : estado.salas.getSalas().entrySet()) {
    	    string = string + "Item = " + entry.getKey() + " / Ganhador = " + entry.getValue() + "\n";
    	}
    	return(string);
    }
    
}









/*
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
*/