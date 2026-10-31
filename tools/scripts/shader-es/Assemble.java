import com.oddlabs.tt.render.shader.ShaderDialect;
import com.oddlabs.tt.render.shader.ShaderDialect.Stage;
import java.nio.file.*;
import java.util.*;

/**
 * Assembles the full GLSL source of every shader in the shader package (the sources are built by
 * concatenating text blocks and shared constants), converts each with {@code ShaderDialect.toEs}, and
 * writes both the desktop (-410) and ES (-es) versions so they can be validated with glslangValidator.
 * Run through check-shader-es.sh.
 */
public class Assemble {
    static final Map<String, Map<String, String>> raw = new HashMap<>(); // file -> name -> expr text
    static final Map<String, String> cache = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[0]), out = Path.of(args[1]);
        List<Path> files = new ArrayList<>();
        try (var s = Files.list(dir)) { s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(files::add); }
        for (Path f : files) raw.put(f.getFileName().toString(), parse(Files.readString(f)));
        int total = 0, failed = 0, doubles = 0;
        for (Path f : files) {
            String file = f.getFileName().toString();
            for (String name : List.of("VERTEX_SHADER", "GEOMETRY_SHADER", "FRAGMENT_SHADER")) {
                if (!raw.get(file).containsKey(name)) continue;
                String src = eval(file, name);
                total++;
                Stage st = name.startsWith("FRAG") ? Stage.FRAGMENT : name.startsWith("GEOM") ? Stage.GEOMETRY : Stage.VERTEX;
                String base = file.replace(".java", "") + "." + name;
                String ext = st == Stage.FRAGMENT ? ".frag" : st == Stage.GEOMETRY ? ".geom" : ".vert";
                Files.writeString(out.resolve(base + "-410" + ext), src);
                try {
                    String es = ShaderDialect.toEs(src, st);
                    Files.writeString(out.resolve(base + "-es" + ext), es);
                    String precisionLine = es.lines().skip(1).findFirst().orElse("");
                    System.out.printf("OK   %-44s %s%n", base, precisionLine.startsWith("precision") ? precisionLine.replace("precision highp ", "").replace("; ", ",") : "-");
                } catch (Exception e) { failed++; System.out.printf("FAIL %s: %s%n", base, e.getMessage()); }
            }
        }
        System.out.println("assembled=" + total + " failed=" + failed);
    }

    static Map<String, String> parse(String text) {
        Map<String, String> m = new LinkedHashMap<>();
        // statement ends at the first ';' that is outside a text block; scan manually
        int i = 0;
        var decl = java.util.regex.Pattern.compile("String\\s+([A-Z_0-9]+)\\s*=\\s*");
        var mm = decl.matcher(text);
        while (mm.find(i)) {
            int start = mm.end(), j = start;
            boolean inBlock = false;
            while (j < text.length()) {
                if (text.startsWith("\"\"\"", j)) { inBlock = !inBlock; j += 3; continue; }
                if (!inBlock && text.charAt(j) == ';') break;
                j++;
            }
            m.put(mm.group(1), text.substring(start, j));
            i = j;
        }
        return m;
    }

    static String eval(String file, String name) {
        String key = file + "#" + name;
        if (cache.containsKey(key)) return cache.get(key);
        String expr = raw.get(file).get(name);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c) || c == '+') { i++; continue; }
            if (expr.startsWith("\"\"\"", i)) {
                int end = expr.indexOf("\"\"\"", i + 3);
                String body = expr.substring(i + 3, end);
                body = body.substring(body.indexOf('\n') + 1);
                sb.append(body.stripIndent().translateEscapes());
                i = end + 3;
            } else if (c == '"') {
                int end = i + 1;
                while (expr.charAt(end) != '"') end += expr.charAt(end) == '\\' ? 2 : 1;
                sb.append(expr.substring(i + 1, end).translateEscapes());
                i = end + 1;
            } else {
                int e = i;
                while (e < expr.length() && (Character.isLetterOrDigit(expr.charAt(e)) || expr.charAt(e) == '_')) e++;
                String id = expr.substring(i, e);
                String owner = raw.get(file).containsKey(id) ? file : raw.entrySet().stream().filter(en -> en.getValue().containsKey(id)).map(Map.Entry::getKey).findFirst().orElseThrow(() -> new IllegalStateException("unresolved " + id + " in " + key));
                sb.append(eval(owner, id));
                i = e;
            }
        }
        cache.put(key, sb.toString());
        return sb.toString();
    }
}
