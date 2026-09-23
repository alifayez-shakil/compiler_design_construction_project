package compiler;

import java.util.ArrayList;
import java.util.List;

public class Diagnostic {

    public enum Severity { ERROR, WARNING }

    public static class Entry {
        public final Severity severity;
        public final String phase;   // e.g. "Syntax", "Semantic"
        public final int line;
        public final String message;

        public Entry(Severity severity, String phase, int line, String message) {
            this.severity = severity;
            this.phase = phase;
            this.line = line;
            this.message = message;
        }

        @Override
        public String toString() {
            String label = (severity == Severity.ERROR) ? "Error" : "Warning";
            return phase + " " + label + " (line " + line + "): " + message;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public void error(String phase, int line, String message) {
        report(new Entry(Severity.ERROR, phase, line, message));
    }

    public void warning(String phase, int line, String message) {
        report(new Entry(Severity.WARNING, phase, line, message));
    }

    private void report(Entry entry) {
        entries.add(entry);
        System.out.println(entry);
    }

    public boolean hasErrors() {
        for (Entry e : entries) {
            if (e.severity == Severity.ERROR) return true;
        }
        return false;
    }

    public List<Entry> getEntries() {
        return entries;
    }
}
