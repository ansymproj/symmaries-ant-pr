package se.lnu.prosses.general;

public enum SymmariesHeapDom {
	DEEPALIAS, SHALLOWALIAS, DUMBALIAS, CONNECT;

	public String toString () {
		switch (this) {
		default:
		case DEEPALIAS: return "deepalias";
		case SHALLOWALIAS: return "shallowalias";
		case DUMBALIAS: return "dumbalias";
		case CONNECT: return "connect";
		}
	}

	public static final class UnknownHeapDomError extends Error {
		private UnknownHeapDomError (String s) { super (s); }
	}

	public static SymmariesHeapDom ofString (String s) throws UnknownHeapDomError {
		switch (s) {
		case "deep": case "deepalias": return DEEPALIAS;
		case "shallow": case "shallowalias": return SHALLOWALIAS;
		case "dumb": case "dumbalias": return DUMBALIAS;
		case "connect": return CONNECT;
		default: throw new UnknownHeapDomError ("Unknown Heap Domain: " + s);
		}
	}

	public String getTransFlags () {
		return "heapdom=" + this.toString ();
	}
}
