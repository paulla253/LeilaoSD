import java.io.Serializable;

public class Protocolo implements Serializable {

    private boolean resposta;
    private String conteudo;
    private String conteudoExtra;
    private int tipo;
    
    //Tipo:
    //0== Informativo.
    //1== é um lance.
    //2== Novo leilao.
    //3== Entrar em um grupo
    //5==Fim de Leilao.
    
    //Comunicação persistenia: 
    //10=Criar novo usuario
    //11=Logar com o usuario
    //12=Criar sala(item com o leilao)
    //13=Registra log (historico)
    //14=Restaura log (Quandor cair)
    //15=Listar itens ganhadores.
    //16=Registrar Ganhador.
    

    public String getConteudo() {
        return this.conteudo;
    }

    public boolean getResposta() {
        return resposta;
    }

    public int getTipo() {
        return tipo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo=conteudo;
    }

    public void setResposta(boolean resposta) {
        this.resposta=resposta;
    }

    public String getConteudoExtra() {
		return conteudoExtra;
	}

	public void setConteudoExtra(String conteudoExtra) {
		this.conteudoExtra = conteudoExtra;
	}

	public void setTipo(int tipo) {
        this.tipo=tipo;
    }
}