package se.esss.litterbox.icebox.icetray;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import se.esss.litterbox.icebox.exceptions.IceCubeException;
import se.esss.litterbox.icebox.exceptions.SignalException;
import se.esss.litterbox.icebox.icetray.icecube.ReadSignal;
import se.esss.litterbox.icebox.icetray.icecube.Signal;
import se.esss.litterbox.icebox.icetray.icecube.WriteSignal;
import se.esss.litterbox.icebox.utilities.InputChecker;

public class IceCube {
	/*
	 * Each physical IceCube consists of an IOC (on a Raspberry Pi)
	 * and a single Arduino.  This Java class is the software 
	 * equivalent of that, and so consists of a name, and a list of 
	 * signals (corresponding to the PV's in the EPICS database).
	 * 
	 * The class is instantiated from a javax.json.JsonObject created
	 * from a JSON file.  This JSON file contains the name of the IceCube
	 * (which will appear as part of the PV name), and the list of signals.
	 * An example of such a file is as follows:
	 * 
	 * {
	 * "name": "RPi1",
	 * "signals": [{
	 *     "name": "photoresistor1",
	 *     "RW": "R",
	 *     "scanRate": "1 second"
	 *     }, {
	 *     "name": "led1",
	 *     "RW": "W"
	 * }]
	 * }
	 * 
	 * From this file, the class creates two extra strings: one 
	 * corresponding to the EPICS DB definition file, and one for
	 * the EPICS protocol file.
	 */
	private final String name;
	private final List<Signal> signals;
	private List<Signal> readSignals = new ArrayList<Signal>();
	private List<Signal> writeSignals = new ArrayList<Signal>(); 
	private final JsonObject jsonRep;
	private final String epicsDBString; // the contents of the EPICS DB defn file
	private final String epicsProtoString; // the contents of the EPICS proto file
	private Integer protoCharCtr = 0; // A ctr to keep track of the no of proto funcs
	private final String dbFileName = "arduino.db";

	public IceCube(JsonObject jsonInput) throws IceCubeException {
		this.jsonRep = jsonInput;
		this.name = jsonInput.getString("name");
		this.signals = new ArrayList<Signal>();
		if (!InputChecker.nameChecker(name)){
			throw new IceCubeException("Illegal IceCube name");
		}

		JsonArray jsonSigs = jsonInput.getJsonArray("signals");
		for (int i=0; i<jsonSigs.size(); i++) {
			JsonObject jsonObj = jsonSigs.getJsonObject(i);
			if (jsonObj.getString("RW").equals("R")) {
				try {
					this.signals.add(new ReadSignal(jsonObj));
					this.readSignals.add(new ReadSignal(jsonObj));
				} catch (SignalException e) {
					throw new IceCubeException(e.getMessage());
				}
			}
			else if (jsonObj.getString("RW").equals("W")) {
				try {
					this.signals.add(new WriteSignal(jsonObj));
					this.writeSignals.add(new WriteSignal(jsonObj));
				} catch (SignalException e) {
					throw new IceCubeException(e.getMessage());
				}
			}
		}
		
		if (!InputChecker.signalListChecker(signals)) {
			throw new IceCubeException("Duplicate signals not allowed in an IceCube");
		}
		
		this.epicsDBString = makeEpicsDBString(dbFileName);
		this.epicsProtoString = makeEpicsProtoString();
	}
	
	public IceCube(String nameInput, List<Signal> signalsInput) throws IceCubeException {
		this.name = nameInput;
		if (!InputChecker.nameChecker(name)){
			throw new IceCubeException("Illegal IceCube name");
		}
		this.signals = new ArrayList<Signal>(signalsInput);
		for (Signal sig : signalsInput) {
			if (sig.isRead()) this.readSignals.add(sig);
			if (sig.isWrite()) this.writeSignals.add(sig);
		}

		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder jBuilder = factory.createObjectBuilder()
				.add("name",  getName());
		JsonArrayBuilder signalArrayBuilder = Json.createArrayBuilder();
		for (int i=0; i<signals.size(); i++) {
			signalArrayBuilder.add(signals.get(i).getJsonRep());
		}
		jBuilder.add("signals", signalArrayBuilder);
		this.jsonRep = jBuilder.build();
		
		if (!InputChecker.signalListChecker(signals)) {
			throw new IceCubeException("Duplicate signals not allowed in an IceCube");
		}
		
		this.epicsDBString = makeEpicsDBString(dbFileName);
		this.epicsProtoString = makeEpicsProtoString();
	}
	
	public String makeEpicsDBString(String fileName) {
		String outString = "";
		Iterator<Signal> iceCubeIterator = this.getSignals().iterator();
		while (iceCubeIterator.hasNext()) {
			outString += iceCubeIterator.next().writeEPICSRecord(fileName);
		}
		return outString;
	}
	
	public String makeEpicsProtoString() {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		alphabet += "abcdefghijklmnopqrstuvwxyz";
		String outString = "Terminator = LF;\n";
		Iterator<Signal> iceCubeIterator = this.getSignals().iterator();
		while (iceCubeIterator.hasNext()) {
			outString += iceCubeIterator.next().writeEPICSProtoFunc(alphabet.charAt(protoCharCtr));
			protoCharCtr++;
		}
		return outString;
	}
	
	public String toString() {
		String outString = "IceCube: " + getName();
		for (int i=0; i<signals.size(); i++) {
			outString += "\n    " + signals.get(i);
		}
		return outString;
	}

	public String getName() {
		return name;
	}

	public List<Signal> getSignals() {
		return signals;
	}

	public String getEpicsDBString() {
		return epicsDBString;
	}

	public String getEpicsProtoString() {
		return epicsProtoString;
	}

	public JsonObject getJsonRep() {
		return jsonRep;
	}

	public List<Signal> getReadSignals() {
		return readSignals;
	}

	public List<Signal> getWriteSignals() {
		return writeSignals;
	}
	
	public int countReadSignals() {
		return getReadSignals().size();
	}
	
	public int countWriteSignals() {
		return getWriteSignals().size();
	}
	
	public int countSignals() {
		return countReadSignals() + countWriteSignals();
	}

	public static void main(String[] args) {
		String filepath = "/Users/stephenmolloy/Code/gitRepos/icebox/configureIceTray/src/systems/icetech/test/jsonTests/";
		try {
			IceCube iceCubeObj = new IceCube(Json.createReader(new FileReader(filepath + "example.json")).readObject());
			System.out.println(iceCubeObj.getEpicsDBString());
			System.out.println(iceCubeObj.getEpicsProtoString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IceCubeException e) {
			e.printStackTrace();
		}
	}

}
