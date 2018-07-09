import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class Visao extends ReceiverAdapter implements RequestHandler {

    JChannel canalDeComunicacao;
    MessageDispatcher  despachante;
	Scanner teclado = new Scanner(System.in);
    
    //tamanho minimo do grupo.
    final int TAMANHO_MINIMO_GRUPO = 1;
    //guardar lance e ganhador
    float NOVO_LANCE=0;
    Address NOVO_GANHADOR=null;
    
    //codigo do leiloeiro
    Address leiloeiro=null;
    
    //olhar se o leilao está acontecendo.
    boolean leilao=false;
    
    //controle do laço do leilao
    boolean leilaoAcontecendo=true;
    
    //grupo do leilao.
    Vector<Address> grupo = new Vector<Address>();
    
    //nome do usuario.
    String nickname="";
    
    //item a ser leiloado   
    String codigo="";
    
    //guardar historico.
    String historico= "[Historico]";
 
    public static void main(String[] args) throws Exception {
        new Visao().start();
    }

    private void start() throws Exception
    {
	    JChannel canalDeComunicacaoControle=new JChannel("teste.xml");	        
	    canalDeComunicacaoControle.connect("XxXControle");
	    canalDeComunicacaoControle.setReceiver(this);
	    despachante=new MessageDispatcher(canalDeComunicacaoControle, null, this, this);
	    
	    nickname=canalDeComunicacaoControle.getName();
	    
        	File nicknameFile = new File("nicknameUser.txt");

	        //olhando a existencia do arquivo.
	        if(!nicknameFile.exists()){
	        	controleMenuUsuario(canalDeComunicacaoControle);
	        }
	        else
	        {
	        	System.out.println("Arquivo existe");
	        	loadNickname();
	        }
	
	     Protocolo prot=new Protocolo();   
         prot.setConteudo("Estou online");
         prot.setResposta(false);
         prot.setTipo(17);
         
         try {
             enviaMulticastNone(prot);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    canalDeComunicacaoControle.close();	    
	    
        //Cria o canal de comunicação com uma configuração padrão do JGroups	    
	    canalDeComunicacao=new JChannel("teste.xml");
	    canalDeComunicacao.setName(nickname);
        canalDeComunicacao.setReceiver(this);
        despachante=new MessageDispatcher(canalDeComunicacao, null, this, this);  
        
        canalDeComunicacao.connect("XxXLeilao");
        
        //recuperar secao.
       // if(nicknameFile.exists()){
       // 	System.out.println("Arquivo existe");
       // 	recuperarSecao();
        //}

           eventLoop();
        canalDeComunicacao.close();
    }

    private void eventLoop()
    {  	
	     Integer op=1;	     
	     while(op!=5)
	     {  
	    	//menu 
	    	op=menu_leilao();
	    	
	        switch (op) {
            case 1:
            	  novoLeilao();
                break;
            case 2:
	        	 	entrarLeilaoNick();
	        	 	//encontrar algum leilao
	        	 	if(leiloeiro!=null)
	        	 	{
	        	 		participarLeilao();
	        	 	}
                break;
            case 3:
            		System.out.println(historicoLeilao());
                break;
            case 4:
            		boolean success = (new File("nicknameUser.txt")).delete();           	
            		if(success)
            		{
            			System.out.println("Deslogado com sucesso.Inicie o aplicativo Novamente");	
            		}
            		else
            			
            			System.out.println("Aconteceu um erro, tente novamente mais tarde");
            		op=5;
            	break;               
	        }	    
	     }
    }
    
    private void controleMenuUsuario(JChannel canalComunicacaoControle)
    {  	
	     Integer op=1;	     
	     while(op!=3)
	     {  
	    	//menu 
	    	op=menu_login();
	    	
	        switch (op) {
            case 1:
            		if(criarUsuario())
            		{
            			op=3;
            			criaArquivoNickname();
            		}
            			
                break;
            case 2:
	        		if(logarUsuario(canalComunicacaoControle))
	        		{
	        			op=3;
	        			criaArquivoNickname();
	        		}
            	
                break;
	        }	    
	     }
    }

    private Integer menu_login()
    {
        System.out.println("Escolha uma Opção:");
        System.out.println("1 - Criar novo usuario");
        System.out.println("2 - Logar com um usuario existe");
        System.out.print("-> ");
        
	    return Integer.parseInt(teclado.nextLine());	
    }
    
    //---------------------------------------------------------------------------------------
    private boolean criarUsuario()
    {
    	boolean certo=false;    	
        System.out.println("Criar novo usuario ... ");

        try {     	
     	    JChannel canalDeComunicacaoControle=new JChannel("teste.xml");
     	    canalDeComunicacaoControle.setName(nickname);
    	    canalDeComunicacaoControle.connect("XxXControle");
    	    canalDeComunicacaoControle.setReceiver(this);
    	    despachante=new MessageDispatcher(canalDeComunicacaoControle, null, null, this);  
  
		     Protocolo prot1=new Protocolo();   
	         //critenciais deveriam está certa.
	         prot1.setResposta(false);
	         prot1.setTipo(10);
	         
			 System.out.println("Digite o nome do usuario");
			 String line = "";  
			 line=teclado.nextLine();		 
		     prot1.setConteudo(line);
		        
			 System.out.println("Digite a senha do usuario");
			 line = "";  
			 line=teclado.nextLine();		 
		     prot1.setConteudoExtra(line);
        	    	 
		     String resp=enviaUnicast(canalDeComunicacaoControle.getView().getMembers().get(0),(prot1));
		     
			 if(resp.contains("y"))
			 {
					System.out.println("Usuario Cadastrado com sucesso.");
					nickname=prot1.getConteudo();
					certo=true;
			 }
			 else
			 {
					System.out.println("Usuario Indisponivel");
			 }

             canalDeComunicacaoControle.close();
             despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);
    	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return certo;  
    }
    
    public void criaArquivoNickname()
    {
      File nicknameFile = new File("nicknameUser.txt");            
      try{
           BufferedWriter auxout = new BufferedWriter(new FileWriter(nicknameFile));
           auxout.append(nickname);
           auxout.close();
      }catch(Exception e){
    	  
      System.out.println();

      } 
    }
    
    private boolean logarUsuario(JChannel canalComunicacaoControle)
    {
    	boolean certo=false;    	
        System.out.println("Logar usuario ... ");

        try {     	
     	    JChannel canalDeComunicacaoControle=new JChannel("teste.xml");
     	    canalDeComunicacaoControle.setName(nickname);
    	    canalDeComunicacaoControle.connect("XxXControle");
    	    canalDeComunicacaoControle.setReceiver(this);
    	    despachante=new MessageDispatcher(canalDeComunicacaoControle, null, null, this);  
  
		     Protocolo prot1=new Protocolo();   
	         //critenciais deveriam está certa.
	         prot1.setResposta(false);
	         prot1.setTipo(11);
	         
			 System.out.println("Digite o nome do usuario");
			 String line = "";  
			 line=teclado.nextLine();		 
		     prot1.setConteudo(line);
		        
			 System.out.println("Digite a senha do usuario");
			 line = "";  
			 line=teclado.nextLine();		 
		     prot1.setConteudoExtra(line);
        	    	 
		     String resp=enviaUnicast(canalDeComunicacaoControle.getView().getMembers().get(0),(prot1));
		     
			 if(resp.contains("y"))
			 {
					System.out.println("Login efetuado com sucesso");
					nickname=prot1.getConteudo();
					certo=true;
			 }
			 else
			 {
					System.out.println("Erro.Por favor tente mais tarde.");
			 }

             canalDeComunicacaoControle.close();
             despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);
             
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return certo;  
    }
    
    //menu criado.
    private Integer menu_leilao()
    {    
        System.out.println("Escolha uma Opção: \n "
        		+ "1 - Criar um novo leilao \n "
        		+ "2 - Entrar para o leilao \n "
        		+ "3 - Mostrar historico do leilao \n"
        		+ "4 - Deslogar usuario \n"
        		+ "5 - Sair \n"
        		+ "-> ");    
	    return Integer.parseInt(teclado.nextLine());	
    }
    
    private String historicoLeilao()
    {
    	String resp="";
        try {
       	 
     	    JChannel canalDeComunicacaoControle=new JChannel("teste.xml");
     	    canalDeComunicacaoControle.setName(nickname);
    	    canalDeComunicacaoControle.connect("XxXControle");
    	    canalDeComunicacaoControle.setReceiver(this);
    	    despachante=new MessageDispatcher(canalDeComunicacaoControle, null, null, this);  
  
    	     Protocolo prot1=new Protocolo();   
             //prot1.setConteudo(); nao precisa colocar o texto.
             prot1.setResposta(false);
             prot1.setTipo(15);
             
             resp= enviaUnicast(canalDeComunicacaoControle.getView().getMembers().get(0), prot1);
             
             canalDeComunicacaoControle.close();
             despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);
             
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	
        return resp;
    }
    
    private void loadNickname()
    { 	
        File nicknameFile = new File("nicknameUser.txt");        
        try{
                  FileReader arq = new FileReader(nicknameFile);
                  BufferedReader lerArq = new BufferedReader(arq);
                  nickname = lerArq.readLine();
                  arq.close();
          }catch(Exception e){
        	
        	  System.out.println("Nao foi possivel, inicializar com o nickname. Tente mais tarde.");       	  
          }
   }       
    
    //Leiloeiro->Controle cadastrar item.
    private boolean cadastrarItem()
    {
	        try {
	        	
	   	     	System.out.println("Digite o codigo do item");
	   		    String line = "";  
	   	  		line=teclado.nextLine();
	       	 
	     	    JChannel canalDeComunicacaoControle=new JChannel("teste.xml");
	     	    canalDeComunicacaoControle.setName(nickname);
	    	    canalDeComunicacaoControle.connect("XxXControle");
	    	    canalDeComunicacaoControle.setReceiver(this);
	    	    despachante=new MessageDispatcher(canalDeComunicacaoControle, null, null, this);  
	  
	    	     Protocolo prot1=new Protocolo();   
	             prot1.setConteudo(line);
	             prot1.setResposta(true);
	             prot1.setTipo(12);
	        	    	 
	             String resp= enviaUnicast(canalDeComunicacaoControle.getView().getMembers().get(0),prot1).toString();

	             canalDeComunicacaoControle.close();
	             despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);	             
	             
				if(resp.contains("y"))
				{
					codigo=line;
					return true;					
				}
	             
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        
	        return false;
    }
    
    //Leiloeiro->Controle para RegistrarLog.
    private void registrarLog(String ganhador,String lance)
    {
	        try {	        	 
	     	    JChannel canalDeComunicacaoControle=new JChannel("teste.xml");
	     	    canalDeComunicacaoControle.setName(nickname);
	    	    canalDeComunicacaoControle.connect("XxXControle");
	    	    canalDeComunicacaoControle.setReceiver(this);
	    	    despachante=new MessageDispatcher(canalDeComunicacaoControle, null, null, this);  
	  
	    	     Protocolo prot1=new Protocolo();   
	    	     prot1.setConteudo(codigo);
	    	     prot1.setConteudoExtra("Ganhador "+ganhador+" Lance"+lance);
	             prot1.setTipo(13);
	        	    	 
	             enviaMulticastNone(prot1);

	             canalDeComunicacaoControle.close();
	             despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);	             
	             
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    }
    
    private void cadastrarGanhadorLeilao(Address ganhador, float lance)
    {
        try {       	
     	    JChannel canalDeComunicacaoControle=new JChannel("teste.xml");
     	    canalDeComunicacaoControle.setName(nickname);
    	    canalDeComunicacaoControle.connect("XxXControle");
    	    canalDeComunicacaoControle.setReceiver(this);
    	    despachante=new MessageDispatcher(canalDeComunicacaoControle, null, null, this);  
  
    	     Protocolo prot1=new Protocolo();   
             prot1.setConteudo(ganhador.toString());
             prot1.setConteudoExtra(Float.toString(lance));;
             prot1.setResposta(false);
             prot1.setTipo(16);
        	    	 
             enviaMulticastNone(prot1);
             
             canalDeComunicacaoControle.close();
             despachante=new MessageDispatcher(canalDeComunicacao, null, null, this);
             
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    }
    
    //criando nova sala de Leilao.
    private void novoLeilao()
    {  
    	if(cadastrarItem())
    	{    	
	    	leilao=true;	    	
	    	System.out.println("Esperando usuarios entrarem...");   
	    	
	        while( grupo.size() < TAMANHO_MINIMO_GRUPO )
		        Util.sleep(100);
	       
	            	Protocolo prot = new Protocolo();
	            	//valor inicial.
	                float lance=0;
	                Address ganhador=null;
	                int cont=0;                
	                //leilão acontencendo.
	                boolean flag=true;             
	                while(flag){ 
	                	
	                	//Esperar chegar novos lances
	                	Util.sleep(10000);          
	
		                if(NOVO_LANCE>lance)
		                {
		                		lance=NOVO_LANCE;
		                		ganhador=NOVO_GANHADOR;
		                		
		                		historico=historico+"\n Ganhador"+ganhador+" Lance : "+Float.toString(lance);
		                		registrarLog(ganhador.toString(),Float.toString(lance));
		                		
		                        prot.setConteudo("Valor atual "+lance+"com o usuario"+ganhador);
		                        prot.setResposta(false);
		                        prot.setTipo(0);
		                        try {
									enviaAnycastNone(grupo,prot);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
			                	cont=0;
		                }
		                //depois de um tempo, não aconteceu nenhum lance.
		                else {
		                		                	
		                	cont++;	                	
		                    prot.setConteudo("o leilao vai acabar em "+cont);
		                    prot.setResposta(false);
		                    prot.setTipo(0);
		                    try {
								enviaAnycastNone(grupo,prot);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		                    
		                	if(cont==3)
		                	{
		                		flag=false;	                		
		                	}
		                }  
	                }
	                
	                cadastrarGanhadorLeilao(ganhador,lance);
	                
	                prot.setConteudo("Ganhador "+ganhador+"Leilao ganho com valor: "+lance);
	                prot.setResposta(false);
	                prot.setTipo(5); 
	                try {
						enviaMulticastNone(prot);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    	}
    	else
    		System.out.println("Esse item esta sendo leiloado");	                
    }

    //recuperar secao quando cair. 
    private void recuperarSecao()
    {   	
    				System.out.println("Recuperar secao : ");
					Protocolo prot=new Protocolo();
			        prot.setResposta(false);
			        prot.setConteudo("Participo de algum leilao?");
			        prot.setTipo(6); 
					
					try {
						//composicao do JChannel.
						Vector<Address> cluster = new Vector<Address>(canalDeComunicacao.getView().getMembers());
						
						String resp = enviaMulticastFirst(prot).toString();
						
						System.out.println(resp);

                }catch(Exception e) {
                        System.err.println( "ERRO: " + e.toString() );
                    }
    }          
    
    //participar do leilao. 
    private void participarLeilao(){
    	
    			boolean flag=true;
    			while(flag)
    			{	   	
					Protocolo prot=new Protocolo();
			        prot.setResposta(true);
			        prot.setConteudo("Entrar para o leilao");
			        prot.setTipo(3); 
					
					try {
						String resp = enviaUnicast(leiloeiro,prot);
						
						System.out.println(resp);
						
						if(resp.contains("y"))
						{
							//Conseguiu entrar no leilao.
							break;						
						}
						
						entrarLeilaoNick();
						System.out.println("Não foi possivel entrar na sala.");					
						
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
    			}
		               
            System.out.println("-----Leilão-----");
            while(leilaoAcontecendo)
            {
            	//sair ao terminar o leilao,para não esperar um lance.
            	if(leilaoAcontecendo==false)
            	{
            		break;
            	}
            	
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
    
    private void enviaMulticastNone(Protocolo conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo.getConteudo());

        Address cluster = null; //endereço null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, conteudo);

        RequestOptions opcoes = new RequestOptions();
          opcoes.setMode(ResponseMode.GET_NONE); // espera receber a resposta de TODOS membros (ALL, MAJORITY, FIRST, NONE)
          opcoes.setAnycasting(false);
          
        despachante.castMessage(null, mensagem, opcoes); //MULTICAST
    }
        private void enviaAnycastNone(Collection<Address> grupo, Protocolo conteudo) throws Exception{
        System.out.println("\nENVIEI o lance: " + conteudo.getConteudo());

        Message mensagem=new Message(null,conteudo); //apesar do endereço ser null, se as opcoes contiverem anycasting==true enviará somente aos destinos listados

        RequestOptions opcoes = new RequestOptions();
        opcoes.setMode(ResponseMode.GET_NONE); // espera receber a resposta da maioria do grupo (ALL, MAJORITY, FIRST, NONE)
        opcoes.setAnycasting(true);
        
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

          despachante.sendMessage(mensagem, opcoes);
    }
    
  //exibe mensagens recebidas
    public void receive(Message msg) {System.out.println("" + msg.getSrc() + ": " + msg.getObject());}

    public Object handle(Message msg) throws Exception{ // responde requisições recebidas
      Protocolo pergunta = (Protocolo)msg.getObject();
    
    	//11-Logar com um usuario já existente,deve responder null
    	//(Controle que tem conexão com o modelo,ficará responsavel por essa parte).
    	if (pergunta.getTipo()==11 && pergunta.getTipo()==12 && pergunta.getTipo()==15 && pergunta.getTipo()==16 && pergunta.getTipo()==30)
    	{
    		Util.sleep(1000);
    		return null;     		
    	}
    	
    	//Olhar se o msg.src pertence a algum grupo.
    	if (pergunta.getTipo()==6)
    	{
    			System.out.println(msg.src());
    			System.out.println(grupo.get(0));
    			
    			for (Address address : grupo)
    			{
            		if(address.toString().equals(msg.src().toString()))
            		{
            				System.out.println("Encontrei historico");
            				System.out.println(historico);
            				return historico;
            		}	
				}
    		Util.sleep(1000);
    		return "Nao posso historico.";     		
    	}
      
    	//Diferenciar lance de outras mensagens,lance==1
    	if(pergunta.getTipo()==1)
    	{
    	      System.out.println("Lance : " + pergunta.getConteudo()+"\n");    						
    	}
    	else
    	{
    		System.out.println("Recebi uma msg : " + pergunta.getConteudo()+"\n");
    	}
    	
	    String line = "";  	    
  		Protocolo prot=new Protocolo();
  		
      	//pedir para entrar no leilao
      	if(pergunta.getTipo()==3)
      	{      		
      		if(leilao) {
          		grupo.add(msg.getSrc());
          		System.out.println("Adicionou"+msg.getSrc());
      			return "y";
      		}
      		
      		return "n";
      	}
      	
      	if(pergunta.getTipo()==5)
      	{
      		leilaoAcontecendo=false;     		
      	}
      
  		//Quando precisa de resposta, para enviar
      	if(pergunta.getResposta())
      	{
      		line=teclado.nextLine();
            prot.setConteudo(line);
            prot.setResposta(false);   
      	}
      	
      	//atualizar a variavel novo_lance.
      	if(pergunta.getTipo()==1)
      	{
        	if(Float.valueOf(pergunta.getConteudo()).floatValue()>NOVO_LANCE)
        	{
          		NOVO_LANCE=Float.valueOf(pergunta.getConteudo()).floatValue();
          		NOVO_GANHADOR=msg.getSrc();
        	}
      	}
      	//pedido de leilao
      	if (pergunta.getTipo()==2)
      	{
      		leiloeiro=msg.getSrc();     		
      	}
  		
        return prot;
    }   

  public void viewAccepted(View new_view) {
	    //exibe alterações na composição do grupo
        // se entrou ou saiu alguem do Jchannel
        // se o coordenador morrer, deve eleger um novo.
        System.out.println("\t** nova View do cluster: " + new_view);
    }
  
  private void entrarLeilaoNick()
  {	    
  	//deve listar as listas de leilao disponivel, e o usuário digita o codigo da sala.
  	
	    System.out.println("Digite o nome do usuario do leiloeiro da sala: ");
  	
	    String line=teclado.nextLine(); 
	    
	    Vector<Address> cluster = new Vector<Address>(canalDeComunicacao.getView().getMembers());
	    
      for (int i = 0; i < cluster.size(); i++)
      {	       	
      	//procurando o endereço para mandar
      	if((cluster.elementAt(i).toString()).equals(line))
      	{
      		System.out.println("Encontrei");
      		leiloeiro=cluster.elementAt(i);       		
      		break;
      	}      	
      }
      
      if(leiloeiro==null)
      {
      	System.out.println("Não foi encontrado esse usuário");        	
      }
  }
  
}//class
