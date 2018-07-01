import java.io.Serializable;

public class State implements Serializable {

	public Nickname_List nicknames;
    public Sala_List salas;
    
    public State() {
    	this.nicknames = new Nickname_List();
        this.salas = new Sala_List();
    }
	
}