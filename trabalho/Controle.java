import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.*;

import tste.ControleSala;

import java.util.*;

public class Controle extends ReceiverAdapter implements RequestHandler {

    JChannel canalDeComunicacao;
    MessageDispatcher  despachante;

    //usuarios online
    Vector<Address> usuariosOnline = new Vector<Address>();
    Vector<ControleSala> controleSala = new Vector<ControleSala>();
    
    public static void main(String[] args) throws Exception {
        new Controle().start();
    }

    private void start() throws Exception
    {
        //Cria o canal de comunicação com uma configuração padrão do JGroups
	    canalDeComunicacao=new JChannel();    
	    
        canalDeComunicacao.setReceiver(this);
        despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);  
	    	    
	     canalDeComunicacao.connect("XxXControle");
	
	        eventLoop();
	     canalDeComunicacao.close();	    
    }

    private void eventLoop()
    {  	
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
         
    	while(true)
    	{  	    		
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

        despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);
        RspList respList = despachante.castMessage(null, mensagem, opcoes); //MULTICAST
        return respList;
    }
    
    private RspList enviaMulticastFirst(Protocolo conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo.getConteudo());

        Address cluster = null; //endereço null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_FIRST); // espera receber a resposta de TODOS membros (ALL, MAJORITY, FIRST, NONE)
          opcoes.setAnycasting(false);

        RspList respList = despachante.castMessage(null, mensagem, opcoes); //MULTICAST
        return respList;
    }
    
    private void enviaMulticastnNone(Protocolo conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo.getConteudo());

        Address cluster = null; //endereço null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_NONE); // espera receber a resposta de TODOS membros (ALL, MAJORITY, FIRST, NONE)
          opcoes.setAnycasting(false);
          
        despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);
        despachante.castMessage(null, mensagem, opcoes); //MULTICAST
    }

    private RspList enviaAnycast(Collection<Address> grupo, String conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo);

        Message mensagem=new Message(null, "{ ANYCAST } " + conteudo); //apesar do endereço ser null, se as opcoes contiverem anycasting==true enviará somente aos destinos listados

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_MAJORITY); // espera receber a resposta da maioria do grupo (ALL, MAJORITY, FIRST, NONE)
          opcoes.setAnycasting(true);
        
        despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);   
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

       // despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);
        despachante.castMessage(grupo, mensagem, opcoes); //ANYCAST
    }

        private String enviaUnicast(Address destino, Protocolo conteudo) throws Exception{
            System.out.println("\nENVIEI a pergunta: " + conteudo.getConteudo());

            Message mensagem=new Message(destino,conteudo);

            RequestOptions opcoes = new RequestOptions(); 
              opcoes.setMode(ResponseMode.GET_FIRST); // não espera receber a resposta do destino (ALL, MAJORITY, FIRST, NONE)

            String resp = despachante.sendMessage(mensagem, opcoes); //UNICAST

            return resp;
        } 
    
    private void enviaUnicastNone(Address destino, Protocolo conteudo) throws Exception{
        System.out.println("\nEnviei: " + conteudo.getConteudo());

        Message mensagem=new Message(destino,conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_NONE); // não espera receber a resposta do destino (ALL, MAJORITY, FIRST, NONE)

         // despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);
          despachante.sendMessage(mensagem, opcoes);
    }
    
  //exibe mensagens recebidas
    public void receive(Message msg) {System.out.println("" + msg.getSrc() + ": " + msg.getObject());}

 // responde requisições recebidas de acordo com o tipo.
    public Object handle(Message msg) throws Exception{ 
      Protocolo pergunta = (Protocolo)msg.getObject();
      
	  	//11=Logar usuario =================MODELO===================.
	  	if(pergunta.getTipo()==11)
	  	{
	  	    System.out.println("Logar usuario "+pergunta.getConteudo()+"Senha "+pergunta.getConteudoExtra());    						
	  	    return "y";
	  	}
	  	
	  	//12=Criar sala(item com o leilao)=================MODELO===================.
	  	if(pergunta.getTipo()==12)
	  	{
	  		ControleSala controle= new ControleSala(pergunta.getConteudo(),msg.src().toString());
	  		
	  		for (ControleSala item : controleSala)
	  		{
	      		if(item.getItem().equals(controle.getItem()))
	      		{
	      			return "y";
	      		}   			
			}
	  		
	  		controleSala.add(controle);
		    	System.out.println("Novo leilao "+pergunta.getConteudo()+"Leiloeiro "+msg.src());
		    	
				return "n";
	  	}
	  	
    	//15=Cadastrar ganhador, deve ser repassado para o modelo)=================MODELO===================
    	if(pergunta.getTipo()==15)
    	{
    		int id;    		
    		for (ControleSala item : controleSala)
    		{
        		if(item.getLeiloeiro().equals(msg.src().toString()))
        		{
        			controleSala.remove(item);
        			System.out.println("Encontrou o item");
        			break;
        		}   			
			}
    		  		
	    System.out.println("Ganhador "+pergunta.getConteudo()+" Lance "+pergunta.getConteudoExtra());    						
    	}

    	// 17 - Novo usuario online.=================MODELO===================.
    	if(pergunta.getTipo()==17)
    	{
    		usuariosOnline.add(msg.src());
    	    System.out.println("Novo usuario "+msg.src());    						
    	}
    	
    	// 18 - Pedir historico do leilao=================MODELO===================.
    	if(pergunta.getTipo()==18)
    	{
    	    System.out.println("Pedir historico do leilao."); 
    	    return pedirHistorico();
    	}
    	
        return null;
    }
    
    //pedir historico para o modelo.
    private String pedirHistorico()
    {
	        try {
	       	 
	     	    JChannel canalDeComunicacaoModelo=new JChannel();
	     	    canalDeComunicacaoModelo.connect("Persistencia");
	     	    canalDeComunicacaoModelo.setReceiver(this);
	    	    despachante=new MessageDispatcher(canalDeComunicacaoModelo, null, null, this);  
	  
	    	     Protocolo prot1=new Protocolo();   
	             prot1.setConteudo("Pedir historico");
	             prot1.setResposta(true);
	             prot1.setTipo(12);
	        	    	 
	             String resp= enviaMulticastFirst(prot1).getFirst().toString();
	             
	             System.out.println(resp);

	             canalDeComunicacaoModelo.close();
	             despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);	             
	        
	             return resp;
	             
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        
	        return "Erro";
    }

  public void viewAccepted(View new_view) {
	    //exibe alterações na composição do grupo
        // se entrou ou saiu alguem do Jchannel
        // se o coordenador morrer, deve eleger um novo.
        System.out.println("\t** nova View do cluster: " + new_view);
    }
 
  
}//class
