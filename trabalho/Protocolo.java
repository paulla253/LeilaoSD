import java.io.Serializable;

public class Protocolo implements Serializable {

    private boolean resposta;
    private String conteudo;
    private String conteudoExtra;
    private float lance;
    private int tipo;
    
    //Tipo:
    //0== Informativo.
    //1== Lance.
    //2== Novo leilao.
    //3== Entrar em um grupo
    //5== Fim de Leilao.
    //6== Participo de algum Leilao?
    
    //Comunicação Controle: 
    //10=Criar novo usuario
    //11=Logar com o usuario
    //12=Criar sala(item com o leilao)
    //13=Registra log (historico)
    //15=Listar itens ganhadores(Historico).
    //16=Registrar Ganhador.
    //17=usuario online.
    //30=pedir historico
    //90=Sincronizar Controle
    

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

	public float getLance() {
		return lance;
	}

	public void setLance(float lance) {
		this.lance = lance;
	}
	
}
