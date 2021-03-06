package se.esss.litterbox.icebox.icetray.icecube;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import se.esss.litterbox.icebox.exceptions.SignalException;

public class ReadSignal extends Signal {
	
	private final String scanRate;
	private final String recordType = "ai";
	private JsonObject jsonRep;

	public ReadSignal(JsonObject jsonInput) throws SignalException {
		super(jsonInput);

		this.scanRate = jsonInput.getString("scanRate");
		
		buildJsonRep();
	}
	
	public ReadSignal(String nameInput, String scanRateString) throws SignalException {
		super(nameInput);
		this.scanRate = scanRateString;
		
		buildJsonRep();
	}
	
	public ReadSignal(String nameInput) throws SignalException {
		this(nameInput, ".1 second");
	}
	
	public JsonObject getJsonRep() {
		return jsonRep;
	}

	@Override
	public String writeEPICSRecord(String fileName) {
		String outString = "record(" + getRecordType() + ", " + getPvName() + ") {\n";
		outString += "\t" + epicsDBFieldString("DTYP", "stream");
		outString += "\t" + epicsDBFieldString("INP", "@"+fileName+" get_"+getName()+"() $(PORT)");
		outString += "\t" + epicsDBFieldString("SCAN", getScanRate());
		outString += "}\n";
		return outString;
	}

	@Override
	public String writeEPICSProtoFunc(char sig) {
		String outString = "get_" + getName() + " {\n";
		outString += "\tout \"" + sig + "\";\n";
		outString += "\tin \"" + sig + " %f\";\n";
		outString += "\tExtraInput = Ignore;\n";
		outString += "}\n";
		return outString;
	}
	
	@Override
	protected void buildJsonRep() {
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		jsonRep = factory.createObjectBuilder()
			     .add("name", getName())
			     .add("RW", "R")
			     .add("scanRate", getScanRate())
			     .build();
	}
	
	@Override
	public boolean isRead() {
		return true;
	}

	public String getScanRate() {
		return scanRate;
	}

	public String getRecordType() {
		return recordType;
	}

	@Override
	public boolean equals(Object inputObject) {
		if (this==inputObject) return true;
		if (this.getClass() != inputObject.getClass()) return false;
		
		ReadSignal inputSignal = (ReadSignal) inputObject;
		
		return this.scanRate.equals(inputSignal.scanRate)
				&& this.getPvName().equals(inputSignal.getPvName())
				&& this.recordType.equals(inputSignal.recordType)
				&& this.getName().equals(inputSignal.getName());
	}

	@Override
	protected String getPvExt() {
		return ":get";
	}
}
