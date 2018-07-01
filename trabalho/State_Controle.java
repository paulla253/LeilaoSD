import java.io.Serializable;
import java.util.Vector;

import org.jgroups.Address;

import tste.ControleSala;

public class State_Controle implements Serializable {

	public Vector<Address> usuariosOnline;
    public Vector<ControleSala> controleSala;
    
    public State_Controle() {
    	this.usuariosOnline = new Vector<Address>();
        this.controleSala = new Vector<ControleSala>();
    }
	
}