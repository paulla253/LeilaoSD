import java.io.Serializable;
import java.util.Vector;

import tste.ControleSala;

public class State_Controle implements Serializable {

	public Vector<String> usuariosOnline;
    public Vector<ControleSala> controleSala;
    
    public State_Controle() {
    	this.usuariosOnline = new Vector<String>();
        this.controleSala = new Vector<ControleSala>();
    }
	
}