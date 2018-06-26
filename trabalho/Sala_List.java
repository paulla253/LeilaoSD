import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Sala_List implements Serializable {

    private int versao;
    private Map<Integer, String> salas;
    // <ID sala, Ganhador>

    public Sala_List() {
        this.versao = 0;
        this.salas = new HashMap<Integer, String>();
    }

    public int getVersao() {
        return this.versao;
    }

    public Map<Integer, String> getSalas() {
        return salas;
    }

    public void setVersao(int versao) {
        this.versao = versao;
    }

    public void setSalas(Map<Integer, String> salas) {
        this.salas = salas;
    }

}