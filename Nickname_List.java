import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Nickname_List implements Serializable {

	private int versao;
    private Map<String, String> nicknames;

    public Nickname_List() {
        this.versao = 0;
        this.nicknames = new HashMap<String, String>();
    }

    public int getVersao() {
        return this.versao;
    }

    public Map<String, String> getNicknames() {
        return nicknames;
    }

    public void setVersao(int versao) {
        this.versao = versao;
    }

    public void setNicknames(Map<String, String> nicknames) {
        this.nicknames = nicknames;
    }
    
}