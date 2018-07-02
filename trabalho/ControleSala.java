import java.io.Serializable;

public class ControleSala implements Serializable{
	
	//não é exatamente ganhador, é os que está ganhando no momento.
    private String usuario;
    private String item;
    private String leiloeiro;
    private String historico="[Historico]";
    
    public String getHistorico() {
		return historico;
	}

	public void setHistorico(String historico) {
		this.historico = historico;
	}

	public ControleSala(String item,String leiloeiro)
    {
        this.leiloeiro = leiloeiro;
        this.item = item;
    }
    
	public String getLeiloeiro() {
		return leiloeiro;
	}

	public void setLeiloeiro(String leiloeiro) {
		this.leiloeiro = leiloeiro;
	}

	public String getUsuario() {
		return usuario;
	}
	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
    
    
}
