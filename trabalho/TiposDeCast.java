/*
 SAIBA MAIS: http://www.jgroups.org/manual/html/user-building-blocks.html#MessageDispatcher
/**/


//import Protocolo;
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
    String nickname="";
    

    public static void main(String[] args) throws Exception {
        new TiposDeCast().start();
    }

    private void start() throws Exception {

        //Cria o canal de comunicação com uma configuração padrão do JGroups
	    canalDeComunicacao=new JChannel();
        canalDeComunicacao.setReceiver(this);

        despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);

        canalDeComunicacao.connect("TiposDeCast");
           eventLoop();
        canalDeComunicacao.close();
    }

    private void eventLoop() {
    	
    	loadNickname();
    	
    	
        //tamanho do grupo, para começar o leilao.
        while( canalDeComunicacao.getView().size() < TAMANHO_MINIMO_CLUSTER )
          Util.sleep(100); // aguarda os membros se juntarem ao cluster
        criaSalaDeLeilao();

    }
    
    private void loadNickname(){ 

        File nicknameFile = new File("nickname.txt");
        
        if(!nicknameFile.exists()){
          System.out.print("Escolha seu nickname: ");
  	    	Scanner teclado = new Scanner(System.in);
	      	System.out.print("Lance :");
          
	        nickname = teclado.nextLine();
	        System.out.println();
          try{
            BufferedWriter auxout = new BufferedWriter(new FileWriter(nicknameFile));
            auxout.append(nickname);
            auxout.close();
          }catch(Exception e){

          } 
        }else{
              try{
                  FileReader arq = new FileReader(nicknameFile);
                  BufferedReader lerArq = new BufferedReader(arq);
                  this.nickname = lerArq.readLine();
                  arq.close();
          }catch(Exception e){

          }
          
        }
      }
    
    
    private void criaSalaDeLeilao(){
        Address meuEndereco = canalDeComunicacao.getAddress();

        Vector<Address> cluster = new Vector<Address>(canalDeComunicacao.getView().getMembers());
        Address primeiroMembro = cluster.elementAt(0);//0 a N

        if( meuEndereco.equals(primeiroMembro) ) {  // somente o primeiro membro envia o teste abaixo

            try {
                Protocolo prot = new Protocolo();
                prot.setConteudo("Quem quer participar do leilao de uma caneta?");
                prot.setResposta(true);
                
                RspList teste=enviaMulticast(prot); //envia multicast para todos
                
                //debug das respostas com ID e valor
                for (int i = 0; i < cluster.size(); i++){
                	
                	prot = (Protocolo)teste.getValue(cluster.elementAt(i));
                	
                    System.out.println("ID : "+cluster.elementAt(i)+" Valor:"+prot.getConteudo());
                }

                //cria grupo para os que responderam y
                Vector<Address> grupo = new Vector<Address>();
                for (int i = 0; i < cluster.size(); i++){

                    if(teste.getValue(cluster.elementAt(i)).equals("y"))
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
	                		System.out.println("Objeto esta com o valor: "+lance);
		                	cont=0;
	                	}
	                	
	                	NOVO_LANCE=0;

	                }
	                //depois de um tempo, não aconteceu nenhum lance.
	                else {
	                	
	                	if(cont==3)
	                	{
	                		flag=false;
	                		
	                	}
	                	
	                	cont++;
	                	
	                    prot.setConteudo("o leilao vai acabar em "+cont);
	                    prot.setResposta(false);
	                    prot.setTipo(0);;
	                    enviaAnycastNone( grupo,prot);
	                }
	                
	                
                }
                
                
                System.out.println("Leilao ganho com valor: "+lance);
                
                //prot.setConteudo("O leilao vai comecar, qual o valor inicial?");
                //prot.setResposta(false);

               //enviaAnycastNone( grupo,prot);

//                float valor=0;
//
//                for (int i = 0; i < grupo.size(); i++){
//
//                    String txt = (String)leilao.getValue(grupo.elementAt(i));
//
//                    float new_valor= Float.parseFloat(txt);
//
//                    if(valor < new_valor)
//                    {
//                        valor=new_valor;
//                    }
//
//                }
//
//                System.out.println("Maior valor de "+valor);

                

            }
            catch(Exception e) {
                System.err.println( "ERRO: " + e.toString() );
            }
        }
        // quem particpa do leilão : 
        else {
            while (CONTINUE) {

                Util.sleep(100);
            }
            
            System.out.println("-----Leilão-----");
            // aguarda o primeiro membro sair do cluster
            Scanner teclado = new Scanner(System.in);
            while(canalDeComunicacao.getView().getMembers().contains(primeiroMembro)) {

            	System.out.print("Lance :");
                String line = "";
                line=teclado.nextLine().toLowerCase();

                try {
                	Protocolo prot=new Protocolo();
                    prot.setConteudo(line);
                    prot.setResposta(false);  
                    prot.setTipo(1);  
                	
                    enviaUnicastNone(primeiroMembro, prot);

                }catch(Exception e) {
                        System.err.println( "ERRO: " + e.toString() );
                    }

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
//        System.out.println("==> Respostas do cluster ao MULTICAST:\n" +respList+"\n");

        return respList;
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

        Message mensagem=new Message(destino,conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_NONE); // não espera receber a resposta do destino (ALL, MAJORITY, FIRST, NONE)

          despachante.sendMessage(mensagem, opcoes);
    }
    
  //exibe mensagens recebidas
    public void receive(Message msg) { 

        System.out.println("" + msg.getSrc() + ": " + msg.getObject());
    }

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
      	
        return prot;
    }

    public void viewAccepted(View new_view) { //exibe alterações na composição do grupo
        // se entrou ou saiu alguem do Jchannel
        // se o coordenador morrer, deve eleger um novo.
        System.out.println("\t** nova View do cluster: " + new_view);
    }


}//class
