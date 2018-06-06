import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class TiposDeCast extends ReceiverAdapter implements RequestHandler {

    JChannel canalDeComunicacao;
    MessageDispatcher  despachante;
    final int TAMANHO_MINIMO_CLUSTER = 2;
    boolean CONTINUE=true;
    float NOVO_LANCE=0;
    Address leiloeiro=null;
    //grupo do leilao.
    Vector<Address> grupo = new Vector<Address>();
    
    public static void main(String[] args) throws Exception {
        new TiposDeCast().start();
    }

    private void start() throws Exception {

        //Cria o canal de comunicação com uma configuração padrão do JGroups
	    canalDeComunicacao=new JChannel();
        canalDeComunicacao.setReceiver(this);
        
        //carregando o nome do usuario.
        //canalDeComunicacao.setName(loadNickname());

        despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);

        canalDeComunicacao.connect("TiposDeCast");
           eventLoop();
        canalDeComunicacao.close();
    }

    private void eventLoop()
    {
    	//carregar o nome do usuario.
    	//loadNickname();  
    	
	     Integer op=1;	     
	     while(op!=3)
	     {  
	    	//menu 
	    	op=menu();
	    	
	        switch (op) {
            case 1:
    	        //tamanho do grupo, para começar o leilao.
    	        while( canalDeComunicacao.getView().size() < TAMANHO_MINIMO_CLUSTER )
    	          Util.sleep(100);
            	  novoLeilao();
                break;
            case 2:
            		leilao();
                break;
	        
	         case 4:
	        	 enviarMsgPeloNick();
	         break;
	        }	        
	     }
    }
    
    private void enviarMsgPeloNick()
    {	    
	    System.out.println("Digite o nome do usuario do leiloeiro da sala");
    	
	    Scanner teclado = new Scanner(System.in);
	    String line=teclado.nextLine();
	    
	    Address destino=null;
	    
	    
	    Vector<Address> cluster = new Vector<Address>(canalDeComunicacao.getView().getMembers());
	    
        for (int i = 0; i < cluster.size(); i++)
        {	       	
        	//procurando o endereço para mandar
        	if((cluster.elementAt(i).toString()).equals(line))
        	{
        		System.out.println("Encontrei");
        		destino=cluster.elementAt(i);
        	}      	
        }
        
        if(destino==null)
        {
        	System.out.println("Não foi encontrado esse usuário");        	
        }
        else
        {
            try {
            	Protocolo prot=new Protocolo();
                prot.setConteudo("ola");
                prot.setResposta(false);  
                prot.setTipo(3);
                prot.setEndereco(canalDeComunicacao.getAddress());
                
                enviaUnicastNone(destino, prot);
                
                //entrar para sala de leilao.
                leilao();

            }catch(Exception e) {
                    System.err.println( "ERRO: " + e.toString() );
            }
        }
    }
    
    //menu criado.
    private Integer menu()
    {
	    Scanner teclado = new Scanner(System.in);
	    
        System.out.println("Escolha uma Opção:");
        System.out.println("1 - Criar um novo leilao");
        System.out.println("2 - Entrar para o leilao");
        System.out.println("4 - Pedir para entrar em um leilao");
        System.out.println("3 - Sair");
        System.out.print("-> ");
        
	    return Integer.parseInt(teclado.nextLine());	
    }
    
    private String loadNickname(){ 
    	
    	String nickname=null;

        File nicknameFile = new File("nickname.txt");
        
        if(!nicknameFile.exists()){
          System.out.print("Escolha seu nickname: ");
  	    	Scanner teclado = new Scanner(System.in);
	        nickname = teclado.nextLine();
          try{
            BufferedWriter auxout = new BufferedWriter(new FileWriter(nicknameFile));
            auxout.append(nickname);
            auxout.close();
          }catch(Exception e){
        	 //nao mostra o erro. 	
          } 
        }else{
              try{
                  FileReader arq = new FileReader(nicknameFile);
                  BufferedReader lerArq = new BufferedReader(arq);
                  nickname = lerArq.readLine();
                  arq.close();
          }catch(Exception e){
        	//nao mostra o erro. 	
          }
        }
        
        return nickname;
      }
    
    private void novoLeilao()
    {
        Address meuEndereco = canalDeComunicacao.getAddress();

        Vector<Address> cluster = new Vector<Address>(canalDeComunicacao.getView().getMembers());
        //Address primeiroMembro = cluster.elementAt(0);//0 a N

            try {
                Protocolo prot = new Protocolo();
                prot.setConteudo("Quem quer participar do leilao de uma caneta?");
                prot.setResposta(true);
                prot.setEndereco(canalDeComunicacao.getAddress());
                prot.setTipo(2);
                
                RspList teste=enviaMulticast(prot); //envia multicast para todos
                
                //debug das respostas com ID e valor
                for (int i = 0; i < cluster.size(); i++){
                	
                	prot = (Protocolo)teste.getValue(cluster.elementAt(i));
                	
                    System.out.println("ID : "+cluster.elementAt(i)+" Valor:"+prot.getConteudo());
                }

                //cria grupo para os que responderam y
                for (int i = 0; i < cluster.size(); i++){
                	
                	prot = (Protocolo)teste.getValue(cluster.elementAt(i));
                    if(prot.getConteudo().equals("y"))
                    {
                        grupo.add(cluster.elementAt(i));
                    }
                }
        
                float lance=0;
                
                int cont=0;                
                //leilão acontencendo.
                boolean flag=true;             
                while(flag){ 
                	
                	Util.sleep(5000); 
                	
            		//System.out.println("--: "+NOVO_LANCE);
	                if(NOVO_LANCE>0)
	                {
	                	if(NOVO_LANCE>lance)
	                	{
	                		lance=NOVO_LANCE;
	                		
	                        prot.setConteudo("Valor atual"+lance);
	                        prot.setResposta(false);
	                        prot.setTipo(0);
	                        prot.setEndereco(canalDeComunicacao.getAddress());
	                        enviaAnycastNone(grupo,prot);
		                	cont=0;
	                	}
	                	
	                	NOVO_LANCE=0;
	                }
	                //depois de um tempo, não aconteceu nenhum lance.
	                else {
	                		                	
	                	cont++;	                	
	                    prot.setConteudo("o leilao vai acabar em "+cont);
	                    prot.setResposta(false);
	                    prot.setTipo(0);
                        prot.setEndereco(canalDeComunicacao.getAddress());
	                    enviaAnycastNone(grupo,prot);
	                    
	                	if(cont==3)
	                	{
	                		flag=false;	                		
	                	}
	                }  
                }
                
                prot.setConteudo("Leilao ganho com valor: "+lance);
                prot.setResposta(false);
                prot.setEndereco(canalDeComunicacao.getAddress());
                prot.setTipo(0); 
                enviaMulticastnNone(prot);
                
            }
            catch(Exception e) {
                System.err.println( "ERRO: " + e.toString() );
            }	
    }
     
    private void leilao(){
        
        	//segurar até aparecer algum leilão.
            while (CONTINUE) {

                Util.sleep(100);
            }
            
            System.out.println("-----Leilão-----");
            // aguarda o primeiro membro sair do cluster
            Scanner teclado = new Scanner(System.in);
            while(canalDeComunicacao.getView().getMembers().contains(leiloeiro))
            {
            	System.out.print("Lance :");
                String line = "";
                line=teclado.nextLine().toLowerCase();

                try {
                	Protocolo prot=new Protocolo();
                    prot.setConteudo(line);
                    prot.setResposta(false);  
                    prot.setTipo(1);  
                	
                    enviaUnicastNone(leiloeiro, prot);

                }catch(Exception e) {
                        System.err.println( "ERRO: " + e.toString() );
                    }
            }          
        
            System.out.println("\nBye bye...");
    }

    private RspList enviaMulticast(Protocolo conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo.getConteudo());

        Address cluster = null; //endereço null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_ALL); // espera receber a resposta de TODOS membros (ALL, MAJORITY, FIRST, NONE)
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

        despachante.castMessage(null, mensagem, opcoes); //MULTICAST
    }

    private RspList enviaAnycast(Collection<Address> grupo, String conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo);

        Message mensagem=new Message(null, "{ ANYCAST } " + conteudo); //apesar do endereço ser null, se as opcoes contiverem anycasting==true enviará somente aos destinos listados

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_MAJORITY); // espera receber a resposta da maioria do grupo (ALL, MAJORITY, FIRST, NONE)
          opcoes.setAnycasting(true);

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

        despachante.castMessage(grupo, mensagem, opcoes); //ANYCAST
    }

    private String enviaUnicast(Address destino, String conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo);

        Message mensagem=new Message(destino, "Lance:" + conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_FIRST); // não espera receber a resposta do destino (ALL, MAJORITY, FIRST, NONE)

        String resp = despachante.sendMessage(mensagem, opcoes); //UNICAST
        System.out.println("==> Respostas do membro ao UNICAST:\n" +resp+"\n");

        return resp;
    }
    
    private void enviaUnicastNone(Address destino, Protocolo conteudo) throws Exception{
        System.out.println("\nEnviei: " + conteudo.getConteudo());
        System.out.println("\nEnviei: " + destino);

        Message mensagem=new Message(destino,conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_NONE); // não espera receber a resposta do destino (ALL, MAJORITY, FIRST, NONE)

          despachante.sendMessage(mensagem, opcoes);
    }
    
  //exibe mensagens recebidas
    public void receive(Message msg) {System.out.println("" + msg.getSrc() + ": " + msg.getObject());}

    public Object handle(Message msg) throws Exception{ // responde requisições recebidas
      Protocolo pergunta = (Protocolo)msg.getObject();
      
    	//Diferenciar lance de outras mensagens
    	if(pergunta.getTipo()==1)
    	{
    	      System.out.println("Lance : " + pergunta.getConteudo()+"\n");    						
    	}
    	else
    	{
    		System.out.println("Recebi uma msg : " + pergunta.getConteudo()+"\n");
    	}

	    Scanner teclado = new Scanner(System.in);
	    String line = "";      
  		Protocolo prot=new Protocolo();
      
  		//Quando precisa de resposta, para enviar
      	if(pergunta.getResposta())
      	{
      		line=teclado.nextLine();
            prot.setConteudo(line);
            prot.setResposta(false);   
            CONTINUE=false;
      	}
      	
      	//atualizar a variavel novo_lance.
      	if(pergunta.getTipo()==1)
      	{
      		NOVO_LANCE=Float.valueOf(pergunta.getConteudo()).floatValue();      						
      	}
      	//pedido de leilao
      	if (pergunta.getTipo()==2)
      	{
      		leiloeiro=pergunta.getEndereco();
      		//System.out.println(pergunta.getEndereco());       		
      	}
      	//pedir para entrar no leilao
      	if(pergunta.getTipo()==3)
      	{
      		grupo.add(pergunta.getEndereco());
      	}
      		
        return prot;
    }

    public void viewAccepted(View new_view) { //exibe alterações na composição do grupo
        // se entrou ou saiu alguem do Jchannel
        // se o coordenador morrer, deve eleger um novo.
        System.out.println("\t** nova View do cluster: " + new_view);
    }
}//class
