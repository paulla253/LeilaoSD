import java.io.Serializable;
import com.sun.jndi.cosnaming.IiopUrl.Address;


public class Protocolo implements Serializable {

    private boolean resposta;
    private String conteudo;
    private int tipo;
    private org.jgroups.Address Endereco;
    
    //Tipo:
    //1== é um lance.
    //2== Novo leilao.
    //3== Entrar em um grupo

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

    public void setTipo(int tipo) {
        this.tipo=tipo;
    }

	public org.jgroups.Address getEndereco() {
		return Endereco;
	}

	public void setEndereco(org.jgroups.Address address) {
		Endereco = address;
	}
}