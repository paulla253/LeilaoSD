/*
 SAIBA MAIS: http://www.jgroups.org/manual/html/user-building-blocks.html#MessageDispatcher
/**/

import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.*;

import java.util.*;

public class TiposDeCast extends ReceiverAdapter implements RequestHandler {

    JChannel canalDeComunicacao;
    MessageDispatcher  despachante;
    final int TAMANHO_MINIMO_CLUSTER = 2;
    boolean CONTINUE=true;

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
        //tamanho do grupo, para começar o leilao.
        while( canalDeComunicacao.getView().size() < TAMANHO_MINIMO_CLUSTER )
          Util.sleep(100); // aguarda os membros se juntarem ao cluster

        criaSalaDeLeilao();

    }//eventLoop

    private void criaSalaDeLeilao(){
        Address meuEndereco = canalDeComunicacao.getAddress();

        Vector<Address> cluster = new Vector<Address>(canalDeComunicacao.getView().getMembers());
        Address primeiroMembro = cluster.elementAt(0);//0 a N

        if( meuEndereco.equals(primeiroMembro) ) {  // somente o primeiro membro envia o teste abaixo

            try {
                //resposta está em teste.
                RspList teste=enviaMulticast( "Quem quer participar do leilao de uma caneta?" ); //envia multicast para todos

                //debug das respostas com ID e valor
                for (int i = 0; i < cluster.size(); i++){
                    System.out.println("ID : "+cluster.elementAt(i)+" Valor:"+teste.getValue(cluster.elementAt(i)));
                }

                //criando grupo para os que responderam y
                Vector<Address> grupo = new Vector<Address>();
                for (int i = 0; i < cluster.size(); i++){

                    if(teste.getValue(cluster.elementAt(i)).equals("y"))
                    {
                        grupo.add(cluster.elementAt(i));
                    }
                }

                enviaAnycastNone( grupo, "O leilao vai comecar, qual o valor inicial?" );

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

                while(true){ Util.sleep(100); }

            }
            catch(Exception e) {
                System.err.println( "ERRO: " + e.toString() );
            }



        } // if primeiro
        else {
            while (canalDeComunicacao.getView().getMembers().contains(primeiroMembro) && CONTINUE) {

                Util.sleep(100); // aguarda o primeiro membro sair do cluster

            }
                System.out.println("Depois de entrar no leilao!");

                Scanner teclado = new Scanner(System.in);
                String line = "";
                line=teclado.nextLine().toLowerCase();

                try {
                    enviaUnicast(primeiroMembro, line);


                }catch(Exception e) {
                        System.err.println( "ERRO: " + e.toString() );
                    }


        }
            System.out.println("\nBye bye...");

    }

    private RspList enviaMulticast(String conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo);

        Address cluster = null; //endereço null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, "{MULTICAST} "+conteudo);

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

        private void enviaAnycastNone(Collection<Address> grupo, String conteudo) throws Exception{
        System.out.println("\nENVIEI a pergunta: " + conteudo);

        Message mensagem=new Message(null, "{ ANYCAST } " + conteudo); //apesar do endereço ser null, se as opcoes contiverem anycasting==true enviará somente aos destinos listados

        RequestOptions opcoes = new RequestOptions();
        opcoes.setMode(ResponseMode.GET_NONE); // espera receber a resposta da maioria do grupo (ALL, MAJORITY, FIRST, NONE)
        opcoes.setAnycasting(true);

        System.out.println("Não precisa de resposta");

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

    public void receive(Message msg) { //exibe mensagens recebidas

//        System.out.println(msg.getSrc() + ": " + msg.getObject());

        System.out.println("" + msg.getSrc() + ": " + msg.getObject());
    }

    public Object handle(Message msg) throws Exception{ // responde requisições recebidas
      String pergunta = (String) msg.getObject();
      System.out.println("RECEBI uma mensagem: " + pergunta+"\n");

        Scanner teclado = new Scanner(System.in);
        String line = "";

        line=teclado.nextLine().toLowerCase();

        CONTINUE=false;

        return line;
    }

    public void viewAccepted(View new_view) { //exibe alterações na composição do grupo
        // se entrou ou saiu alguem do Jchannel
        // se o coordenador morrer, deve eleger um novo.
        System.out.println("\t** nova View do cluster: " + new_view);
    }


}//class
