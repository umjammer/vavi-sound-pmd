
package pmd.common;

import java.util.ArrayList;
import java.util.List;


public class Environment {

    private List<String> envs = null;

    public Environment() {
        envs = new ArrayList<>();
    }

    public void AddEnv(String envname) {
        var env = System.getenv(envname);
        if (env != null && !env.isEmpty()) {
            envs.add(String.format("%s=%s", envname, env));
        }
    }

    public String[] GetEnv() {
        return envs.toArray(String[]::new);
    }

    public String[] GetEnvVal(String envname) {
        if (envs == null) return null;

        for (String item : envs) {
            String[] kv = item.split("=");
            if (kv.length != 2) continue;
            if (!kv[0].equalsIgnoreCase(envname)) continue;

            String[] vals = kv[1].split(";");
            return vals;
        }

        return null;
    }
}
