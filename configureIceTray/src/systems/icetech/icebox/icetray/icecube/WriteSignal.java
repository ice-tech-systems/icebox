package systems.icetech.icebox.icetray.icecube;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

public class WriteSignal extends Signal {

	private final String pvName;
	private final String recordType = "ao";
	private JsonObject jsonRep;

	public WriteSignal(JsonObject jsonInput) {
		super(jsonInput);
		this.pvName = getName() + ":set";
		
		buildJsonRep();
	}
	
	public WriteSignal(String nameInput) {
		super(nameInput);
		this.pvName = getName() + ":set";
		
		buildJsonRep();
	}
	
	public JsonObject getJsonRep() {
		return jsonRep;
	}

	@Override
	public String WriteEPICSRecord(String fileName) {
		String outString = "record(" + getRecordType() + ", " + getPvName() + ") {\n";
		outString += "\t" + epicsDBFieldString("DTYP", "stream");
		outString += "\t" + epicsDBFieldString("OUT", "@"+fileName+" set_"+getName()+"() $(PORT)");
		outString += "}\n";
		return outString;
	}

	@Override
	public String WriteEPICSProtoFunc(char sig) {
		String outString = "set_" + getName() + " {\n";
		outString += "\tout \"" + sig + "%d\\n\";\n";
		outString += "\tExtraInput = Ignore;\n";
		outString += "}\n";
		return outString;
	}
	
	@Override
	protected void buildJsonRep() {
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		jsonRep = factory.createObjectBuilder()
			     .add("name", getName())
			     .add("RW", "W")
			     .build();
	}
	
	@Override
	public boolean isRead() {
		return false;
	}

	public String getPvName() {
		return pvName;
	}

	public String getRecordType() {
		return recordType;
	}

}
