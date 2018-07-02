import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.*;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Map;

public class Persistencia extends ReceiverAdapter implements RequestHandler, Serializable {

    JChannel canalDeComunicacao;
    MessageDispatcher despachante; 
    State estado;
    boolean sincronizando=false;
    

    public static void main(String[] args) throws Exception {
        new Persistencia().start();
    }

    private void start() throws Exception {
        canalDeComunicacao = new JChannel("teste.xml");
        canalDeComunicacao.setReceiver(this);
        
        despachante = new MessageDispatcher(canalDeComunicacao, null, this, this);

        canalDeComunicacao.connect("XxXPersistencia");
        	eventLoop();
        canalDeComunicacao.close();
    }

    private void eventLoop() throws Exception {   
    	
    	try {
        	Protocolo prot=new Protocolo();
            prot.setConteudo("teste");
            prot.setResposta(false);  
            prot.setTipo(1);  
        	
            enviaMulticast(prot);

        }catch(Exception e) {
                System.err.println( "ERRO: " + e.toString() );
            }
    	
    	sincronizando=true;

    	estado = new State();
        System.out.println(canalDeComunicacao.getView().getMembers().toString());
    	if (canalDeComunicacao.getView().getMembers().size() > 1) {
    		try {
            	Protocolo protocolo = new Protocolo();  
                protocolo.setTipo(17);  
                estado = (State) enviaUnicast(canalDeComunicacao.getView().getMembers().get(0), protocolo);
                gravaState();
            } catch(Exception e) {
            	System.out.println("ERRO - Não foi possivel iniciar a Persistencia");
    			System.exit(1);
            }
    	}
    	else {
    		try {
            	System.out.println("Loading Nicknames..."); 
    			getNicknames();
    			System.out.println("Loading Salas..."); 
    	        getSalas();
    		} catch (IOException e) {
    			System.out.println("ERRO - Não foi possivel iniciar a Persistencia");
    			System.exit(1);
    		}
    	} 
    	sincronizando=false;
    	System.out.println("Persistencia Funcional!");
        while(true) {  
            Util.sleep(100);
        }
    }
    
    //Comunicação persistenia: 
    //20=Criar novo usuario
    //21=Logar com o usuario
    //22=Criar sala(item com o leilao)
    //25=Listar itens ganhadores.
    //26=Registrar Ganhador.
    //27=Sincronizar Persistencias
    
    public Object handle(Message msg) throws Exception { 
    	Protocolo pergunta = (Protocolo)msg.getObject();
    	
    	while(sincronizando)
    	{
    		Util.sleep(100);
    	}
    
        // 20 = Criar novo usuario 
    	if (pergunta.getTipo() == 20) {
    	    if (criarNickname(pergunta.getConteudo(), pergunta.getConteudoExtra())) {
    	    	System.out.println("Usuario Cadastrado: " + pergunta.getConteudo()); 
    	    	return("y");
    	    }
    	    System.out.println("Usuario Indisponivel: " + pergunta.getConteudo());  
    	    return("n");    	
    	}
    	
        // 21 = Logar com o usuario
    	if (pergunta.getTipo() == 21) {
    		
    		
    		
    		if (loginNickname(pergunta.getConteudo(), pergunta.getConteudoExtra())) {
    	    	System.out.println("Acesso Permitido: " + pergunta.getConteudo()); 
    	    	return("y");
    	    }
    	    System.out.println("Acesso Negado: " + pergunta.getConteudo());  
    	    return("n");    	
    	}
    	
        // 22 = Cria Sala
    	if (pergunta.getTipo() == 22) {
    		if (newSala(Integer.parseInt(pergunta.getConteudo()))) {
    	    	System.out.println("Sala Cadastrada: " + pergunta.getConteudo()); 
    	    	return("y");
    	    }
    	    System.out.println("Sala Indisponivel: " + pergunta.getConteudo());  
    	    return("n");    	
    	}
    	
    	// 25 = Listar itens ganhadores.
    	if (pergunta.getTipo() == 25) {
    		System.out.println("Itens/Ganhadores Enviado");
    		return(getItemGanhador());   						
    	}
    	
    	// 26 = Registrar Ganhador.
    	if (pergunta.getTipo() == 26) {
    		if (setGanhador(Integer.parseInt(pergunta.getConteudo()), pergunta.getConteudoExtra())) {
    			System.out.println("Ganhador Registrado: " + pergunta.getConteudoExtra()); 
    			return("y");
    		}
    	    System.out.println("Ganhador nao Registrado: " + pergunta.getConteudoExtra());  
    	    return("n");
    	}
    	
    	// 17 = Sincronizar Persistencias.
    	if (pergunta.getTipo() == 17) {
    		System.out.println("Sincronizando...");
    		return(estado);
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
    	if(string.isEmpty()) {
    		string = "Nao existe nenhum ganhador!";
    	}
    	return(string);
    }
    
    private void gravaState() {
    	try {
        	ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("salas.txt"));
            out.writeObject(estado.salas);
            out.close();
        } catch (Exception e) {
            System.out.println("ERRO: setSalas()");
        }
    	try {
        	ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("nicknames.txt"));
            out.writeObject(estado.nicknames); 
            out.close();  
        } catch (Exception e) {
            System.out.println("ERRO: setNicknames()");
        }
    	return;
    }
    
    private Object enviaUnicast(Address destino, Protocolo conteudo) throws Exception{
        Message mensagem = new Message(destino, conteudo);
        RequestOptions opcoes = new RequestOptions(); 
        opcoes.setMode(ResponseMode.GET_FIRST);
        Object resp = despachante.sendMessage(mensagem, opcoes);
        return resp;
    } 
    
    private RspList enviaMulticast(Protocolo conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo.getConteudo());

        Address cluster = null; //endere�o null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_ALL); // espera receber a resposta de TODOS membros (ALL, MAJORITY, FIRST, NONE)
          opcoes.setAnycasting(false);

        RspList respList = despachante.castMessage(null, mensagem, opcoes); //MULTICAST
        return respList;
    }

      public void viewAccepted(View new_view) {
            //System.out.println("\t** nova View do cluster: " + new_view);
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

/*
public void getState(OutputStream output) throws Exception {
    synchronized(estado) {
        Util.objectToStream(estado, new DataOutputStream(output));
    }
}

public void setState(InputStream input) throws Exception {
    State state;
    System.out.println("ENTREI AQUI");
    state = (State) Util.objectFromStream(new DataInputStream(input));
    synchronized(estado) {
        estado = state;
    }
}*/
