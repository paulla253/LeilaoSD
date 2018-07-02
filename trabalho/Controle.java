import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.*;
import java.io.Serializable;
import java.util.*;

public class Controle extends ReceiverAdapter implements RequestHandler,Serializable {

    JChannel canalDeComunicacao;
    MessageDispatcher  despachante;
    
    boolean controle=true;
    boolean sincronizando = false;

    //usuarios online
    State_Controle state;
    
    public static void main(String[] args) throws Exception {
        new Controle().start();
    }

    private void start() throws Exception {
        //Cria o canal de comunicação com uma configuração padrão do JGroups
	    canalDeComunicacao=new JChannel("teste.xml");    
	    
        canalDeComunicacao.setReceiver(this);
        despachante=new MessageDispatcher(canalDeComunicacao, null, this, this);  
	    	    
	     canalDeComunicacao.connect("XxXControle");
	
	        eventLoop();
	     canalDeComunicacao.close();	    
    }

    private void eventLoop() {  	
	    sincronizando = true;
    	state = new State_Controle(); 
        System.out.println(canalDeComunicacao.getView().getMembers().toString());
    	if (canalDeComunicacao.getView().getMembers().size() > 1) {
    		try {
            	Protocolo protocolo = new Protocolo();  
                protocolo.setTipo(90);  
                state =(State_Controle) enviaUnicastSincronia(canalDeComunicacao.getView().getMembers().get(0), protocolo);
            } catch(Exception e) {
            	System.out.println("ERRO - Não foi possivel iniciar o Controle");
    			System.exit(1);
            }
    	}
    	sincronizando = false;
    	System.out.println("Controle Funcional!");
    	
    	System.out.println("Funcionarios online: ");
    	for (String item : state.usuariosOnline) {
    		
    		System.out.println(item);
		}

    	 Protocolo prot=new Protocolo();   
         prot.setConteudo("Teste-Controle");
         prot.setResposta(false);
         prot.setTipo(0);
         try {
             enviaMulticastnNone(prot);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
         Address meuEndereco = canalDeComunicacao.getAddress();
         
        // System.out.println(pedirHistorico());
         
    	while(true) {  	    		
            Vector<Address> cluster = new Vector<Address>(canalDeComunicacao.getView().getMembers());
            Address primeiroMembro = cluster.elementAt(0);  //0 a N
            if( meuEndereco.equals(canalDeComunicacao.getView().getMembers().get(0)) ) {
            	System.out.println("Eu sou o primeiro.");
            }
	        Util.sleep(10000);
    	}
    }
    
    private RspList enviaMulticast(Protocolo conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo.getConteudo());

        Address cluster = null; //endereço null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_ALL); // espera receber a resposta de TODOS membros (ALL, MAJORITY, FIRST, NONE)
          opcoes.setAnycasting(false);

        despachante=new MessageDispatcher(canalDeComunicacao, null, this, this);
        RspList respList = despachante.castMessage(null, mensagem, opcoes); //MULTICAST
        return respList;
    }
    
    private RspList enviaMulticastFirst(Protocolo conteudo, MessageDispatcher  despachante1) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo.getConteudo());

        Address cluster = null; //endereço null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_FIRST); // espera receber a resposta de TODOS membros (ALL, MAJORITY, FIRST, NONE)
          opcoes.setAnycasting(false);

        RspList respList = despachante1.castMessage(null, mensagem, opcoes); //MULTICAST
        return respList;
    }
    
    private void enviaMulticastnNone(Protocolo conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo.getConteudo());

        Address cluster = null; //endereço null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_NONE); // espera receber a resposta de TODOS membros (ALL, MAJORITY, FIRST, NONE)
          opcoes.setAnycasting(false);
          
        despachante=new MessageDispatcher(canalDeComunicacao, null, this, this);
        despachante.castMessage(null, mensagem, opcoes); //MULTICAST
    }

    private RspList enviaAnycast(Collection<Address> grupo, String conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo);

        Message mensagem=new Message(null, "{ ANYCAST } " + conteudo); //apesar do endereço ser null, se as opcoes contiverem anycasting==true enviará somente aos destinos listados

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_MAJORITY); // espera receber a resposta da maioria do grupo (ALL, MAJORITY, FIRST, NONE)
          opcoes.setAnycasting(true);
        
        despachante=new MessageDispatcher(canalDeComunicacao, null, this, this);   
        RspList respList = despachante.castMessage(grupo, mensagem, opcoes); //ANYCAST
        System.out.println("==> Respostas do grupo ao ANYCAST:\n" +respList+"\n");

        return respList;
    }

        private void enviaAnycastNone(Collection<Address> grupo, Protocolo conteudo) throws Exception{
        System.out.println("\nENVIEI o lance: " + conteudo.getConteudo());

        Message mensagem=new Message(null,conteudo); //apesar do endereço ser null, se as opcoes contiverem anycasting==true enviará somente aos destinos listados

        RequestOptions opcoes = new RequestOptions();
        opcoes.setMode(ResponseMode.GET_NONE); // espera receber a resposta da maioria do grupo (ALL, MAJORITY, FIRST, NONE)
        opcoes.setAnycasting(true);

       // despachante=new MessageDispatcher(canalDeComunicacao, null, this, this);
        despachante.castMessage(grupo, mensagem, opcoes); //ANYCAST
    }

        private String enviaUnicast(Address destino, Protocolo conteudo,MessageDispatcher  despachante1) throws Exception{
            System.out.println("\nENVIEI a pergunta: " + conteudo.getConteudo());

            Message mensagem=new Message(destino,conteudo);

            RequestOptions opcoes = new RequestOptions(); 
              opcoes.setMode(ResponseMode.GET_FIRST); // não espera receber a resposta do destino (ALL, MAJORITY, FIRST, NONE)

            String resp = despachante1.sendMessage(mensagem, opcoes); //UNICAST

            return resp;
        } 
    
    private void enviaUnicastNone(Address destino, Protocolo conteudo) throws Exception{
        System.out.println("\nEnviei: " + conteudo.getConteudo());

        Message mensagem=new Message(destino,conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_NONE); // não espera receber a resposta do destino (ALL, MAJORITY, FIRST, NONE)

         // despachante=new MessageDispatcher(canalDeComunicacao, null, this, this);
          despachante.sendMessage(mensagem, opcoes);
    }
    
    private Object enviaUnicastSincronia(Address destino, Protocolo conteudo) throws Exception{
        Message mensagem = new Message(destino, conteudo);
        RequestOptions opcoes = new RequestOptions(); 
        opcoes.setMode(ResponseMode.GET_FIRST);
        Object resp = despachante.sendMessage(mensagem, opcoes);
        return resp;
    } 
    
  //exibe mensagens recebidas
    public void receive(Message msg) {System.out.println("" + msg.getSrc() + ": " + msg.getObject());}

 // responde requisições recebidas de acordo com o tipo.
    public Object handle(Message msg) throws Exception{ 
      Protocolo pergunta = (Protocolo)msg.getObject();
      
      while (sincronizando) {
    	  Util.sleep(100);
      }      
	  	//10=Criar Novo Usuario
	  	if(pergunta.getTipo()==10)
	  	{	
	  	    System.out.println("Novo usuario "+pergunta.getConteudo()+"Senha "+pergunta.getConteudoExtra());	  	    	  	    
	  	    pergunta.setTipo(20);
	  	    if(cadastrarUsuarioOUlogar(pergunta))
	  	    	return "y";
	  	    else
	  	    	return "n";
	  	}
      
	  	//11=Logar usuario
	  	if(pergunta.getTipo()==11)
	  	{
	  	    System.out.println("Logar usuario "+pergunta.getConteudo()+"Senha "+pergunta.getConteudoExtra());    						
	  	    pergunta.setTipo(21);
	  	    if(cadastrarUsuarioOUlogar(pergunta))
	  	    	return "y";
	  	    else
	  	    	return "n";
	  	}
	  	
	  	//12=Criar sala(item com o leilao)
	  	if(pergunta.getTipo()==12)
	  	{
	  		ControleSala controle= new ControleSala(pergunta.getConteudo(),msg.src().toString());
	  		if(msg.src().equals(canalDeComunicacao.getView().getMembers().get(0)))
	  		{
	  			//controle primario mandou resposta para si mesmo.
	  			if(canalDeComunicacao.getAddress().equals(msg.src()))
	  			{
	  				return null;	
	  			}
	  			
	  			state.controleSala.add(controle);
			    System.out.println("Novo leilao "+pergunta.getConteudo()+"Leiloeiro "+msg.src());
	  			
	  		}
	  		//se a mensagem recebida nao for do primeiro lembro,devera fazer a acao Controle -> Persistencia
	  		else
	  		{		  		
		  		//olhar se esta acontecendo o leilao do item no momento
		  		for (ControleSala item : state.controleSala)
		  		{
		      		if(item.getItem().equals(controle.getItem()))
		      		{
		      			System.out.println("Controle");
		      			return "n";
		      		}   			
				}
		  		
		  		//olhar com o modelo.
		  		String resp=criaSalaItemLeilao(pergunta.getConteudo());
		  		//existe no modelo
		  		if(resp.contains("y"))
		  		{
		  			System.out.println("Modelo");
			  		//caso não exite no controle, e nem no modelo poderá criar a sala.
		  			state.controleSala.add(controle);
				    System.out.println("Novo leilao "+pergunta.getConteudo()+"Leiloeiro "+msg.src());
				    
				    //registrar nos outros controles.  	 
		            enviaMulticastnNone(pergunta);
	
		  			return "y";
		  		}
		  		
		  		System.out.println(resp);
			    	
					return "n";
	  		}
	  	}
	  	
	  	//13=registrarLog
	  	if(pergunta.getTipo()==13)
	  	{
	  		for (ControleSala sala : state.controleSala) {
	  			
	  			if(pergunta.getConteudo().equals(sala.getItem()))
	  			{
	  				sala.setHistorico("\n"+sala.getHistorico()+pergunta.getConteudoExtra());
	  				System.out.println((sala.getHistorico()));
	  			}
			}
	  	}
	  	
    	// 30 - Pedir historico do leilao que esta acontecendo
    	if(pergunta.getTipo()==30)
    	{
    		System.out.println("Pedir historico do leilao que esta acontecendo");
	  		for (ControleSala sala : state.controleSala) {
	  			
	  			if(pergunta.getConteudo().equals(sala.getItem()))
	  			{
	  				System.out.println(sala.getHistorico());
	  				return sala.getHistorico();
	  			}
			}
	  		
	  		return ("Não achou!");
    	}
	  	
    	// 15 - Pedir item ganhadores
    	if(pergunta.getTipo()==15)
    	{
    	    System.out.println("Pedir item ganhadores."); 
    	    String histo=pedirHistorico();
    	    System.out.println("Historico : "+histo); 
    	    return histo;
    	}
	  	
    	//16=Cadastrar ganhador, deve ser repassado para o modelo)=================MODELO===================
    	if(pergunta.getTipo()==16)
    	{  		
    		for (ControleSala item : state.controleSala)
    		{
        		if(item.getLeiloeiro().equals(msg.src().toString()))
        		{
        			//tentar gravar 3 vezes no modelo.(Ganhador,Lance)
        			for (int i = 0; i < 3; i++) {
        				if(registrarGanhador(pergunta.getConteudo(),pergunta.getConteudoExtra(),item.getItem()))
        				{
        					state.controleSala.remove(item);
                			System.out.println("Registrado Ganhador.");
        					
        					return "y";        					
        				}
					}
        		}   			
			}
    		
    		return ("Ocorreu um erro nesse leilao.Tente novamente");					
    	}
    	
    	// 90 = Sincronizar Controle.
    	if (pergunta.getTipo() == 90) {
    		System.out.println("Sincronizando...");
    		return(state);
    	}

    	// 17 - Novo usuario online.=================MODELO===================.
    	if(pergunta.getTipo()==17)
    	{
    		state.usuariosOnline.add(msg.src().toString());
    	    System.out.println("Novo usuario "+msg.src());    						
    	}
    	
    	// 15 - Pedir item ganhadores
    	if(pergunta.getTipo()==15)
    	{
    	    System.out.println("Pedir item ganhadores."); 
    	    String histo=pedirHistorico();
    	    System.out.println("Historico : "+histo); 
    	    return histo;
    	}
    	
    	Util.sleep(1000);
    	
        return null;
    }
    
    //cadastrarUsuario ou logar usuario.
    private boolean cadastrarUsuarioOUlogar(Protocolo prot1)
    {
    	boolean resp=false;
    	
        try {          	 
     	    JChannel canalDeComunicacaoControle=new JChannel("teste.xml");
    	    canalDeComunicacaoControle.connect("XxXPersistencia");
    	    canalDeComunicacaoControle.setReceiver(this);
    	    MessageDispatcher  despachante0=new MessageDispatcher(canalDeComunicacaoControle, null, null, this);  
        	    	 
            String resposta=enviaMulticastFirst(prot1, despachante0).getFirst().toString();
            
            if(resposta.contains("y"))
            {
                resp= true;
            }
             
             canalDeComunicacaoControle.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        return resp;
    } 
        
    //registrarGanhador.
    private boolean registrarGanhador(String ganhador,String lance,String item)
    {
    	boolean resp=false;   	
        try {          	 
     	    JChannel canalDeComunicacaoControle=new JChannel("teste.xml");
    	    canalDeComunicacaoControle.connect("XxXPersistencia");
    	    canalDeComunicacaoControle.setReceiver(this);
    	    MessageDispatcher  despachante0=new MessageDispatcher(canalDeComunicacaoControle, null, null, this);  
  
    	     Protocolo prot1=new Protocolo();
    	     prot1.setConteudo(item);
             prot1.setConteudoExtra("Ganhador : "+ganhador+" Lance "+ lance);
             prot1.setTipo(26);
        	    	 
            String resposta=enviaMulticastFirst(prot1, despachante0).getFirst().toString();
            
            if(resposta.contains("y"))
            {
                resp= true;
            }
             
             canalDeComunicacaoControle.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        return resp;
    }
    
    //pedir historico para o modelo.
    private String pedirHistorico()
    {
        try {
          	 
     	    JChannel canalDeComunicacaoControle=new JChannel("teste.xml");
    	    canalDeComunicacaoControle.connect("XxXPersistencia");
    	    canalDeComunicacaoControle.setReceiver(this);
    	    MessageDispatcher  despachante0=new MessageDispatcher(canalDeComunicacaoControle, null, null, this);  
  
    	     Protocolo prot1=new Protocolo();   
             prot1.setConteudo("Pedir historico");
             prot1.setTipo(25);
        	    	 
            String resp= enviaMulticastFirst(prot1, despachante0).getFirst().toString();
             
             canalDeComunicacaoControle.close();
             
             return resp;
             
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        return "Erro";
    }
    
    //pedir historico para o modelo.
    private String criaSalaItemLeilao(String item)
    {
        try {
          	 
     	    JChannel canalDeComunicacaoControle=new JChannel("teste.xml");
    	    canalDeComunicacaoControle.connect("XxXPersistencia");
    	    canalDeComunicacaoControle.setReceiver(this);
    	    MessageDispatcher despachante0=new MessageDispatcher(canalDeComunicacaoControle, null, null, this);  
  
    	     Protocolo prot1=new Protocolo();   
             prot1.setConteudo(item);
             prot1.setTipo(22);
        	    	 
            String resp= enviaMulticastFirst(prot1, despachante0).getFirst().toString();
             
             canalDeComunicacaoControle.close();
             
             System.out.println("Respostas"+resp);
             
             return resp;
             
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
				System.out.println("Erro : "+e.getMessage());
				
			
			}
	        
	        return "Erro";
    }

  public void viewAccepted(View new_view) {
	    //exibe alterações na composição do grupo
        // se entrou ou saiu alguem do Jchannel
        // se o coordenador morrer, deve eleger um novo.
        //System.out.println("\t** nova View do cluster: " + new_view);
    }
 
  
}//class
