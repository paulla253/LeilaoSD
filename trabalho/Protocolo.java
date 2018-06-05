import java.io.Serializable;


public class Protocolo implements Serializable {

    private boolean resposta;
    private String conteudo;
    private int tipo;
    
    //Tipo == 1 é um lance.

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



}